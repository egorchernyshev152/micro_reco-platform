import { create } from 'zustand';
import { CollectionType } from '../api/movieService';

type SummaryMap = Record<number, CollectionType[]>;

interface CollectionStoreState {
  statuses: SummaryMap;
  setMovieTypes: (movieId: number, types?: CollectionType[]) => void;
  mergeSummary: (summary: SummaryMap) => void;
  markType: (movieId: number, type: CollectionType, isActive: boolean) => void;
  reset: () => void;
}

const normalize = (types?: CollectionType[]) => Array.from(new Set(types ?? []));

export const useCollectionStore = create<CollectionStoreState>((set) => ({
  statuses: {},
  setMovieTypes: (movieId, types) =>
    set((state) => {
      const next = { ...state.statuses };
      const normalized = normalize(types);
      if (!normalized.length) {
        delete next[movieId];
      } else {
        next[movieId] = normalized;
      }
      return { statuses: next };
    }),
  mergeSummary: (summary) =>
    set((state) => {
      const next = { ...state.statuses };
      Object.entries(summary).forEach(([id, list]) => {
        const movieId = Number(id);
        if (Number.isNaN(movieId)) {
          return;
        }
        const normalized = normalize(list);
        if (!normalized.length) {
          delete next[movieId];
        } else {
          next[movieId] = normalized;
        }
      });
      return { statuses: next };
    }),
  markType: (movieId, type, isActive) =>
    set((state) => {
      const next = { ...state.statuses };
      const current = new Set(next[movieId] ?? []);
      if (isActive) {
        current.add(type);
      } else {
        current.delete(type);
      }
      const normalized = Array.from(current);
      if (!normalized.length) {
        delete next[movieId];
      } else {
        next[movieId] = normalized;
      }
      return { statuses: next };
    }),
  reset: () => ({ statuses: {} })
}));
