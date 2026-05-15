import { CSSProperties, useMemo } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import './sidebar.css';
import { useUserStore } from '../context/userStore';
import { useTranslation } from '../i18n/translations';

const Sidebar = () => {
  const user = useUserStore((state) => state.user);
  const logout = useUserStore((state) => state.logout);
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = useMemo(() => {
    const items = [
      { to: '/', exact: true, icon: '🎥', label: t('navHome') },
      { to: '/movies', icon: '📺', label: t('navMovies') },
      { to: '/favorites', icon: '♡', label: t('navFavorites') },
      { to: '/bookmarks', icon: '🔖', label: t('navBookmarks') },
      { to: '/watched', icon: '👁', label: t('navWatched') }
    ];
    if (user?.role === 'ADMIN') {
      items.push({ to: '/admin', icon: '🛠', label: t('navAdmin') });
    }
    return items;
  }, [t, user?.role]);

  const activeIndex = useMemo(() => {
    const index = navItems.findIndex((item) => {
      if (item.exact) {
        return location.pathname === item.to;
      }
      return location.pathname.startsWith(item.to);
    });
    return index >= 0 ? index : 0;
  }, [location.pathname, navItems]);

  const indicatorStyle = {
    '--active-index': activeIndex
  } as CSSProperties;

  return (
    <aside className="sidebar">
      <div className="sidebar__brand" title={user?.name ?? 'ReMovie'}>
        <span className="sidebar__logo">ReMovie</span>
      </div>

      <nav className="sidebar__nav" style={indicatorStyle}>
        <div className="sidebar__indicator" aria-hidden="true" />
        {navItems.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.exact} className={({ isActive }) => `sidebar__item ${isActive ? 'is-active' : ''}`}>
            <span className="sidebar__icon" aria-hidden="true">
              {item.icon}
            </span>
            <span className="sidebar__label">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <button className="sidebar__logout" onClick={handleLogout}>
        <span>↪</span>
        <span>{t('navLogout')}</span>
      </button>
    </aside>
  );
};

export default Sidebar;
