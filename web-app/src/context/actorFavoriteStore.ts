import { create } from 'zustand';
import { ActorFavorite } from '../api/movieService';

interface ActorFavoriteState {
  favorites: Record<number, ActorFavorite>;
  ordered: ActorFavorite[];
  loadedUserId?: number;
  setFavorites: (userId: number, items: ActorFavorite[]) => void;
  addFavorite: (favorite: ActorFavorite) => void;
  removeFavorite: (actorTmdbId: number) => void;
  reset: () => void;
}

const sortFavorites = (items: ActorFavorite[]) =>
  [...items].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

export const useActorFavoriteStore = create<ActorFavoriteState>((set) => ({
  favorites: {},
  ordered: [],
  loadedUserId: undefined,
  setFavorites: (userId, items) =>
    set({
      loadedUserId: userId,
      favorites: items.reduce<Record<number, ActorFavorite>>((acc, item) => {
        acc[item.actorTmdbId] = item;
        return acc;
      }, {}),
      ordered: sortFavorites(items)
    }),
  addFavorite: (favorite) =>
    set((state) => {
      const nextFavorites = { ...state.favorites, [favorite.actorTmdbId]: favorite };
      const without = state.ordered.filter((item) => item.actorTmdbId !== favorite.actorTmdbId);
      return {
        favorites: nextFavorites,
        ordered: [favorite, ...without]
      };
    }),
  removeFavorite: (actorTmdbId) =>
    set((state) => {
      if (!(actorTmdbId in state.favorites)) return state;
      const nextFavorites = { ...state.favorites };
      delete nextFavorites[actorTmdbId];
      return {
        favorites: nextFavorites,
        ordered: state.ordered.filter((item) => item.actorTmdbId !== actorTmdbId)
      };
    }),
  reset: () =>
    set({
      favorites: {},
      ordered: [],
      loadedUserId: undefined
    })
}));
