import { create } from 'zustand';

export type AppTheme = 'dark' | 'light';
export type AppLanguage = 'ru' | 'en';

interface PreferencesState {
  avatar: string;
  language: AppLanguage;
  theme: AppTheme;
  parentalControl: boolean;
  setAvatar: (emoji: string) => void;
  setLanguage: (lang: AppLanguage) => void;
  setTheme: (theme: AppTheme) => void;
  setParentalControl: (enabled: boolean) => void;
}

const STORAGE_KEY = 'reco_preferences';

const readInitial = () => {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Partial<PreferencesState>) : null;
  } catch (err) {
    console.warn('Failed to read preferences', err);
    return null;
  }
};

const persist = (state: PreferencesState) => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      avatar: state.avatar,
      language: state.language,
      theme: state.theme,
      parentalControl: state.parentalControl
    })
  );
};

const initial = readInitial();

export const usePreferencesStore = create<PreferencesState>((set, get) => ({
  avatar: initial?.avatar ?? '🙂',
  language: initial?.language ?? 'ru',
  theme: initial?.theme ?? 'dark',
  parentalControl: initial?.parentalControl ?? false,
  setAvatar: (avatar) => {
    const next = { ...get(), avatar };
    persist(next);
    set({ avatar });
  },
  setLanguage: (language) => {
    const next = { ...get(), language };
    persist(next);
    set({ language });
  },
  setTheme: (theme) => {
    const next = { ...get(), theme };
    persist(next);
    set({ theme });
  },
  setParentalControl: (parentalControl) => {
    const next = { ...get(), parentalControl };
    persist(next);
    set({ parentalControl });
  }
}));
