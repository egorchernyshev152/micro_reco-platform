import { create } from 'zustand';

type RatingsMap = Record<number, number | null>;

interface RatingState {
  ratings: RatingsMap;
  setRating: (movieId: number, score: number) => void;
  removeRating: (movieId: number) => void;
  hydrateRating: (movieId: number, score: number | null) => void;
}

const STORAGE_KEY = 'reco_user_ratings';

const isBrowser = typeof window !== 'undefined';

const readInitialRatings = (): RatingsMap => {
  if (!isBrowser) return {};
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as RatingsMap) : {};
  } catch (err) {
    console.warn('Не удалось прочитать кеш оценок', err);
    return {};
  }
};

const persistRatings = (ratings: RatingsMap) => {
  if (!isBrowser) return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ratings));
  } catch (err) {
    console.warn('Не удалось сохранить кеш оценок', err);
  }
};

export const useRatingStore = create<RatingState>((set, get) => ({
  ratings: readInitialRatings(),
  setRating: (movieId, score) =>
    set((state) => {
      const next = { ...state.ratings, [movieId]: score };
      persistRatings(next);
      return { ratings: next };
    }),
  removeRating: (movieId) =>
    set((state) => {
      const next = { ...state.ratings, [movieId]: null };
      persistRatings(next);
      return { ratings: next };
    }),
  hydrateRating: (movieId, score) => {
    set((state) => {
      const next = { ...state.ratings, [movieId]: typeof score === 'number' ? score : null };
      persistRatings(next);
      return { ratings: next };
    });
  }
}));
