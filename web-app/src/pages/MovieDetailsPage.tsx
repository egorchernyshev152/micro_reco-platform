import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CollectionSummary, movieService, Movie, CastMember } from '../api/movieService';
import RecommendationShelf from '../components/RecommendationShelf';
import MovieActions from '../components/MovieActions';
import UserRating from '../components/UserRating';
import MovieReviewsSection from '../components/MovieReviewsSection';
import { useUserStore } from '../context/userStore';
import { useTranslation } from '../i18n/translations';
import './movie-details.css';
import { resolveImageUrl } from '../utils/imageUrl';
import { useNotesStore } from '../context/notesStore';
import { sanitizeSynopsisParagraphs } from '../utils/text';
import { useRatingStore } from '../context/ratingStore';

const MovieDetailsPage = () => {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const userId = useUserStore((state) => state.user?.id);
  const id = Number(movieId);
  const [movie, setMovie] = useState<Movie | null>(null);
  const [similar, setSimilar] = useState<Movie[] | null>(null);
  const [summary, setSummary] = useState<CollectionSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const cachedRating = useRatingStore((state) => state.ratings[id] ?? null);
  const [userRating, setUserRating] = useState<number | null>(typeof cachedRating === 'number' ? cachedRating : null);
  const { t } = useTranslation();
  const noteValue = useNotesStore((state) => (movie ? state.notes[movie.id] ?? '' : ''));
  const setNote = useNotesStore((state) => state.setNote);
  const clearNote = useNotesStore((state) => state.clearNote);
  const [noteDraft, setNoteDraft] = useState('');
  const [castModalOpen, setCastModalOpen] = useState(false);

  useEffect(() => {
    if (cachedRating !== undefined) {
      setUserRating(cachedRating);
    }
  }, [cachedRating]);

  useEffect(() => {
    if (!id) return;
    let active = true;
    setLoading(true);
    (async () => {
      try {
        const [detailsResult, similarResult] = await Promise.allSettled([movieService.details(id), movieService.similar(id, 8)]);
        if (!active) return;
        if (detailsResult.status !== 'fulfilled') {
          throw detailsResult.reason;
        }
        setError(null);
        const details = detailsResult.value;
        setMovie(details);
        if (similarResult.status === 'fulfilled') {
          setSimilar(similarResult.value.items.map((item) => item.movie));
        } else {
          console.warn('Не удалось загрузить похожие фильмы', similarResult.reason);
          setSimilar(null);
        }
        if (userId) {
          try {
            const summaryResponse = await movieService.collectionSummary(userId, [details.id]);
            if (!active) return;
            if (summaryResponse && summaryResponse.length) {
              setSummary(summaryResponse[0]);
            } else {
              setSummary(null);
            }
          } catch (summaryError) {
            console.warn('Не удалось загрузить статус коллекции', summaryError);
            setSummary(null);
          }
        } else {
          setSummary(null);
        }
      } catch (err) {
        if (!active) return;
        setError('Не удалось загрузить карточку фильма');
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    })();
    return () => {
      active = false;
    };
  }, [id, userId]);

  useEffect(() => {
    if (cachedRating === undefined) return;
    setUserRating(cachedRating);
  }, [cachedRating]);

  useEffect(() => {
    setNoteDraft(noteValue ?? '');
  }, [noteValue, movie?.id]);

  const handleActorNavigate = (member: CastMember) => {
    if (!member.name) return;
    navigate(`/actor/${encodeURIComponent(member.name)}`, { state: { actor: member } });
  };

  const handleSaveNote = () => {
    if (!movie) return;
    const trimmed = noteDraft.trim();
    if (!trimmed) {
      clearNote(movie.id);
      setNoteDraft('');
      return;
    }
    setNote(movie.id, trimmed);
  };

  if (!id) return <p>Неверный идентификатор фильма.</p>;
  if (loading && !movie) return <p>Загружаем фильм…</p>;
  if (error) return <p>{error}</p>;
  if (!movie) return <p>Фильм не найден.</p>;

  const posterUrl = resolveImageUrl(movie.posterUrl);
  const synopsisText = movie.synopsis ?? movie.description;
  const sanitizedSynopsis = sanitizeSynopsisParagraphs(synopsisText);
  const synopsisParagraphs = sanitizedSynopsis.length
    ? sanitizedSynopsis
    : synopsisText
    ? [synopsisText.trim()]
    : [];
  const genres: string[] = Array.isArray(movie.genres) ? movie.genres : [];
  const countries: string[] = Array.isArray(movie.countries) ? movie.countries : [];
  const tags: string[] = Array.isArray(movie.tags) ? movie.tags : [];
  const castMembers: CastMember[] = Array.isArray(movie.cast) ? movie.cast : [];
  const catalogRatingValue = typeof movie.averageRating === 'number' ? movie.averageRating : movie.importedRating;

  const interestingFacts = (() => {
    const facts: string[] = [];
    if (movie.releaseDate) {
      facts.push(`${t('movieFactsRelease')}: ${movie.releaseDate}`);
    } else if (movie.releaseYear) {
      facts.push(`${t('movieFactsRelease')}: ${movie.releaseYear}`);
    }
    if (movie.durationMinutes) {
      facts.push(`${t('movieFactsDuration')}: ${movie.durationMinutes} ${t('movieFactsMinutes')}`);
    }
    if (movie.ageRating) {
      facts.push(`${t('movieFactsAge')}: ${movie.ageRating}`);
    }
    if (movie.budget) {
      facts.push(`${t('movieFactsBudget')}: $${movie.budget.toLocaleString('ru-RU')}`);
    }
    if (movie.revenue) {
      facts.push(`${t('movieFactsRevenue')}: $${movie.revenue.toLocaleString('ru-RU')}`);
    }
    if (genres.length) {
      facts.push(`${t('movieFactsGenres')}: ${genres.join(', ')}`);
    }
    if (countries.length) {
      facts.push(`${t('movieFactsCountries')}: ${countries.join(', ')}`);
    }
    if (castMembers.length) {
      const topCast = castMembers
        .slice(0, 3)
        .map((member) => member.name)
        .filter(Boolean);
      if (topCast.length) {
        facts.push(`${t('movieFactsCast')}: ${topCast.join(', ')}`);
      }
    }
    return facts;
  })();

  return (
    <div className="details">
      <div className="details__hero">
        <div className="details__poster">
          {posterUrl ? (
            <img src={posterUrl} alt={movie.title} loading="lazy" referrerPolicy="no-referrer" />
          ) : (
            <div className="movie-card__fallback">🎞</div>
          )}
        </div>
        <div className="details__info">
          <div className="details__titleBlock">
            <h1>{movie.title}</h1>
            <p className="hero__eyebrow">{movie.releaseYear}</p>
          </div>
          <p className="details__meta">
            {[movie.durationMinutes ? `${movie.durationMinutes} мин` : null, genres.join(', '), countries.join(', ')].filter(Boolean).join(' • ')}
          </p>
          {movie.tagline && <p className="details__tagline">“{movie.tagline}”</p>}
          <div className="details__ratingSummary">
            <div className="rating-chip" title={movie.ratingsCount ? `${movie.ratingsCount.toLocaleString('ru-RU')} ${t('detailsRatingsCountLabel')}` : undefined}>
              <span>{t('detailsAudienceRating')}</span>
              <strong>{typeof catalogRatingValue === 'number' ? catalogRatingValue.toFixed(1) : '—'}</strong>
              {movie.ratingsCount ? <small>{`${movie.ratingsCount.toLocaleString('ru-RU')} ${t('detailsRatingsCountLabel')}`}</small> : null}
            </div>
            {userId && (
              <div className="rating-chip rating-chip--personal">
                <span>{t('detailsMyRating')}</span>
                <strong>{typeof userRating === 'number' ? userRating : '—'}</strong>
                {typeof userRating !== 'number' && <small>{t('ratingNotSet')}</small>}
              </div>
            )}
          </div>
          <UserRating movieId={movie.id} variant="compact" className="details__ratingCard" showCaption={false} onRatingChange={setUserRating} />
          <div className="details__actionRow">
            <MovieActions movieId={movie.id} activeTypes={summary?.types as any} />
          </div>
        </div>
      </div>

      {synopsisParagraphs.length > 0 && (
        <div className="details__synopsisPanel">
          <h3 className="details__synopsisTitle">{t('movieSynopsisTitle')}</h3>
          {synopsisParagraphs.map((paragraph, index) => (
            <p key={`synopsis-${index}`} className="details__synopsis">
              {paragraph}
            </p>
          ))}
        </div>
      )}

      {(interestingFacts.length > 0 || tags.length > 0) && (
        <section className="details__factsSection">
          {interestingFacts.length > 0 && (
            <div className="details__factsPanel">
              <h3>{t('movieFactsTitle')}</h3>
              <ul>
                {interestingFacts.map((fact, index) => (
                  <li key={`fact-${index}`}>{fact}</li>
                ))}
              </ul>
            </div>
          )}
          {tags.length > 0 && (
            <div className="details__tagsPanel">
              <h3>{t('movieTagsTitle')}</h3>
              <div className="details__tags">
                {tags.map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
            </div>
          )}
        </section>
      )}

      <section className="details__cast">
        <h3>{t('detailsActors')}</h3>
        <div className="cast-list">
          {castMembers.slice(0, 6).map((member) => {
            const profile = resolveImageUrl(member.profileUrl, 'w185');
            return (
              <button key={`${member.tmdbId}-${member.name}`} type="button" className="cast-card" onClick={() => handleActorNavigate(member)}>
                {profile ? (
                  <img src={profile} alt={member.name} loading="lazy" referrerPolicy="no-referrer" />
                ) : (
                  <div className="cast-card__placeholder">👤</div>
                )}
                <div>
                  <strong>{member.name}</strong>
                  <span>{member.character}</span>
                </div>
              </button>
            );
          })}
          {castMembers.length > 6 && (
            <button type="button" className="cast-card cast-card--toggle" onClick={() => setCastModalOpen(true)}>
              {`Показать ещё ${Math.max(castMembers.length - 6, 0)}`}
            </button>
          )}
        </div>
      </section>

      <section className="details-panel movie-notes">
        <div className="movie-notes__header">
          <h3>{t('movieNotesTitle')}</h3>
          {noteValue && <span>{t('movieNoteBadge')}</span>}
        </div>
        <textarea
          value={noteDraft}
          placeholder={t('movieNotesPlaceholder')}
          onChange={(e) => setNoteDraft(e.target.value)}
        />
        <div className="movie-notes__actions">
          <button type="button" onClick={handleSaveNote}>
            {t('movieNotesSave')}
          </button>
          {noteValue && (
            <button
              type="button"
              className="movie-notes__clear"
              onClick={() => {
                if (!movie) return;
                clearNote(movie.id);
                setNoteDraft('');
              }}
            >
              {t('movieNotesClear')}
            </button>
          )}
        </div>
      </section>

      <MovieReviewsSection movieId={movie.id} userId={userId} />

      {similar && <RecommendationShelf title="Похожие фильмы" movies={similar} />}

      {castModalOpen && (
        <div className="cast-modal" role="dialog" aria-modal="true">
          <div className="cast-modal__content">
            <div className="cast-modal__header">
              <h3>{t('detailsActors')}</h3>
              <button type="button" onClick={() => setCastModalOpen(false)}>
                ✕
              </button>
            </div>
            <div className="cast-modal__grid">
              {castMembers.map((member) => {
                const profile = resolveImageUrl(member.profileUrl, 'w185');
                return (
                  <button key={`${member.tmdbId}-${member.name}-modal`} type="button" className="cast-card" onClick={() => handleActorNavigate(member)}>
                    {profile ? (
                      <img src={profile} alt={member.name} loading="lazy" referrerPolicy="no-referrer" />
                    ) : (
                      <div className="cast-card__placeholder">👤</div>
                    )}
                    <div>
                      <strong>{member.name}</strong>
                      <span>{member.character}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MovieDetailsPage;
