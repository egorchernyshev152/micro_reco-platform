import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { movieService, MovieReview } from '../api/movieService';
import { useTranslation } from '../i18n/translations';
import { notifyError, notifySuccess } from '../context/notificationStore';
import './movie-reviews.css';

interface MovieReviewsSectionProps {
  movieId: number;
  userId?: number | null;
}

const MIN_LENGTH = 30;

const resolveScoreTone = (score: number) => {
  if (score >= 8) return 'positive';
  if (score >= 5) return 'neutral';
  return 'negative';
};

const MovieReviewsSection = ({ movieId, userId }: MovieReviewsSectionProps) => {
  const { t, language } = useTranslation();
  const [reviews, setReviews] = useState<MovieReview[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(false);
  const [myReview, setMyReview] = useState<MovieReview | null>(null);
  const [formContent, setFormContent] = useState('');
  const [formScore, setFormScore] = useState(8);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [scorePickerOpen, setScorePickerOpen] = useState(false);
  const scorePickerRef = useRef<HTMLDivElement | null>(null);
  const scoreTone = useMemo(() => resolveScoreTone(formScore), [formScore]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    movieService
      .getReviews(movieId, 0, 6)
      .then((data) => {
        if (!active) return;
        setReviews(data.items);
        setPage(data.page);
        setHasNext(data.hasNext);
      })
      .catch(() => {
        if (!active) return;
        setError(t('reviewsLoadError'));
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [movieId, t]);

  useEffect(() => {
    if (!userId) {
      setMyReview(null);
      return;
    }
    let active = true;
    movieService
      .getMyReview(movieId)
      .then((review) => {
        if (!active) return;
        setMyReview(review);
      })
      .catch(() => {
        if (!active) return;
        setMyReview(null);
      });
    return () => {
      active = false;
    };
  }, [movieId, userId]);

  const submitDisabled = formContent.trim().length < MIN_LENGTH || submitting;

  const pendingNotice = useMemo(() => {
    if (!myReview) return null;
    if (myReview.status === 'PUBLISHED') {
      return t('reviewStatusPublished');
    }
    return t('reviewStatusPending');
  }, [myReview, t]);

  const loadMore = () => {
    if (!hasNext) return;
    const nextPage = page + 1;
    setLoading(true);
    movieService
      .getReviews(movieId, nextPage, 6)
      .then((data) => {
        setReviews((prev) => [...prev, ...data.items]);
        setPage(data.page);
        setHasNext(data.hasNext);
      })
      .catch(() => notifyError(t('reviewsLoadError')))
      .finally(() => setLoading(false));
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!userId) {
      notifyError(t('reviewsLoginRequired'));
      return;
    }
    const payload = {
      score: formScore,
      content: formContent.trim()
    };
    setSubmitting(true);
    movieService
      .submitReview(movieId, payload)
      .then((review) => {
        setMyReview(review);
        setFormContent('');
        notifySuccess(t('reviewSubmitted'));
        if (review.status === 'PUBLISHED') {
          movieService.getReviews(movieId, 0, 6).then((data) => {
            setReviews(data.items);
            setPage(data.page);
            setHasNext(data.hasNext);
          });
        }
      })
      .catch(() => notifyError(t('reviewSubmitError')))
      .finally(() => setSubmitting(false));
  };

  useEffect(() => {
    if (!scorePickerOpen) return;
    const handleClickOutside = (event: MouseEvent) => {
      if (!scorePickerRef.current) return;
      if (!scorePickerRef.current.contains(event.target as Node)) {
        setScorePickerOpen(false);
      }
    };
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setScorePickerOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [scorePickerOpen]);

  const handleScoreSelect = (value: number) => {
    setFormScore(value);
    setScorePickerOpen(false);
  };

  const formatDate = (value: string) => {
    return new Date(value).toLocaleDateString(language === 'ru' ? 'ru-RU' : 'en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  return (
    <section className="details-panel reviews-panel">
      <div className="reviews-panel__header">
        <div>
          <h3>{t('reviewsSectionTitle')}</h3>
          <p>{t('reviewsSectionSubtitle')}</p>
        </div>
        {pendingNotice && <span className="reviews-panel__badge">{pendingNotice}</span>}
      </div>

      {userId ? (
        <form className="movie-notes review-form" onSubmit={handleSubmit}>
          <div className="review-form__row">
            <div className="review-form__scorePanel">
              <span className="review-form__label">{t('reviewFormScore')}</span>
              <div className={`score-picker ${scorePickerOpen ? 'is-open' : ''} score-tone--${scoreTone}`} ref={scorePickerRef}>
                <button
                  type="button"
                  className="score-picker__toggle"
                  onClick={() => setScorePickerOpen((prev) => !prev)}
                  aria-label={`${t('reviewFormScore')}: ${formScore}`}
                >
                  <strong>{formScore}</strong>
                  <span aria-hidden className="score-picker__chevron" />
                </button>
                {scorePickerOpen && (
                  <div className="score-picker__dropdown">
                    {Array.from({ length: 10 }, (_, index) => index + 1).map((score) => (
                      <button
                        type="button"
                        key={`score-${score}`}
                        className={`${score === formScore ? 'is-active' : ''} score-tone--${resolveScoreTone(score)}`}
                        onClick={() => handleScoreSelect(score)}
                      >
                        {score}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <p className="review-form__hint">{t('reviewFormHint')}</p>
            </div>
            <div className="review-form__editor">
              <textarea
                value={formContent}
                placeholder={t('reviewFormPlaceholder')}
                onChange={(event) => setFormContent(event.target.value)}
              />
            </div>
          </div>
          <div className="review-form__actions">
            <button type="submit" disabled={submitDisabled}>
              {submitting ? t('reviewSubmitting') : t('reviewFormSubmit')}
            </button>
            <small>
              {formContent.trim().length}/{MIN_LENGTH}
            </small>
          </div>
        </form>
      ) : (
        <p className="reviews-panel__muted">{t('reviewsLoginPrompt')}</p>
      )}

      <div className="reviews-list">
        {error && <p className="reviews-panel__error">{error}</p>}
        {!error && !reviews.length && !loading && <p className="reviews-panel__muted">{t('reviewsEmpty')}</p>}
        {reviews.map((review) => (
          <article key={review.id} className="review-card">
            <header>
              <div>
                <strong>{review.authorName}</strong>
                <span>{formatDate(review.createdAt)}</span>
              </div>
              <span className={`review-card__score score-tone--${resolveScoreTone(review.score)}`}>{review.score}</span>
            </header>
            <p>{review.content}</p>
          </article>
        ))}
      </div>

      {hasNext && (
        <button type="button" className="reviews-panel__more" onClick={loadMore} disabled={loading}>
          {loading ? t('reviewsLoading') : t('reviewsLoadMore')}
        </button>
      )}
    </section>
  );
};

export default MovieReviewsSection;
