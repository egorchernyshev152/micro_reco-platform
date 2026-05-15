import { Navigate, Route, Routes, Outlet } from 'react-router-dom';
import { ReactNode, useEffect, useState } from 'react';
import Layout from './components/Layout';
import NotificationCenter from './components/NotificationCenter';
import DashboardPage from './pages/DashboardPage';
import MovieDetailsPage from './pages/MovieDetailsPage';
import ProfilePage from './pages/ProfilePage';
import MoviesPage from './pages/MoviesPage';
import CollectionPage from './pages/CollectionPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SettingsPage from './pages/SettingsPage';
import PublicProfilePage from './pages/PublicProfilePage';
import AdminPage from './pages/AdminPage';
import MovieAdminPage from './pages/MovieAdminPage';
import AdminRecommendationsPage from './pages/AdminRecommendationsPage';
import ActorPage from './pages/ActorPage';
import { authService } from './api/authService';
import { useUserStore } from './context/userStore';
import { usePreferencesStore } from './context/preferencesStore';
import { useTranslation } from './i18n/translations';

const RequireAuth = () => {
  const token = useUserStore((state) => state.token);
  const user = useUserStore((state) => state.user);
  const setUser = useUserStore((state) => state.setUser);
  const logout = useUserStore((state) => state.logout);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    if (!token) {
      setChecking(false);
      return;
    }
    if (user) {
      setChecking(false);
      return;
    }
    let active = true;
    authService
      .me()
      .then((profile) => active && setUser(profile))
      .catch(() => logout())
      .finally(() => active && setChecking(false));
    return () => {
      active = false;
    };
  }, [token, user, setUser, logout]);

  if (checking) {
    return <div className="page-loader">Проверяем доступ…</div>;
  }

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};

const RequireAdmin = () => {
  const user = useUserStore((state) => state.user);
  if (!user) {
    return <Navigate to="/" replace />;
  }
  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
};

const PublicRoute = ({ children }: { children: ReactNode }) => {
  const token = useUserStore((state) => state.token);
  return token ? <Navigate to="/" replace /> : <>{children}</>;
};

const App = () => {
  const { t } = useTranslation();
  return (
    <>
      <ThemeAndLocaleWatcher />
      <NotificationCenter />
      <Routes>
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/movies" element={<MoviesPage />} />
            <Route path="/favorites" element={<CollectionPage type="FAVORITE" title={t('navFavorites')} emptyText={t('collectionEmpty')} />} />
            <Route path="/bookmarks" element={<CollectionPage type="WATCHLIST" title={t('navBookmarks')} emptyText={t('collectionEmpty')} />} />
            <Route path="/watched" element={<CollectionPage type="WATCHED" title={t('navWatched')} emptyText={t('collectionEmpty')} />} />
            <Route path="/movie/:movieId" element={<MovieDetailsPage />} />
            <Route path="/actor/:actorName" element={<ActorPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/public-profile/:userId" element={<PublicProfilePage />} />
            <Route element={<RequireAdmin />}>
              <Route path="/admin" element={<AdminPage />} />
              <Route path="/admin/movies" element={<MovieAdminPage />} />
              <Route path="/admin/reco" element={<AdminRecommendationsPage />} />
            </Route>
          </Route>
        </Route>
        <Route
          path="/login"
          element={
            <PublicRoute>
              <LoginPage />
            </PublicRoute>
          }
        />
        <Route
          path="/register"
          element={
            <PublicRoute>
              <RegisterPage />
            </PublicRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
};

const ThemeAndLocaleWatcher = () => {
  const theme = usePreferencesStore((state) => state.theme);
  const language = usePreferencesStore((state) => state.language);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  return null;
};

export default App;
