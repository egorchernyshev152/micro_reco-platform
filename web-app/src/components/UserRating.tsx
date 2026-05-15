import { CSSProperties, useEffect, useMemo, useRef, useState } from 'react';
import { movieService } from '../api/movieService';
import { useUserStore } from '../context/userStore';
import { useRatingStore } from '../context/ratingStore';
import { useTranslation } from '../i18n/translations';
import './user-rating.css';

interface Props {
  movieId: number;
  variant?: 'default' | 'compact';
  className?: string;
  showCaption?: boolean;
  onRatingChange?: (score: number | null) => void;
}

const MIN_SCORE = 1;
const MAX_SCORE = 10;
const DEFAULT_SCORE = 5;

const UserRating = ({ movieId, variant = 'default', className, showCaption = true, onRatingChange }: Props) => {
  const user = useUserStore((state) => state.user);
  const hydrateRating = useRatingStore((state) => state.hydrateRating);
  const setRatingInStore = useRatingStore((state) => state.setRating);
  const removeRatingInStore = useRatingStore((state) => state.removeRating);
  const { t } = useTranslation();
  const [rating, setRating] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [sliderValue, setSliderValue] = useState(DEFAULT_SCORE);
  const commitTimer = useRef<number | null>(null);

  useEffect(() => {
    if (!user) return;
    let active = true;
    movieService
      .getUserRating(movieId)
      .then((res) => {
        if (active) {
          const score = typeof res?.score === 'number' ? res.score : null;
          setRating(score);
          hydrateRating(movieId, score);
        }
      })
      .catch(() => {
        if (active) {
          setRating(null);
          hydrateRating(movieId, null);
        }
      });
    return () => {
      active = false;
    };
  }, [movieId, user]);

  const onSet = (score: number) => {
    if (!user) return;
    setRating(score);
    setLoading(true);
    movieService
      .setUserRating(movieId, score)
      .catch(() => {
        alert(t('ratingError'));
      })
      .finally(() => setLoading(false));
    setRatingInStore(movieId, score);
  };

  const onRemove = () => {
    if (!user) return;
    setLoading(true);
    movieService
      .removeUserRating(movieId)
      .then(() => {
        setRating(null);
        removeRatingInStore(movieId);
        alert(t('ratingRemoved'));
      })
      .catch(() => alert(t('ratingError')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (typeof rating === 'number') {
      setSliderValue(rating);
      return;
    }
    setSliderValue(DEFAULT_SCORE);
  }, [rating]);

  useEffect(() => {
    return () => {
      if (commitTimer.current) {
        window.clearTimeout(commitTimer.current);
      }
    };
  }, []);

  useEffect(() => {
    onRatingChange?.(rating);
  }, [rating, onRatingChange]);

  const ratingProgress = useMemo(() => {
    const percent = ((sliderValue - MIN_SCORE) / (MAX_SCORE - MIN_SCORE)) * 100;
    const hueStart = 0;
    const hueEnd = 140;
    const hue = hueStart + (hueEnd - hueStart) * ((sliderValue - MIN_SCORE) / (MAX_SCORE - MIN_SCORE));
    return {
      percent: `${percent}%`,
      color: `hsl(${hue}, 75%, 55%)`
    };
  }, [sliderValue]);

  const commitSliderValue = (value: number) => {
    if (commitTimer.current) {
      window.clearTimeout(commitTimer.current);
    }
    commitTimer.current = window.setTimeout(() => {
      commitTimer.current = null;
      if (rating === value) return;
      onSet(value);
    }, 220);
  };

  const handleSliderChange = (value: number) => {
    setSliderValue(value);
    commitSliderValue(value);
  };

  if (!user) {
    return null;
  }

  const classes = ['user-rating', variant === 'compact' ? 'user-rating--compact' : '', className].filter(Boolean).join(' ');
  const valueIndicatorClass = ['user-rating__value-indicator', !showCaption ? 'user-rating__value-indicator--solo' : ''].filter(Boolean).join(' ');

  return (
    <div
      className={classes}
      style={
        {
          '--rating-color': ratingProgress.color,
          '--rating-percent': ratingProgress.percent
        } as CSSProperties
      }
    >
      <div className="user-rating__header">
        <h4>{t('ratingTitle')}</h4>
        {rating && (
          <button type="button" onClick={onRemove} disabled={loading}>
            {t('ratingRemove')}
          </button>
        )}
      </div>
      <div className="user-rating__slider">
        <div className={valueIndicatorClass}>
          {showCaption && <span>{t('ratingCurrent')}</span>}
          <strong>{sliderValue}</strong>
        </div>
        <input
          type="range"
          min={MIN_SCORE}
          max={MAX_SCORE}
          step={1}
          value={sliderValue}
          onChange={(e) => handleSliderChange(Number(e.target.value))}
          disabled={loading}
        />
        <div className="user-rating__scale" aria-hidden>
          <span>{MIN_SCORE}</span>
          <span>{MAX_SCORE}</span>
        </div>
      </div>
      {!rating && <p className="user-rating__hint">{t('ratingNotSet')}</p>}
    </div>
  );
};

export default UserRating;
