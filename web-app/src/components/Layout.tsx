import { useEffect, useMemo, useState } from 'react';
import { Link, Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import { movieService, CatalogFilters } from '../api/movieService';
import './layout.css';
import { useTranslation } from '../i18n/translations';

const RightPanel = () => {
  const [filters, setFilters] = useState<CatalogFilters | null>(null);
  const { t } = useTranslation();

  useEffect(() => {
    movieService.filters().then(setFilters).catch(() => setFilters(null));
  }, []);

  const genres = useMemo(() => Array.from(new Set((filters?.genres ?? []).map((g) => g.trim()))), [filters?.genres?.join(',')]);
  const tags = useMemo(() => Array.from(new Set((filters?.tags ?? []).map((t) => t.trim()))), [filters?.tags?.join(',')]);

  return (
    <aside className="right-panel">
      <div className="right-panel__block">
        <h3>{t('catalogGenres')}</h3>
        <ul>
          {genres.slice(0, 5).map((genre) => (
            <li key={genre}>
              <Link to={`/movies?genre=${encodeURIComponent(genre)}`}>{genre}</Link>
            </li>
          ))}
        </ul>
        <Link to="/movies" className="ghost-button">
          Смотреть все
        </Link>
      </div>

      <div className="right-panel__block">
        <h3>{t('catalogTags')}</h3>
        <ul>
          {tags.slice(0, 5).map((tag) => (
            <li key={tag}>
              <Link to={`/movies?tag=${encodeURIComponent(tag)}`}>{tag}</Link>
            </li>
          ))}
        </ul>
        <Link to="/movies" className="ghost-button">
          Смотреть все
        </Link>
      </div>
    </aside>
  );
};

const Layout = () => {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-shell__content">
        <TopBar />
        <main className="app-shell__main">
          <Outlet />
        </main>
      </div>
      <RightPanel />
    </div>
  );
};

export default Layout;
