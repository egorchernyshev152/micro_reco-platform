import { Link } from 'react-router-dom';
import { ReactNode, useEffect } from 'react';
import { Movie, movieService } from '../api/movieService';
import './movie-card.css';
import { resolveImageUrl } from '../utils/imageUrl';
import { useNotesStore } from '../context/notesStore';
import { useTranslation } from '../i18n/translations';
import { useRatingStore } from '../context/ratingStore';
import { useUserStore } from '../context/userStore';
import { buildSynopsisPreview } from '../utils/text';

interface Props {
  movie: Movie;
  actions?: ReactNode;
  showMeta?: boolean;
}

const MovieCard = ({ movie, actions, showMeta = true }: Props) => {
  const catalogRatingValue = typeof movie.averageRating === 'number' ? movie.averageRating : movie.importedRating;
  const rating = typeof catalogRatingValue === 'number' ? catalogRatingValue.toFixed(1) : '—';
  const note = useNotesStore((state) => state.notes[movie.id]);
  const userId = useUserStore((state) => state.user?.id);
  const personalRating = useRatingStore((state) => state.ratings[movie.id]);
  const hydrateRating = useRatingStore((state) => state.hydrateRating);
  const { t } = useTranslation();

  const image = resolveImageUrl(movie.posterUrl) ?? resolveImageUrl(movie.backdropUrl, 'w780');
  const synopsisPreview = buildSynopsisPreview(movie.synopsis ?? movie.description);

  useEffect(() => {
    if (!userId) return;
    if (personalRating !== undefined) return;
    let active = true;
    movieService
      .getUserRating(movie.id)
      .then((res) => {
        if (!active) return;
        hydrateRating(movie.id, typeof res?.score === 'number' ? res.score : null);
      })
      .catch(() => active && hydrateRating(movie.id, null));
    return () => {
      active = false;
    };
  }, [userId, personalRating, movie.id, hydrateRating]);

  return (
    <Link to={`/movie/${movie.id}`} className="movie-card">
      <div className="movie-card__poster">
        {image ? (
          <img src={image} alt={movie.title} loading="lazy" referrerPolicy="no-referrer" />
        ) : (
          <div className="movie-card__fallback">Нет постера</div>
        )}

        {note && (
          <span className="movie-card__note" title={t('movieNoteBadge')}>
            ✎
          </span>
        )}

        <div className="movie-card__rating">
          <span title={t('movieCardCatalogRating')}>⭐ {rating}</span>
          {userId && personalRating !== undefined && (
            <span title={t('movieCardMyRating')}>👤 {typeof personalRating === 'number' ? personalRating : '—'}</span>
          )}
        </div>
      </div>
      <div className="movie-card__body">
        <div className="movie-card__title">{movie.title}</div>
        {showMeta && (
          <>
            <div className="movie-card__meta">
              {movie.releaseYear ? `${movie.releaseYear} • ` : ''}
              {movie.durationMinutes ? `${movie.durationMinutes} мин • ` : ''}
              {movie.genres.slice(0, 2).join(', ')}
            </div>
            {synopsisPreview && <p className="movie-card__synopsis">{synopsisPreview}</p>}
          </>
        )}
        {actions}
      </div>
    </Link>
  );
};

export default MovieCard;
