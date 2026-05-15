import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CatalogFilters, Movie, MovieSearchFilters, movieService } from '../api/movieService';
import { useUserStore } from '../context/userStore';
import MovieCard from '../components/MovieCard';
import MovieActions from '../components/MovieActions';
import './movies.css';
import { TranslationKey, useTranslation } from '../i18n/translations';
import { useCollectionStore } from '../context/collectionStore';
import { toSummaryMap } from '../utils/collectionSummary';
import { useInfiniteScroll } from '../utils/useInfiniteScroll';

const sortOptions: { value: string; labelKey: TranslationKey }[] = [
  { value: 'rating_desc', labelKey: 'sortRatingDesc' },
  { value: 'rating_asc', labelKey: 'sortRatingAsc' },
  { value: 'year_desc', labelKey: 'sortYearDesc' },
  { value: 'year_asc', labelKey: 'sortYearAsc' },
  { value: 'created_desc', labelKey: 'sortCreatedDesc' }
];

const MoviesPage = () => {
  const userId = useUserStore((state) => state.user?.id);
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState<MovieSearchFilters>({
    genres: [],
    countries: [],
    tags: [],
    cast: [],
    sort: 'rating_desc',
    limit: 12
  });
  const [options, setOptions] = useState<CatalogFilters | null>(null);
  const [movies, setMovies] = useState<Movie[]>([]);
  const [pageInfo, setPageInfo] = useState<{ page: number; hasNext: boolean } | null>(null);
  const [loadingState, setLoadingState] = useState<'idle' | 'initial' | 'append'>('idle');
  const mergeCollectionSummary = useCollectionStore((state) => state.mergeSummary);

  const isLoadingInitial = loadingState === 'initial';
  const isAppending = loadingState === 'append';

  useEffect(() => {
    const tag = searchParams.get('tag');
    const genre = searchParams.get('genre');
    const queryParam = searchParams.get('query');
    const castParam = searchParams.get('cast');
    if (!tag && !genre && queryParam === null && castParam === null) return;
    setFilters((prev) => {
      let updated = false;
      let next: MovieSearchFilters = { ...prev };
      if (tag) {
        next.tags = [tag];
        updated = true;
      }
      if (genre) {
        next.genres = [genre];
        updated = true;
      }
      if (queryParam !== null) {
        next.query = queryParam;
        next.cast = [];
        updated = true;
      }
      if (castParam !== null) {
        const castValue = castParam.trim();
        next.cast = castValue ? [castValue] : [];
        if (castValue) {
          next.query = '';
        }
        updated = true;
      }
      if (!updated) {
        return prev;
      }
      return { ...next };
    });
  }, [searchParams]);

  useEffect(() => {
    movieService.filters().then(setOptions).catch(() => setOptions(null));
  }, []);

  const loadPage = useCallback(
    (pageNumber: number, replace: boolean) => {
      setLoadingState(replace ? 'initial' : 'append');
      movieService
        .search({ ...filters, page: pageNumber })
        .then((response) => {
          setMovies((prev) => (replace ? response.items : [...prev, ...response.items]));
          setPageInfo({ page: pageNumber, hasNext: response.hasNext });
          if (!response.items.length || !userId) {
            return null;
          }
          return movieService.collectionSummary(
            userId,
            response.items.map((movie) => movie.id)
          );
        })
        .then((summary) => {
          if (!summary) return;
          mergeCollectionSummary(toSummaryMap(summary));
        })
        .finally(() => setLoadingState('idle'));
    },
    [filters, userId, mergeCollectionSummary]
  );

  useEffect(() => {
    setMovies([]);
    setPageInfo(null);
    loadPage(0, true);
  }, [loadPage]);

  const handleLoadMore = useCallback(() => {
    if (loadingState !== 'idle') return;
    if (!pageInfo?.hasNext) return;
    loadPage((pageInfo?.page ?? 0) + 1, false);
  }, [loadingState, pageInfo, loadPage]);

  const sentinelRef = useInfiniteScroll<HTMLDivElement>(handleLoadMore, {
    enabled: (pageInfo?.hasNext ?? false) && loadingState === 'idle'
  });

  const toggleGenre = (genre: string) => {
    setFilters((prev) => {
      const genres = prev.genres ?? [];
      const has = genres.includes(genre);
      const updated = has ? genres.filter((g) => g !== genre) : [...genres, genre];
      return { ...prev, genres: updated };
    });
  };

  const toggleCountry = (country: string) => {
    setFilters((prev) => {
      const countries = prev.countries ?? [];
      const has = countries.includes(country);
      const updated = has ? countries.filter((c) => c !== country) : [country];
      return { ...prev, countries: updated };
    });
  };

  const setQuery = (value: string) => {
    setFilters((prev) => ({ ...prev, query: value, cast: [] }));
  };

  const setSort = (value: string) => {
    setFilters((prev) => ({ ...prev, sort: value }));
  };

  const highlightedGenres = useMemo(() => new Set(filters.genres ?? []), [filters.genres?.join(',')]);
  const highlightedCountries = useMemo(() => new Set(filters.countries ?? []), [filters.countries?.join(',')]);

  const genreOptions = useMemo(() => Array.from(new Set((options?.genres ?? []).map((item) => item.trim()))), [options?.genres?.join(',')]);
  const countryOptions = useMemo(() => Array.from(new Set((options?.countries ?? []).map((item) => item.trim()))), [options?.countries?.join(',')]);

  return (
    <div className="movies-page">
      <header className="movies-page__header">
        <div>
          <p className="eyebrow">{t('catalogSubtitle')}</p>
          <h1>{t('catalogTitle')}</h1>
        </div>
        <div className="movies-page__search">
          <input
            type="search"
            placeholder={t('catalogSearch')}
            value={filters.query ?? ''}
            onChange={(e) => setQuery(e.target.value)}
          />
          <select value={filters.sort} onChange={(e) => setSort(e.target.value)}>
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {t(option.labelKey)}
              </option>
            ))}
          </select>
        </div>
      </header>

      <section className="movies-page__filters">
        <div>
          <h3>{t('catalogGenres')}</h3>
          <div className="chip-list">
            {genreOptions.slice(0, 12).map((genre) => (
              <button
                key={genre}
                className={`chip ${highlightedGenres.has(genre) ? 'is-active' : ''}`}
                onClick={() => toggleGenre(genre)}
              >
                {genre}
              </button>
            ))}
          </div>
        </div>
        <div>
          <h3>{t('catalogCountries')}</h3>
          <div className="chip-list">
            {countryOptions.slice(0, 8).map((country) => (
              <button
                key={country}
                className={`chip ${highlightedCountries.has(country) ? 'is-active' : ''}`}
                onClick={() => toggleCountry(country)}
              >
                {country}
              </button>
            ))}
          </div>
        </div>
      </section>

      {isLoadingInitial && !movies.length && <p>{t('catalogLoading')}</p>}

      <div className="movies-grid">
        {movies.map((movie: Movie) => (
          <MovieCard key={movie.id} movie={movie} actions={<MovieActions movieId={movie.id} />} />
        ))}
      </div>

      {!isLoadingInitial && movies.length === 0 && <p>{t('collectionEmpty')}</p>}

      <div ref={sentinelRef} className="movies-page__sentinel">
        {isAppending && <span>{t('catalogLoadingMore')}</span>}
      </div>
    </div>
  );
};

export default MoviesPage;
