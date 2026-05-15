import { useMemo, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CollectionType, movieService } from '../api/movieService';
import { useUserStore } from '../context/userStore';
import { useCollectionStore } from '../context/collectionStore';
import './movie-actions.css';

interface MovieActionsProps {
  movieId: number;
  activeTypes?: CollectionType[];
}

const ORDER: CollectionType[] = ['FAVORITE', 'WATCHLIST', 'WATCHED'];

const labels: Record<CollectionType, { title: string; icon: string }> = {
  FAVORITE: { title: 'В избранное', icon: '♡' },
  WATCHLIST: { title: 'В закладки', icon: '🔖' },
  WATCHED: { title: 'Просмотрен', icon: '👁' }
};

export const MovieActions = ({ movieId, activeTypes }: MovieActionsProps) => {
  const userId = useUserStore((state) => state.user?.id);
  const navigate = useNavigate();
  const storeTypes = useCollectionStore((state) => state.statuses[movieId]);
  const setMovieTypes = useCollectionStore((state) => state.setMovieTypes);
  const markType = useCollectionStore((state) => state.markType);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!activeTypes || !activeTypes.length) return;
    const next = Array.from(new Set(activeTypes));
    const current = storeTypes ?? [];
    const equal =
      current.length === next.length && next.every((type) => current.includes(type));
    if (equal) return;
    setMovieTypes(movieId, next);
  }, [movieId, activeTypes?.join(','), storeTypes?.join(','), setMovieTypes]);

  const activeSet = useMemo(() => {
    const source = storeTypes ?? activeTypes ?? [];
    return new Set(source);
  }, [storeTypes?.join(','), activeTypes?.join(',')]);

  const toggle = (type: CollectionType) => {
    if (!userId) {
      navigate('/login');
      return;
    }
    const wasActive = activeSet.has(type);
    const previous = Array.from(activeSet);
    markType(movieId, type, !wasActive);
    setBusy(true);
    const action = wasActive ? movieService.removeFromCollection : movieService.addToCollection;
    action(userId, movieId, type)
      .catch((err) => {
        console.error('Collection update failed', err);
        const message = err?.response?.data?.message ?? 'Не удалось обновить список. Попробуйте позже.';
        alert(message);
        setMovieTypes(movieId, previous);
      })
      .finally(() => setBusy(false));
  };

  return (
    <div className="movie-actions">
      {ORDER.map((type) => {
        const active = activeSet.has(type);
        return (
          <button
            key={type}
            className={`movie-action ${active ? 'is-active' : ''}`}
            disabled={busy}
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              toggle(type);
            }}
            title={labels[type].title}
          >
            <span>{labels[type].icon}</span>
          </button>
        );
      })}
    </div>
  );
};

export default MovieActions;
