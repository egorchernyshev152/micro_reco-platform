import { create } from 'zustand';

type NotesState = {
  notes: Record<number, string>;
  setNote: (movieId: number, content: string) => void;
  clearNote: (movieId: number) => void;
};

const STORAGE_KEY = 'reco_notes';

const readNotes = (): Record<number, string> => {
  if (typeof window === 'undefined') return {};
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    if (typeof parsed !== 'object' || parsed === null) return {};
    return parsed as Record<number, string>;
  } catch (err) {
    console.warn('Failed to parse notes', err);
    return {};
  }
};

const writeNotes = (notes: Record<number, string>) => {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
  } catch (err) {
    console.warn('Failed to persist notes', err);
  }
};

export const useNotesStore = create<NotesState>((set, get) => ({
  notes: readNotes(),
  setNote: (movieId, content) => {
    set((state) => {
      const next = { ...state.notes, [movieId]: content };
      writeNotes(next);
      return { notes: next };
    });
  },
  clearNote: (movieId) => {
    const current = get().notes;
    if (!(movieId in current)) return;
    const next = { ...current };
    delete next[movieId];
    writeNotes(next);
    set({ notes: next });
  }
}));
