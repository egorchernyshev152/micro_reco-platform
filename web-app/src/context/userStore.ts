import { create } from 'zustand';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
}

interface AuthSession {
  token: string;
  user: AuthUser;
}

interface UserState {
  user: AuthUser | null;
  token: string | null;
  setSession: (session: AuthSession) => void;
  setUser: (user: AuthUser) => void;
  logout: () => void;
}

const STORAGE_KEY = 'reco_auth_session';

const readInitialSession = (): AuthSession | null => {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch (err) {
    console.error('Failed to read auth session', err);
    return null;
  }
};

const persistSession = (session: AuthSession) => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
};

const clearSession = () => {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(STORAGE_KEY);
};

const initialSession = readInitialSession();

export const useUserStore = create<UserState>((set, get) => ({
  user: initialSession?.user ?? null,
  token: initialSession?.token ?? null,
  setSession: (session) => {
    persistSession(session);
    set({ token: session.token, user: session.user });
  },
  setUser: (user) => {
    const token = get().token;
    if (token) {
      persistSession({ token, user });
    }
    set({ user });
  },
  logout: () => {
    clearSession();
    set({ user: null, token: null });
  }
}));
