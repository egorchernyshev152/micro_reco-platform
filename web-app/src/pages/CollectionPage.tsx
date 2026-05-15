import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CollectionType, Movie, movieService } from '../api/movieService';
import { useUserStore } from '../context/userStore';
import MovieCard from '../components/MovieCard';
import MovieActions from '../components/MovieActions';
import './movies.css';
import { useCollectionStore } from '../context/collectionStore';
import { toSummaryMap } from '../utils/collectionSummary';
import { useInfiniteScroll } from '../utils/useInfiniteScroll';
import { useActorFavoriteStore } from '../context/actorFavoriteStore';
import { useTranslation } from '../i18n/translations';

interface Props {
  type: CollectionType;
  title: string;
  emptyText: string;
}

const CollectionPage = ({ type, title, emptyText }: Props) => {
  const userId = useUserStore((state) => state.user?.id);
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(false);
  const [visibleCount, setVisibleCount] = useState(12);
  const mergeCollectionSummary = useCollectionStore((state) => state.mergeSummary);
  const actorFavorites = useActorFavoriteStore((state) => state.ordered);
  const actorFavoritesLoadedFor = useActorFavoriteStore((state) => state.loadedUserId);
  const setActorFavoritesStore = useActorFavoriteStore((state) => state.setFavorites);
  const resetActorFavorites = useActorFavoriteStore((state) => state.reset);

  useEffect(() => {
    if (!userId) return;
    let active = true;
    setLoading(true);
    movieService
      .getCollection(userId, type)
      .then((list) => {
        if (active) setMovies(list);
        if (!list.length) return null;
        return movieService.collectionSummary(
          userId,
          list.map((movie) => movie.id)
        );
      })
      .then((summary) => {
        if (!summary) return;
        mergeCollectionSummary(toSummaryMap(summary));
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [userId, type, mergeCollectionSummary]);

  useEffect(() => {
    setVisibleCount(12);
  }, [type]);

  useEffect(() => {
    setVisibleCount((prev) => Math.min(prev, movies.length || 12));
  }, [movies.length]);

  useEffect(() => {
    if (type !== 'FAVORITE') return;
    if (!userId) {
      resetActorFavorites();
      return;
    }
    if (actorFavoritesLoadedFor === userId) return;
    movieService
      .getFavoriteActors(userId)
      .then((data) => setActorFavoritesStore(userId, data))
      .catch(() => setActorFavoritesStore(userId, []));
  }, [type, userId, actorFavoritesLoadedFor, setActorFavoritesStore, resetActorFavorites]);

  useEffect(() => {
    if (type !== 'FAVORITE') return;
    if (window.location.hash === '#actors') {
      document.getElementById('actors')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [type, actorFavorites.length]);

  const loadMoreLocal = useCallback(() => {
    setVisibleCount((prev) => Math.min(prev + 8, movies.length));
  }, [movies.length]);

  const hasMoreLocal = visibleCount < movies.length;

  const sentinelRef = useInfiniteScroll<HTMLDivElement>(
    () => {
      if (!hasMoreLocal || loading) return;
      loadMoreLocal();
    },
    { enabled: hasMoreLocal && !loading }
  );

  return (
    <div className="movies-page">
      <header className="movies-page__header">
        <div>
          <p className="eyebrow">Коллекции</p>
          <h1>{title}</h1>
        </div>
      </header>
      {type === 'FAVORITE' && (
        <section className="favorite-actors-section" id="actors">
          <div className="favorite-actors-section__header">
            <h2>{t('favoritesActorsTitle')}</h2>
          </div>
          {!actorFavorites.length && <p>{t('favoritesActorsEmpty')}</p>}
          {actorFavorites.length > 0 && (
            <div className="favorite-actors-grid">
              {actorFavorites.map((actor) => (
                <button
                  key={actor.actorTmdbId}
                  type="button"
                  className="favorite-actorCard"
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
      )}
      {loading && <p>Загружаем…</p>}
      {!loading && !movies.length && <p>{emptyText}</p>}
      <div className="movies-grid">
        {movies.slice(0, visibleCount).map((movie) => (
          <MovieCard key={movie.id} movie={movie} actions={<MovieActions movieId={movie.id} />} />
        ))}
      </div>
      <div ref={sentinelRef} className="movies-page__sentinel">
        {hasMoreLocal && !loading && <span>• • •</span>}
      </div>
    </div>
  );
};

export default CollectionPage;
