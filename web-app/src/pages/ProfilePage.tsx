import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { movieService, Movie, FollowUser } from '../api/movieService';
import { userSimilarityService, SimilarUser } from '../api/userSimilarityService';
import { useUserStore } from '../context/userStore';
import RecommendationShelf from '../components/RecommendationShelf';
import MovieCard from '../components/MovieCard';
import MovieActions from '../components/MovieActions';
import './profile.css';
import { usePreferencesStore } from '../context/preferencesStore';
import { formatProfileStats, useTranslation } from '../i18n/translations';
import { useCollectionStore } from '../context/collectionStore';
import { toSummaryMap } from '../utils/collectionSummary';
import UserRating from '../components/UserRating';
import { notifyError, notifySuccess } from '../context/notificationStore';
import { useCacheStore } from '../context/cacheStore';
import { resolveImageUrl } from '../utils/imageUrl';
import { useActorFavoriteStore } from '../context/actorFavoriteStore';

const ProfilePage = () => {
  const user = useUserStore((state) => state.user);
  const userId = user?.id;
  const avatar = usePreferencesStore((state) => state.avatar);
  const language = usePreferencesStore((state) => state.language);
  const { t } = useTranslation();
  const profileCache = useCacheStore((state) => state.profile);
  const setProfileCache = useCacheStore((state) => state.setProfile);
  const [watched, setWatched] = useState<Movie[]>(profileCache?.watched ?? []);
  const [watchlist, setWatchlist] = useState<Movie[]>(profileCache?.watchlist ?? []);
  const [favorites, setFavorites] = useState<Movie[]>(profileCache?.favorites ?? []);
  const [personal, setPersonal] = useState<Movie[]>(profileCache?.personal ?? []);
  const [loading, setLoading] = useState(!profileCache);
  const [similarUsers, setSimilarUsers] = useState<SimilarUser[]>([]);
  const [similarLoading, setSimilarLoading] = useState(false);
  const [followingUsers, setFollowingUsers] = useState<FollowUser[]>([]);
  const [followersUsers, setFollowersUsers] = useState<FollowUser[]>([]);
  const [followLoading, setFollowLoading] = useState(false);
  const [followActionUser, setFollowActionUser] = useState<number | null>(null);
  const navigate = useNavigate();
  const mergeCollectionSummary = useCollectionStore((state) => state.mergeSummary);
  const actorFavorites = useActorFavoriteStore((state) => state.ordered);
  const actorFavoritesLoadedFor = useActorFavoriteStore((state) => state.loadedUserId);
  const setActorFavoritesStore = useActorFavoriteStore((state) => state.setFavorites);
  const resetActorFavorites = useActorFavoriteStore((state) => state.reset);

  useEffect(() => {
    if (!userId) return;
    let active = true;
    setLoading(true);
    Promise.all([
      movieService.getCollection(userId, 'WATCHED'),
      movieService.getCollection(userId, 'WATCHLIST'),
      movieService.getCollection(userId, 'FAVORITE'),
      movieService.forUser(userId, { limit: 8, algo: 'HYBRID', period: 'MONTH' })
    ])
      .then(([watchedRes, watchlistRes, favoriteRes, personalRes]) => {
        if (!active) return;
        setWatched(watchedRes);
        setWatchlist(watchlistRes);
        setFavorites(favoriteRes);
        setPersonal(personalRes.items.map((item) => item.movie));
        setProfileCache({
          watched: watchedRes,
          watchlist: watchlistRes,
          favorites: favoriteRes,
          personal: personalRes.items.map((item) => item.movie)
        });
        const summaryIds = new Set<number>();
        [...watchedRes, ...watchlistRes, ...favoriteRes].forEach((movie) => summaryIds.add(movie.id));
        if (userId && summaryIds.size) {
          movieService
            .collectionSummary(userId, Array.from(summaryIds))
            .then((summary) => {
              if (!summary) return;
              mergeCollectionSummary(toSummaryMap(summary));
            })
            .catch(() => {});
        }
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [userId, mergeCollectionSummary, setProfileCache]);

  useEffect(() => {
    if (!userId) {
      setSimilarUsers([]);
      setFollowingUsers([]);
      setFollowersUsers([]);
      return;
    }
    let active = true;
    setSimilarLoading(true);
    userSimilarityService
      .getSimilar(userId, { limit: 4, minOverlap: 2 })
      .then((response) => {
        if (!active) return;
        setSimilarUsers(response.items ?? []);
      })
      .catch(() => {
        if (!active) return;
        setSimilarUsers([]);
      })
      .finally(() => active && setSimilarLoading(false));
    return () => {
      active = false;
    };
  }, [userId]);

  useEffect(() => {
    if (!userId) {
      setFollowingUsers([]);
      setFollowersUsers([]);
      return;
    }
    let active = true;
    setFollowLoading(true);
    Promise.all([movieService.getFollowingUsers(userId, 12), movieService.getFollowerUsers(userId, 12)])
      .then(([followingRes, followersRes]) => {
        if (!active) return;
        setFollowingUsers(followingRes);
        setFollowersUsers(followersRes);
      })
      .catch(() => {
        if (!active) return;
        setFollowingUsers([]);
        setFollowersUsers([]);
      })
      .finally(() => active && setFollowLoading(false));
    return () => {
      active = false;
    };
  }, [userId]);

  useEffect(() => {
    if (!userId) {
      resetActorFavorites();
      return;
    }
    if (actorFavoritesLoadedFor === userId) return;
    movieService
      .getFavoriteActors(userId)
      .then((data) => setActorFavoritesStore(userId, data))
      .catch(() => setActorFavoritesStore(userId, []));
  }, [userId, actorFavoritesLoadedFor, setActorFavoritesStore, resetActorFavorites]);

  const stats = useMemo(
    () => ({
      watched: watched.length,
      favorites: favorites.length,
      watchlist: watchlist.length,
      favoriteActors: actorFavorites.length
    }),
    [watched.length, favorites.length, watchlist.length, actorFavorites.length]
  );

  const [activeTab, setActiveTab] = useState<'overview' | 'ratings' | 'similar'>('overview');
  const ratingList = useMemo(() => watched.slice(0, 8), [watched]);
  const hasRatings = watched.length > 0;
  const followingIds = useMemo(() => new Set(followingUsers.map((item) => item.id)), [followingUsers]);

  const refreshFollowing = () => {
    if (!userId) return Promise.resolve();
    return movieService
      .getFollowingUsers(userId, 12)
      .then((data) => setFollowingUsers(data))
      .catch(() => {});
  };

  const toggleFollow = (targetId: number, follow: boolean, successMessage: string) => {
    if (!userId) {
      notifyError(t('reviewsLoginRequired'));
      return;
    }
    setFollowActionUser(targetId);
    const request = follow ? movieService.followUser(targetId) : movieService.unfollowUser(targetId);
    request
      .then(() => {
        notifySuccess(successMessage);
        return refreshFollowing();
      })
      .catch(() => notifyError(t('profileFollowError')))
      .finally(() => setFollowActionUser(null));
  };

  return (
    <div className="profile-page">
      <header className="profile-header">
        <div className="profile-avatar">{avatar}</div>
        <div className="profile-header__info">
          <p className="eyebrow">{t('profileHeader')}</p>
          <h1>{user?.name ?? 'Киноман'}</h1>
          <p className="profile-subtitle">{formatProfileStats(language, stats.watched, stats.favorites, stats.watchlist, stats.favoriteActors)}</p>
        </div>
        <button className="profile-settingsBtn" type="button" onClick={() => navigate('/settings')}>
          <span className="profile-settingsBtn__icon">⚙</span>
          <span>{t('settingsTitle')}</span>
        </button>
      </header>

  <div className="profile-tabs">
    <button className={activeTab === 'overview' ? 'is-active' : ''} onClick={() => setActiveTab('overview')}>
      {t('profileTabOverview')}
    </button>
    <button className={activeTab === 'ratings' ? 'is-active' : ''} onClick={() => setActiveTab('ratings')}>
      {t('profileTabRatings')}
    </button>
    <button className={activeTab === 'similar' ? 'is-active' : ''} onClick={() => setActiveTab('similar')}>
      {t('profileTabSimilar')}
    </button>
  </div>

      <div className="profile-body">
        {activeTab === 'overview' && (
          <>
            <section className="profile-stats">
              <button className="stat-card" onClick={() => navigate('/watched')}>
                <p>{t('profileWatched')}</p>
                <strong>{stats.watched}</strong>
                <span>→</span>
              </button>
              <button className="stat-card" onClick={() => navigate('/favorites')}>
                <p>{t('profileFavorites')}</p>
                <strong>{stats.favorites}</strong>
                <span>→</span>
              </button>
              <button className="stat-card" onClick={() => navigate('/favorites#actors')}>
                <p>{t('profileFavoriteActors')}</p>
                <strong>{stats.favoriteActors}</strong>
                <span>→</span>
              </button>
              <button className="stat-card" onClick={() => navigate('/bookmarks')}>
                <p>{t('profileWatchlist')}</p>
                <strong>{stats.watchlist}</strong>
                <span>→</span>
              </button>
            </section>

            <section className="profile-section profile-section--tight">
              <div className="profile-section__header">
                <h2>{t('profileWatched')}</h2>
                <button onClick={() => navigate('/watched')}>{t('profileViewAll')}</button>
              </div>
              {loading && !watched.length ? (
                <div className="profile-grid profile-grid--four profile-grid--loading">
                  {Array.from({ length: 4 }).map((_, idx) => (
                    <div key={`watched-skeleton-${idx}`} className="profile-skeletonCard" />
                  ))}
                </div>
              ) : (
                <div className="profile-grid profile-grid--four">
                  {watched.slice(0, 4).map((movie) => (
                    <MovieCard key={movie.id} movie={movie} showMeta={false} actions={<MovieActions movieId={movie.id} />} />
                  ))}
                </div>
              )}
            </section>

            <section className="profile-section profile-section--tight">
              <div className="profile-section__header">
                <h2>{t('profileFavorites')}</h2>
                <button onClick={() => navigate('/favorites')}>{t('profileOpenList')}</button>
              </div>
              {loading && !favorites.length ? (
                <div className="profile-grid profile-grid--four profile-grid--loading">
                  {Array.from({ length: 4 }).map((_, idx) => (
                    <div key={`fav-skeleton-${idx}`} className="profile-skeletonCard" />
                  ))}
                </div>
              ) : (
                <div className="profile-grid profile-grid--four">
                  {favorites.slice(0, 4).map((movie) => (
                    <MovieCard key={movie.id} movie={movie} showMeta={false} actions={<MovieActions movieId={movie.id} />} />
                  ))}
                </div>
              )}
            </section>

            <section className="profile-section profile-section--tight">
              <div className="profile-section__header">
                <h2>{t('profileFavoriteActors')}</h2>
              </div>
              {!actorFavorites.length && <p>{t('profileFavoriteActorsEmpty')}</p>}
              {actorFavorites.length > 0 && (
                <div className="profile-actor-grid">
                  {actorFavorites.slice(0, 6).map((actor) => (
                    <button
                      key={actor.actorTmdbId}
                      type="button"
                      className="profile-actorCard"
                      onClick={() =>
                        navigate(`/actor/${encodeURIComponent(actor.actorName)}`, {
                          state: { actor: { tmdbId: actor.actorTmdbId, name: actor.actorName, profileUrl: actor.profileUrl } }
                        })
                      }
                    >
                      {actor.profileUrl ? (
                        <img src={actor.profileUrl} alt={actor.actorName} loading="lazy" referrerPolicy="no-referrer" />
                      ) : (
                        <span>🎭</span>
                      )}
                      <strong>{actor.actorName}</strong>
                    </button>
                  ))}
                </div>
              )}
            </section>

            <section className="profile-section profile-section--tight">
              <div className="profile-section__header">
                <h2>{t('profileWatchlist')}</h2>
                <button onClick={() => navigate('/bookmarks')}>{t('profileOpenList')}</button>
              </div>
              {loading && !watchlist.length ? (
                <div className="profile-grid profile-grid--four profile-grid--loading">
                  {Array.from({ length: 4 }).map((_, idx) => (
                    <div key={`watchlist-skeleton-${idx}`} className="profile-skeletonCard" />
                  ))}
                </div>
              ) : (
                <div className="profile-grid profile-grid--four">
                  {watchlist.slice(0, 4).map((movie) => (
                    <MovieCard key={movie.id} movie={movie} showMeta={false} actions={<MovieActions movieId={movie.id} />} />
                  ))}
                </div>
              )}
            </section>

            <RecommendationShelf title="Новые рекомендации для вас" movies={personal} loading={loading && !personal.length} />
          </>
        )}

        {activeTab === 'ratings' && (
          <section className="profile-ratings">
            <h2>{t('profileRatingsTitle')}</h2>
            {!hasRatings && <p>{t('profileRatingsEmpty')}</p>}
            <div className="profile-ratings__list">
              {ratingList.map((movie) => {
                const posterUrl = resolveImageUrl(movie.posterUrl, 'w185');
                return (
                  <div key={movie.id} className="profile-ratingCard">
                    <button type="button" className="profile-ratingCard__poster" onClick={() => navigate(`/movie/${movie.id}`)} aria-label={movie.title}>
                      {posterUrl ? (
                        <img src={posterUrl} alt={movie.title} loading="lazy" referrerPolicy="no-referrer" />
                      ) : (
                        <div className="profile-ratingCard__posterFallback">🎞</div>
                      )}
                    </button>
                    <div className="profile-ratingCard__body">
                      <div className="profile-ratingCard__info" onClick={() => navigate(`/movie/${movie.id}`)}>
                        <strong>{movie.title}</strong>
                        <span>{movie.releaseYear ?? '—'}</span>
                      </div>
                      <UserRating movieId={movie.id} variant="compact" showCaption={false} />
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {activeTab === 'similar' && (
          <section className="profile-section">
            <div className="profile-similarBlocks">
              <section className="profile-similarBlock">
                <div className="profile-similarBlock__header">
                  <div>
                    <h3>{t('profileFollowingTitle')}</h3>
                    <p>{t('profileFollowingSubtitle')}</p>
                  </div>
                  <span className="profile-similarBlock__count">{followingUsers.length}</span>
                </div>
                {followLoading && <p className="profile-similar__hint">{t('profileSimilarLoading')}</p>}
                {!followLoading && !followingUsers.length && <p className="profile-similar__hint">{t('profileFollowingEmpty')}</p>}
                <div className="profile-followGrid">
                  {followingUsers.map((user) => {
                    const isActive = followActionUser === user.id;
                    return (
                      <div key={user.id} className="follow-card">
                        <div>
                          <strong>{user.name}</strong>
                          <span>{user.mutual ? t('profileFollowMutual') : t('profileFollowWatching')}</span>
                        </div>
                        <div className="follow-card__actions">
                          <button type="button" className="profile-actionBtn profile-actionBtn--ghost" onClick={() => navigate(`/public-profile/${user.id}`)}>
                            {t('profileSimilarOpen')}
                          </button>
                          <button
                            type="button"
                            className="profile-actionBtn profile-actionBtn--primary"
                            disabled={isActive}
                            onClick={() => toggleFollow(user.id, false, t('followActionRemoved'))}
                          >
                            {t('followActionFollowing')}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>

              <section className="profile-similarBlock">
                <div className="profile-similarBlock__header">
                  <div>
                    <h3>{t('profileFollowersTitle')}</h3>
                    <p>{t('profileFollowersSubtitle')}</p>
                  </div>
                  <span className="profile-similarBlock__count">{followersUsers.length}</span>
                </div>
                {!followersUsers.length && <p className="profile-similar__hint">{t('profileFollowersEmpty')}</p>}
                <div className="profile-followGrid">
                  {followersUsers.map((user) => {
                    const isFollowing = followingIds.has(user.id);
                    const isActive = followActionUser === user.id;
                    return (
                      <div key={user.id} className="follow-card">
                        <div>
                          <strong>{user.name}</strong>
                          <span>{user.mutual ? t('profileFollowMutual') : t('profileFollowersWatching')}</span>
                        </div>
                        <div className="follow-card__actions">
                          <button type="button" className="profile-actionBtn profile-actionBtn--ghost" onClick={() => navigate(`/public-profile/${user.id}`)}>
                            {t('profileSimilarOpen')}
                          </button>
                          <button
                            type="button"
                            className="profile-actionBtn profile-actionBtn--primary"
                            disabled={isActive}
                            onClick={() => toggleFollow(user.id, !isFollowing, isFollowing ? t('followActionRemoved') : t('followActionAdded'))}
                          >
                            {isFollowing ? t('followActionFollowing') : t('followActionFollow')}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>

              <section className="profile-similarBlock">
                <div className="profile-similarBlock__header">
                  <div>
                    <h3>{t('profileSimilarTitle')}</h3>
                    <p>{t('profileSimilarSubtitle')}</p>
                  </div>
                  <span className="profile-similarBlock__count">{similarUsers.length}</span>
                </div>
                {similarLoading && <p className="profile-similar__hint">{t('profileSimilarLoading')}</p>}
                {!similarLoading && !similarUsers.length && <p className="profile-similar__hint">{t('profileSimilarEmpty')}</p>}
                <div className="profile-similarGrid">
                  {similarUsers.map((item) => {
                    const similarityPercent = Math.round(item.similarity * 100);
                    const profileInfo = item.profile;
                    const profileId = profileInfo?.id ?? null;
                    const initials = profileInfo?.name ? profileInfo.name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase() : '🎬';
                    const isFollowing = profileId ? followingIds.has(profileId) : false;
                    return (
                      <div key={item.userId} className="similar-card">
                        <div className="similar-card__avatar">{initials}</div>
                        <div className="similar-card__info">
                          <strong>{profileInfo?.name ?? t('profileSimilarAnon')}</strong>
                          <span>
                            {t('profileSimilarShared')} · {item.sharedMovies}
                          </span>
                          <span>
                            {similarityPercent}% {t('profileSimilarScore')}
                          </span>
                        </div>
                        {profileInfo && !profileInfo.profilePrivate && (
                          <div className="similar-card__ctaGroup">
                            <button type="button" className="profile-actionBtn profile-actionBtn--ghost" onClick={() => navigate(`/public-profile/${profileInfo.id}`)}>
                              {t('profileSimilarOpen')}
                            </button>
                            <button
                              type="button"
                              className="profile-actionBtn profile-actionBtn--primary"
                              disabled={followActionUser === profileInfo.id}
                              onClick={() =>
                                toggleFollow(
                                  profileInfo.id,
                                  !isFollowing,
                                  isFollowing ? t('followActionRemoved') : t('followActionAdded')
                                )
                              }
                            >
                              {isFollowing ? t('followActionFollowing') : t('followActionFollow')}
                            </button>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </section>
            </div>
          </section>
        )}
      </div>
    </div>
  );
};

export default ProfilePage;
