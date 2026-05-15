import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authService } from '../api/authService';
import { useUserStore } from '../context/userStore';
import './auth.css';

const RegisterPage = () => {
  const setSession = useUserStore((state) => state.setSession);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    authService
      .register({ name, email, password })
      .then((session) => {
        setSession(session);
        navigate('/');
      })
      .catch(() => setError('Не получилось создать аккаунт, попробуйте другой email'))
      .finally(() => setLoading(false));
  };

  return (
    <div className="auth-shell">
      <section className="auth-hero auth-hero--register">
        <span className="auth-badge">ReMovie</span>
        <h1>Создайте свой уютный кинотеатр</h1>
        <p>Расскажите нам пару фактов — и ReMovie подберёт фильмы под вашу атмосферу.</p>
        <div className="auth-perks">
          <div>
            <strong>100+ подборок</strong>
            <small>По актёрам и жанрам</small>
          </div>
          <div>
            <strong>Живые заметки</strong>
            <small>Фиксируйте эмоции и цитаты</small>
          </div>
          <div>
            <strong>Синхрон на всех устройствах</strong>
            <small>Начали дома — продолжили в дороге</small>
          </div>
          <div>
            <strong>Рекомендации по друзьям</strong>
            <small>Подсматривайте, что смотрят ваши люди</small>
          </div>
        </div>
        <p className="auth-caption">Ваша коллекция воспоминаний начнётся отсюда.</p>
      </section>

      <form className="auth-card" onSubmit={onSubmit}>
        <div>
          <p className="auth-card__eyebrow">Присоединяйтесь</p>
          <h2>Регистрация</h2>
        <p className="text-muted">Откройте все возможности персональных рекомендаций.</p>
        </div>
        <label>
          Имя
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Пароль
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </label>
        {error && <p className="auth-error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Создаем…' : 'Создать аккаунт'}
        </button>
        <p className="auth-switch">
          Уже есть аккаунт? <Link to="/login">Войдите</Link>
        </p>
      </form>
    </div>
  );
};

export default RegisterPage;
