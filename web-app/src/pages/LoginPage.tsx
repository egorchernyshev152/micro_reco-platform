import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authService } from '../api/authService';
import { useUserStore } from '../context/userStore';
import './auth.css';

const LoginPage = () => {
  const setSession = useUserStore((state) => state.setSession);
  const [email, setEmail] = useState('demo@example.com');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    authService
      .login({ email, password })
      .then((session) => {
        setSession(session);
        navigate('/');
      })
      .catch(() => setError('Неверный email или пароль'))
      .finally(() => setLoading(false));
  };

  return (
    <div className="auth-shell">
      <section className="auth-hero">
        <span className="auth-badge">ReMovie</span>
        <h1>Добро пожаловать!</h1>
        <p>Твоя личная кинотека c рекомендациями, которые помнят настроение прошлого просмотра.</p>
        <div className="auth-perks">
          <div>
            <strong>Персональные подборки</strong>
            <small>Под настроение, актёров и жанры</small>
          </div>
          <div>
            <strong>Идеальная память</strong>
            <small>Заметки, цитаты и оценки в одном месте</small>
          </div>
          <div>
            <strong>ReMovie везде</strong>
            <small>Продолжайте просмотр на любом устройстве</small>
          </div>
          <div>
            <strong>Синхрон с друзьями</strong>
            <small>Обменивайтесь подборками и вкусами</small>
          </div>
        </div>
        <p className="auth-caption">Вдохновляйся. Сохраняй. Делись.</p>
      </section>

      <form className="auth-card" onSubmit={handleSubmit}>
        <div>
          <p className="auth-card__eyebrow">С возвращением!</p>
          <h2>Войдите в аккаунт</h2>
          <p className="text-muted">Продолжайте смотреть там, где остановились.</p>
        </div>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Пароль
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <p className="auth-error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Входим…' : 'Войти'}
        </button>
        <p className="auth-switch">
          Нет аккаунта? <Link to="/register">Создайте профиль</Link>
        </p>
      </form>
    </div>
  );
};

export default LoginPage;
