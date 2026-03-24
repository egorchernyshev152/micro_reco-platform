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
    <div className="auth-page">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>Регистрация</h1>
        <p className="text-muted">Создайте профиль, чтобы сохранять закладки и рекомендации.</p>
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
        <p>
          Уже есть аккаунт? <Link to="/login">Войдите</Link>
        </p>
      </form>
    </div>
  );
};

export default RegisterPage;
