import { create } from 'zustand';
import { Movie } from '../api/movieService';

interface DashboardCache {
  personal: Movie[];
  taste: Movie[];
  genre: Movie[];
  trend: Movie[];
  fresh: Movie[];
  favoriteActors: Movie[];
  updatedAt: number;
}

interface ProfileCache {
  watched: Movie[];
  watchlist: Movie[];
  favorites: Movie[];
  personal: Movie[];
  updatedAt: number;
}

interface CacheState {
  dashboard: DashboardCache | null;
  profile: ProfileCache | null;
  setDashboard: (payload: Omit<DashboardCache, 'updatedAt'>) => void;
  setProfile: (payload: Omit<ProfileCache, 'updatedAt'>) => void;
  reset: () => void;
}

const STORAGE_KEY = 'reco_cache';

const readInitial = (): Pick<CacheState, 'dashboard' | 'profile'> => {
  if (typeof window === 'undefined') {
    return { dashboard: null, profile: null };
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return { dashboard: null, profile: null };
    const parsed = JSON.parse(raw) as Partial<CacheState>;
    return {
      dashboard: parsed.dashboard ?? null,
      profile: parsed.profile ?? null
    };
  } catch (err) {
    console.warn('Failed to read cache', err);
    return { dashboard: null, profile: null };
  }
};

const persist = (state: Pick<CacheState, 'dashboard' | 'profile'>) => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
};

const initial = readInitial();

export const useCacheStore = create<CacheState>((set, get) => ({
  dashboard: initial.dashboard,
  profile: initial.profile,
  setDashboard: (payload) => {
    const nextDashboard: DashboardCache = {
      ...payload,
      updatedAt: Date.now()
    };
    const snapshot = { dashboard: nextDashboard, profile: get().profile };
    persist(snapshot);
    set({ dashboard: nextDashboard });
  },
  setProfile: (payload) => {
    const nextProfile: ProfileCache = {
      ...payload,
      updatedAt: Date.now()
    };
    const snapshot = { dashboard: get().dashboard, profile: nextProfile };
    persist(snapshot);
    set({ profile: nextProfile });
  },
  reset: () => {
    persist({ dashboard: null, profile: null });
    set({ dashboard: null, profile: null });
  }
}));
