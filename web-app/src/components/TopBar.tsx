import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './topbar.css';
import { usePreferencesStore } from '../context/preferencesStore';
import { useTranslation } from '../i18n/translations';
import { movieService, Movie, CatalogFilters } from '../api/movieService';

const SUGGESTION_LIMIT = 5;

const TopBar = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<CatalogFilters | null>(null);
  const [focused, setFocused] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const avatar = usePreferencesStore((state) => state.avatar);
  const { t } = useTranslation();
  const showSearch = location.pathname === '/';
  const searchRef = useRef<HTMLDivElement | null>(null);
  const trimmedQuery = query.trim();

  useEffect(() => {
    movieService.filters().then(setFilters).catch(() => setFilters(null));
  }, []);

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (!searchRef.current) return;
      if (!searchRef.current.contains(event.target as Node)) {
        setFocused(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  useEffect(() => {
    if (!showSearch) {
      setResults([]);
      return;
    }
    if (!trimmedQuery) {
      setResults([]);
      return;
    }
    let active = true;
    setLoading(true);
    const timer = window.setTimeout(() => {
      movieService
        .search({ query: trimmedQuery, limit: SUGGESTION_LIMIT })
        .then((page) => {
          if (!active) return;
          setResults(page.items);
        })
        .catch(() => {
          if (!active) return;
          setResults([]);
        })
        .finally(() => active && setLoading(false));
    }, 220);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [trimmedQuery, showSearch]);

  const genreMatches = useMemo(() => {
    if (!filters?.genres || !trimmedQuery) return [];
    const lower = trimmedQuery.toLowerCase();
    return Array.from(new Set(filters.genres))
      .map((genre) => genre.trim())
      .filter((genre) => genre.toLowerCase().includes(lower))
      .slice(0, 4);
  }, [filters?.genres, trimmedQuery]);

  const exactGenre = useMemo(() => {
    if (!trimmedQuery) return null;
    return genreMatches.find((genre) => genre.toLowerCase() === trimmedQuery.toLowerCase()) ?? null;
  }, [genreMatches, trimmedQuery]);

  const inferSearchPath = () => {
    if (!trimmedQuery) return '/movies';
    if (exactGenre) {
      return `/movies?genre=${encodeURIComponent(exactGenre)}`;
    }
    const words = trimmedQuery.split(/\s+/).filter(Boolean);
    if (words.length >= 2) {
      return `/movies?cast=${encodeURIComponent(trimmedQuery)}`;
    }
    return `/movies?query=${encodeURIComponent(trimmedQuery)}`;
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const target = inferSearchPath();
    navigate(target);
    setFocused(false);
  };

  const handleNavigate = (path: string) => {
    navigate(path);
    setFocused(false);
  };

  const showDropdown = showSearch && focused && Boolean(trimmedQuery);

  const actions = [
    {
      id: 'settings',
      icon: '⚙',
      label: t('topbarSettings'),
      onClick: () => navigate('/settings')
    },
    {
      id: 'profile',
      icon: avatar,
      label: t('topbarProfile'),
      onClick: () => navigate('/profile')
    }
  ] as const;

  return (
    <header className="topbar">
      {showSearch ? (
        <div className="topbar__searchArea" ref={searchRef}>
          <form className="topbar__searchWrap" onSubmit={onSubmit}>
            <button className="topbar__searchBtn" type="submit">
              ⌕
            </button>
            <input
              className="topbar__input"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onFocus={() => setFocused(true)}
              placeholder={t('catalogSearch')}
            />
          </form>
          {showDropdown && (
            <div className="topbar__suggestions">
              {loading && <p className="topbar__hint">{t('searchLoading')}</p>}
              {!loading && (
                <>
                  {results.length > 0 && (
                    <div className="topbar__group">
                      <p className="topbar__groupTitle">{t('searchMovies')}</p>
                      <ul>
                        {results.map((movie) => (
                          <li key={movie.id}>
                            <button
                              type="button"
                              onMouseDown={() => handleNavigate(`/movie/${movie.id}`)}
                            >
                              <span>{movie.title}</span>
                              {movie.releaseYear && <span>{movie.releaseYear}</span>}
                            </button>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {genreMatches.length > 0 && (
                    <div className="topbar__group">
                      <p className="topbar__groupTitle">{t('searchGenres')}</p>
                      <div className="topbar__chips">
                        {genreMatches.map((genre) => (
                          <button
                            key={genre}
                            type="button"
                            className="topbar__chip"
                            onMouseDown={() => handleNavigate(`/movies?genre=${encodeURIComponent(genre)}`)}
                          >
                            {genre}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {trimmedQuery.length >= 2 && (
                    <button
                      type="button"
                      className="topbar__actor"
                      onMouseDown={() => handleNavigate(`/movies?cast=${encodeURIComponent(trimmedQuery)}`)}
                    >
                      {t('searchActorHint')} «{trimmedQuery}»
                    </button>
                  )}

                  <button type="button" className="topbar__all" onMouseDown={() => handleNavigate(inferSearchPath())}>
                    {t('searchSeeAll')}
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="topbar__spacer" />
      )}

      <div className="topbar__actions">
        {actions.map((action) => (
          <button key={action.id} className="topbar__action" type="button" onClick={action.onClick}>
            <span className="topbar__circle">{action.icon}</span>
            <span className="topbar__actionLabel">{action.label}</span>
          </button>
        ))}
      </div>
    </header>
  );
};

export default TopBar;
