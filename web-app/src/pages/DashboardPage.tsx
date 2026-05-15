import { useEffect, useRef, useState } from 'react';
import { movieService, Movie } from '../api/movieService';
import { useUserStore } from '../context/userStore';
import RecommendationShelf from '../components/RecommendationShelf';
import { useTranslation } from '../i18n/translations';
import { useCacheStore } from '../context/cacheStore';
import { useActorFavoriteStore } from '../context/actorFavoriteStore';
import { useCollectionStore } from '../context/collectionStore';
import { toSummaryMap } from '../utils/collectionSummary';

const DashboardPage = () => {
  const userId = useUserStore((state) => state.user?.id);
  const dashboardCache = useCacheStore((state) => state.dashboard);
  const setDashboardCache = useCacheStore((state) => state.setDashboard);
  const [personal, setPersonal] = useState<Movie[]>(dashboardCache?.personal ?? []);
  const [tasteShelf, setTasteShelf] = useState<Movie[]>(dashboardCache?.taste ?? []);
  const [genreShelf, setGenreShelf] = useState<Movie[]>(dashboardCache?.genre ?? []);
  const [freshShelf, setFreshShelf] = useState<Movie[]>(dashboardCache?.fresh ?? []);
  const [favoriteActorShelf, setFavoriteActorShelf] = useState<Movie[]>(dashboardCache?.favoriteActors ?? []);
  const [trendShelf, setTrendShelf] = useState<Movie[]>(dashboardCache?.trend ?? []);
  const [userShelvesLoading, setUserShelvesLoading] = useState(!dashboardCache?.personal?.length);
  const [globalShelvesLoading, setGlobalShelvesLoading] = useState(!dashboardCache?.fresh?.length);
  const [favoriteShelfLoading, setFavoriteShelfLoading] = useState(false);
  const { t } = useTranslation();
  const shelvesRef = useRef({
    personal: dashboardCache?.personal ?? [],
    taste: dashboardCache?.taste ?? [],
    genre: dashboardCache?.genre ?? [],
    trend: dashboardCache?.trend ?? [],
    fresh: dashboardCache?.fresh ?? [],
    favoriteActors: dashboardCache?.favoriteActors ?? []
  });
  const favoriteActors = useActorFavoriteStore((state) => state.ordered);
  const favoriteActorsLoadedFor = useActorFavoriteStore((state) => state.loadedUserId);
  const setFavoriteActorsStore = useActorFavoriteStore((state) => state.setFavorites);
  const resetFavoriteActorsStore = useActorFavoriteStore((state) => state.reset);
  const collectionSummaryStore = useCollectionStore((state) => state.statuses);
  const mergeCollectionSummary = useCollectionStore((state) => state.mergeSummary);

  const updateCache = (patch: Partial<typeof shelvesRef.current>) => {
    shelvesRef.current = { ...shelvesRef.current, ...patch };
    setDashboardCache(shelvesRef.current);
  };

  useEffect(() => {
    shelvesRef.current = {
      personal,
      taste: tasteShelf,
      genre: genreShelf,
      trend: trendShelf,
      fresh: freshShelf,
      favoriteActors: favoriteActorShelf
    };
  }, [personal, tasteShelf, genreShelf, trendShelf, freshShelf, favoriteActorShelf]);

  useEffect(() => {
    if (!userId) return;
    let active = true;
    Promise.all([
      movieService.forUser(userId, { limit: 8, period: 'MONTH', algo: 'HYBRID' }),
      movieService.forUser(userId, { limit: 8, period: 'WEEK', algo: 'CO_OCCURRENCE' }),
      movieService.forUser(userId, { limit: 8, period: 'MONTH', algo: 'CONTENT_BASED' })
    ])
      .then(([hybrid, taste, genre]) => {
        if (!active) return;
        const personalItems = hybrid.items.map((item) => item.movie);
        const tasteItems = taste.items.map((item) => item.movie);
        const genreItems = genre.items.map((item) => item.movie);
        setPersonal(personalItems);
        setTasteShelf(tasteItems);
        setGenreShelf(genreItems);
        updateCache({ personal: personalItems, taste: tasteItems, genre: genreItems });
      })
      .catch(() => {
        if (!active) return;
        setPersonal([]);
        setTasteShelf([]);
        setGenreShelf([]);
      })
      .finally(() => active && setUserShelvesLoading(false));
    return () => {
      active = false;
    };
  }, [userId]);

  useEffect(() => {
    if (!userId) {
      resetFavoriteActorsStore();
      return;
    }
    if (favoriteActorsLoadedFor === userId) return;
    let active = true;
    movieService
      .getFavoriteActors(userId)
      .then((data) => {
        if (active) {
          setFavoriteActorsStore(userId, data);
        }
      })
      .catch(() => active && setFavoriteActorsStore(userId, []));
    return () => {
      active = false;
    };
  }, [userId, favoriteActorsLoadedFor, setFavoriteActorsStore, resetFavoriteActorsStore]);

  useEffect(() => {
    let active = true;
    Promise.all([movieService.trending('WEEK', 8), movieService.popular('MONTH', 8)])
      .then(([trendRes, freshRes]) => {
        if (!active) return;
        const trendItems = trendRes.items.map((item) => item.movie);
        const freshItems = freshRes.items.map((item) => item.movie);
        setTrendShelf(trendItems);
        setFreshShelf(freshItems);
        updateCache({ trend: trendItems, fresh: freshItems });
      })
      .catch(() => {
        if (!active) return;
        setTrendShelf([]);
        setFreshShelf([]);
      })
      .finally(() => active && setGlobalShelvesLoading(false));
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!userId || !favoriteActors.length) {
      setFavoriteActorShelf([]);
      updateCache({ favoriteActors: [] });
      setFavoriteShelfLoading(false);
      return;
    }
    let active = true;
    setFavoriteShelfLoading(true);
    const castNames = favoriteActors.slice(0, 5).map((actor) => actor.actorName);
    movieService
      .search({ cast: castNames, limit: 24 })
      .then((page) => {
        if (!active) return null;
        const items = page.items;
        if (!items.length) {
          setFavoriteActorShelf([]);
          updateCache({ favoriteActors: [] });
          return null;
        }
        return movieService.collectionSummary(userId, items.map((movie) => movie.id)).then((summary) => ({
          items,
          summary
        }));
      })
      .then((result) => {
        if (!result || !active) return;
        const summaryMap = result.summary ? toSummaryMap(result.summary) : {};
        if (result.summary) {
          mergeCollectionSummary(summaryMap);
        }
        const filtered = result.items.filter((movie) => {
          const summaryTypes = summaryMap[movie.id] ?? [];
          const storeTypes = collectionSummaryStore[movie.id] ?? [];
          const combined = new Set([...summaryTypes, ...storeTypes]);
          return !combined.has('WATCHED') && !combined.has('FAVORITE');
        });
        const sliced = filtered.slice(0, 12);
        setFavoriteActorShelf(sliced);
        updateCache({ favoriteActors: sliced });
      })
      .catch(() => {
        if (!active) return;
        setFavoriteActorShelf([]);
        updateCache({ favoriteActors: [] });
      })
      .finally(() => active && setFavoriteShelfLoading(false));
    return () => {
      active = false;
    };
  }, [userId, favoriteActors, collectionSummaryStore, mergeCollectionSummary]);

  return (
    <div className="dashboard">
      <RecommendationShelf title={t('navHome')} movies={personal} loading={userShelvesLoading} />
      {favoriteActorShelf.length > 0 && (
        <RecommendationShelf title={t('dashboardFavoriteActors')} movies={favoriteActorShelf} loading={favoriteShelfLoading} />
      )}
      <RecommendationShelf title={t('dashboardTaste')} movies={tasteShelf} loading={userShelvesLoading} />
      <RecommendationShelf title={t('dashboardGenres')} movies={genreShelf} loading={userShelvesLoading} />
      <RecommendationShelf title={t('dashboardFresh')} movies={freshShelf} loading={globalShelvesLoading} />
      <RecommendationShelf title={t('dashboardTrending')} movies={trendShelf} loading={globalShelvesLoading} />
    </div>
  );
};

export default DashboardPage;
