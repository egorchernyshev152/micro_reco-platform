import api from './client';

export interface SimilarUserProfile {
  id: number;
  name: string;
  profilePrivate: boolean;
  favoritesCount?: number;
  watchlistCount?: number;
  watchedCount?: number;
}

export interface SimilarUser {
  userId: number;
  similarity: number;
  sharedMovies: number;
  sharedMovieIds: number[];
  profile?: SimilarUserProfile | null;
}

export interface SimilarUsersResponse {
  userId: number;
  period?: string | null;
  generatedAt: string;
  items: SimilarUser[];
}

export const userSimilarityService = {
  getSimilar(userId: number, options: { limit?: number; minOverlap?: number } = {}) {
    return api
      .get<SimilarUsersResponse>(`/api/v1/users/${userId}/similar`, {
        params: {
          limit: options.limit ?? 4,
          minOverlap: options.minOverlap ?? 2
        }
      })
      .then((res) => res.data);
  }
};
