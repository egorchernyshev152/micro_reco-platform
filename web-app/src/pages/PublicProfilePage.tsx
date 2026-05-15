import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { movieService, Movie, UserProfile, FollowStatus } from '../api/movieService';
import MovieCard from '../components/MovieCard';
import RecommendationShelf from '../components/RecommendationShelf';
import './profile.css';
import { formatFollowStats, formatProfileStats, useTranslation } from '../i18n/translations';
import { useUserStore } from '../context/userStore';
import { notifyError, notifySuccess } from '../context/notificationStore';
import { complaintService } from '../api/complaintService';

const PublicProfilePage = () => {
  const { userId } = useParams();
  const numericId = Number(userId);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [favorites, setFavorites] = useState<Movie[]>([]);
  const [watchlist, setWatchlist] = useState<Movie[]>([]);
  const [watched, setWatched] = useState<Movie[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [followStatus, setFollowStatus] = useState<FollowStatus | null>(null);
  const [followBusy, setFollowBusy] = useState(false);
  const [complaintOpen, setComplaintOpen] = useState(false);
  const [complaintCategory, setComplaintCategory] = useState<'spam' | 'abuse' | 'suspicious'>('spam');
  const [complaintDescription, setComplaintDescription] = useState('');
  const [complaintSubmitting, setComplaintSubmitting] = useState(false);
  const navigate = useNavigate();
  const { t, language } = useTranslation();
  const viewerId = useUserStore((state) => state.user?.id);

  useEffect(() => {
    if (!numericId) {
      setError(t('publicProfileMissing'));
      setLoading(false);
      return;
    }
    let active = true;
    setLoading(true);
    setError(null);
    movieService
      .getPublicProfile(numericId)
      .then((info) => {
        if (!active) return;
        setProfile(info);
        return Promise.all([
          movieService.getPublicCollection(numericId, 'FAVORITE'),
          movieService.getPublicCollection(numericId, 'WATCHLIST'),
          movieService.getPublicCollection(numericId, 'WATCHED')
        ]).then(([fav, watch, seen]) => {
          if (!active) return;
          setFavorites(fav);
          setWatchlist(watch);
          setWatched(seen);
        })
        .then(() => {
          if (!active || !viewerId || !numericId || viewerId === numericId) {
            setFollowStatus(null);
            return;
          }
          movieService
            .followStatus(numericId)
            .then((status) => active && setFollowStatus(status))
            .catch(() => active && setFollowStatus(null));
        });
      })
      .catch((err) => {
        if (!active) return;
        const message = err?.response?.data?.message ?? t('publicProfilePrivateDescription');
        setError(message);
        setProfile(null);
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [numericId, t]);

  const stats = useMemo(
    () => ({
      favorites: profile?.favoritesCount ?? favorites.length,
      watchlist: profile?.watchlistCount ?? watchlist.length,
      watched: profile?.watchedCount ?? watched.length
    }),
    [profile, favorites.length, watchlist.length, watched.length]
  );

  const complaintOptions = useMemo(
    () => [
      { value: 'spam', label: t('complaintCategorySpam') },
      { value: 'abuse', label: t('complaintCategoryAbuse') },
      { value: 'suspicious', label: t('complaintCategorySuspicious') }
    ],
    [t]
  );

  if (loading) {
    return (
      <div className="profile-page">
        <div className="profile-header">
          <p>{t('publicProfileLoading')}</p>
        </div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="profile-page">
        <div className="profile-header">
          <div>
            <h1>{t('publicProfilePrivateTitle')}</h1>
            <p className="profile-subtitle">{error ?? t('publicProfilePrivateDescription')}</p>
          </div>
          <button className="profile-settingsBtn" type="button" onClick={() => navigate(-1)}>
            {t('publicProfileBack')}
          </button>
        </div>
      </div>
    );
  }

  const followCounts = formatFollowStats(
    language,
    profile.followersCount ?? followStatus?.followersCount ?? 0,
    profile.followingCount ?? followStatus?.followingCount ?? 0
  );

  const canFollow = !!viewerId && profile.id !== viewerId;
  const canReport = canFollow;

  const handleFollowToggle = () => {
    if (!canFollow) return;
    setFollowBusy(true);
    const action = followStatus?.following ? movieService.unfollowUser(profile.id) : movieService.followUser(profile.id);
    action
      .then((status) => {
        setFollowStatus(status);
        setProfile((prev) =>
          prev
            ? {
                ...prev,
                followersCount: status.followersCount,
                followingCount: status.followingCount
              }
            : prev
        );
        notifySuccess(status.following ? t('followActionAdded') : t('followActionRemoved'));
      })
      .catch(() => notifyError(t('profileFollowError')))
      .finally(() => setFollowBusy(false));
  };

  const MIN_COMPLAINT_LENGTH = 10;
  const trimmedDescription = complaintDescription.trim();
  const complaintTooShort = trimmedDescription.length > 0 && trimmedDescription.length < MIN_COMPLAINT_LENGTH;

  const handleComplaintSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!profile || !canReport || complaintSubmitting) {
      return;
    }
    if (trimmedDescription.length < MIN_COMPLAINT_LENGTH) {
      notifyError(t('complaintTooShort'));
      return;
    }
    const categoryOption = complaintOptions.find((option) => option.value === complaintCategory);
    const categoryLabel = categoryOption?.label ?? complaintCategory;
    const categoryCode = categoryOption?.value ?? complaintCategory;
    setComplaintSubmitting(true);
    try {
      await complaintService.submit({
        targetUserId: profile.id,
        category: `${categoryLabel} (${categoryCode.toUpperCase()})`,
        description: trimmedDescription
      });
      notifySuccess(t('complaintSubmitted'));
      setComplaintDescription('');
      setComplaintCategory('spam');
      setComplaintOpen(false);
    } catch (error) {
      notifyError(t('complaintError'));
    } finally {
      setComplaintSubmitting(false);
    }
  };

  return (
    <div className="profile-page">
      <header className="profile-header">
        <div className="profile-avatar">{profile.name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</div>
        <div className="profile-header__info">
          <p className="eyebrow">{t('publicProfileTitle')}</p>
          <h1>{profile.name}</h1>
          <p className="profile-subtitle">
            {formatProfileStats(language, stats.watched, stats.favorites, stats.watchlist, 0)} · {followCounts}
          </p>
        </div>
        <div className="profile-header__actions">
          {canFollow && (
            <button
              className={`profile-settingsBtn ${followStatus?.following ? 'is-active' : ''}`}
              type="button"
              onClick={handleFollowToggle}
              disabled={followBusy}
            >
              {followStatus?.following ? t('followActionFollowing') : t('followActionFollow')}
            </button>
          )}
          {canReport && (
            <button className="profile-settingsBtn" type="button" onClick={() => setComplaintOpen(true)}>
              {t('complaintReportAction')}
            </button>
          )}
          <button className="profile-settingsBtn" type="button" onClick={() => navigate(-1)}>
            {t('publicProfileBack')}
          </button>
        </div>
      </header>

      <div className="profile-body">
        <div className="profile-body__main">
          <section className="profile-section">
            <div className="profile-section__header">
              <h2>{t('profileFavorites')}</h2>
            </div>
            <div className="profile-grid">
              {favorites.slice(0, 6).map((movie) => (
                <MovieCard key={movie.id} movie={movie} showMeta={false} />
              ))}
              {!favorites.length && <p>{t('collectionEmpty')}</p>}
            </div>
          </section>

          <section className="profile-section">
            <div className="profile-section__header">
              <h2>{t('profileWatchlist')}</h2>
            </div>
            <div className="profile-grid">
              {watchlist.slice(0, 6).map((movie) => (
                <MovieCard key={movie.id} movie={movie} showMeta={false} />
              ))}
              {!watchlist.length && <p>{t('collectionEmpty')}</p>}
            </div>
          </section>

          <RecommendationShelf title={t('profileWatched')} movies={watched.slice(0, 12)} />
        </div>
      </div>
      {complaintOpen && canReport && (
        <div className="profile-complaintModal" role="dialog" aria-modal="true">
          <form className="profile-complaintModal__content" onSubmit={handleComplaintSubmit}>
            <div className="profile-complaintModal__header">
              <h3>{t('complaintDialogTitle')}</h3>
              <button type="button" onClick={() => setComplaintOpen(false)}>
                ×
              </button>
            </div>
            <div className="profile-complaintModal__field">
              <span>{t('complaintCategoryLabel')}</span>
              <div className="complaint-categoryOptions" role="radiogroup" aria-label={t('complaintCategoryLabel')}>
                {complaintOptions.map((option) => {
                  const isActive = complaintCategory === option.value;
                  return (
                    <button
                      key={option.value}
                      type="button"
                      className={`complaint-categoryOption ${isActive ? 'is-active' : ''}`}
                      aria-pressed={isActive}
                      onClick={() => setComplaintCategory(option.value as 'spam' | 'abuse' | 'suspicious')}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>
            <label>
              {t('complaintDescriptionLabel')}
              <textarea
                value={complaintDescription}
                onChange={(event) => setComplaintDescription(event.target.value)}
                rows={4}
                minLength={MIN_COMPLAINT_LENGTH}
                maxLength={2048}
                required
                aria-invalid={complaintTooShort}
              />
              {complaintTooShort && (
                <div className="profile-formHint is-error">
                  <span className="profile-formHint__icon" aria-hidden="true">!</span>
                  <span>{t('complaintTooShort')}</span>
                </div>
              )}
            </label>
            <div className="profile-complaintModal__actions">
              <button type="button" className="complaint-cancelBtn" onClick={() => setComplaintOpen(false)}>
                {t('complaintCancel')}
              </button>
              <button type="submit" className="complaint-submitBtn" disabled={complaintSubmitting}>
                {complaintSubmitting ? t('complaintSubmitting') : t('complaintSubmit')}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

export default PublicProfilePage;
