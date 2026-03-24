import api from './client';
import catalogApi from './catalogClient';

export interface CastMember {
  tmdbId?: number;
  name: string;
  character?: string;
  profileUrl?: string;
  orderIndex?: number;
}

export interface RecommendationItem {
  movie: Movie;
  score: number;
  popularityScore?: number;
  contentScore?: number;
}

export interface RecommendationResponse {
  algorithm: string;
  strategyId?: number;
  generatedAt: string;
  items: RecommendationItem[];
}

export interface Movie {
  id: number;
  title: string;
  originalTitle?: string;
  originalLanguage?: string;
  description?: string;
  synopsis?: string;
  releaseYear?: number;
  releaseDate?: string;
  durationMinutes?: number;
  ageRating?: string;
  tagline?: string;
  posterUrl?: string;
  backdropUrl?: string;
  genres: string[];
  countries: string[];
  tags: string[];
  averageRating?: number;
  ratingsCount?: number;
  cast?: CastMember[];
  budget?: number;
  revenue?: number;
}

export interface MovieDetail extends Movie {
  trailerUrl?: string;
  budget?: number;
  revenue?: number;
}

export interface MovieSearchFilters {
  query?: string;
  genres?: string[];
  countries?: string[];
  tags?: string[];
  ratingFrom?: number;
  ratingTo?: number;
  releaseYearFrom?: number;
  releaseYearTo?: number;
  durationFrom?: number;
  durationTo?: number;
  sort?: string;
  page?: number;
  limit?: number;
}

export interface MoviePageResponse {
  items: Movie[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface CatalogFilters {
  genres: string[];
  countries: string[];
  tags: string[];
  minYear?: number;
  maxYear?: number;
}

export type CollectionType = 'FAVORITE' | 'WATCHLIST' | 'WATCHED';

export interface CollectionSummary {
  movieId: number;
  types: CollectionType[];
}

export interface UserRatingDto {
  id?: number;
  userId?: number;
  movieId?: number;
  score: number;
}

const buildSearchParams = (filters?: MovieSearchFilters) => {
  if (!filters) return undefined;
  const params: Record<string, any> = {};
  if (filters.query) params.query = filters.query;
  if (filters.genres?.length) params.genres = filters.genres;
  if (filters.countries?.length) params.countries = filters.countries;
  if (filters.tags?.length) params.tags = filters.tags;
  if (filters.ratingFrom) params.ratingFrom = filters.ratingFrom;
  if (filters.ratingTo) params.ratingTo = filters.ratingTo;
  if (filters.releaseYearFrom) params.releaseYearFrom = filters.releaseYearFrom;
  if (filters.releaseYearTo) params.releaseYearTo = filters.releaseYearTo;
  if (filters.durationFrom) params.durationFrom = filters.durationFrom;
  if (filters.durationTo) params.durationTo = filters.durationTo;
  if (filters.sort) params.sort = filters.sort;
  if (typeof filters.page === 'number') params.page = filters.page;
  if (filters.limit) params.limit = filters.limit;
  return params;
};

export const movieService = {
  popular: (period?: string, limit = 10) =>
    api
      .get<RecommendationResponse>('/api/v1/movies/popular', {
        params: { period, limit }
      })
      .then((res) => res.data),

  trending: (period?: string, limit = 10) =>
    api
      .get<RecommendationResponse>('/api/v1/movies/trending', {
        params: { period, limit }
      })
      .then((res) => res.data),

  forUser: (userId: number, options: { limit?: number; period?: string; algo?: string } = {}) =>
    api
      .get<RecommendationResponse>(`/api/v1/movies/user/${userId}`, {
        params: {
          limit: options.limit ?? 10,
          period: options.period,
          algo: options.algo
        }
      })
      .then((res) => res.data),

  similar: (movieId: number, limit = 8) =>
    api.get<RecommendationResponse>(`/api/v1/movies/similar/${movieId}`, { params: { limit } }).then((res) => res.data),

  search: (filters?: MovieSearchFilters) => {
    return catalogApi
      .get<MoviePageResponse>('/movies', {
        params: buildSearchParams(filters),
        paramsSerializer: { indexes: null }
      })
      .then((res) => res.data);
  },

  filters: () => catalogApi.get<CatalogFilters>('/catalog/filters').then((res) => res.data),

  details: (movieId: number) => catalogApi.get<MovieDetail>(`/movies/${movieId}`).then((res) => res.data),

  getCollection: (userId: number, type: CollectionType) =>
    catalogApi.get<Movie[]>(`/users/${userId}/collections/${type}`).then((res) => res.data),

  addToCollection: (userId: number, movieId: number, type: CollectionType) =>
    catalogApi.post(`/users/${userId}/collections/${type}/${movieId}`),

  removeFromCollection: (userId: number, movieId: number, type: CollectionType) =>
    catalogApi.delete(`/users/${userId}/collections/${type}/${movieId}`),

  collectionSummary: (userId: number, movieIds: number[]) =>
    catalogApi
      .get<CollectionSummary[]>(`/users/${userId}/collections/summary`, {
        params: { movieIds },
        paramsSerializer: { indexes: null }
      })
      .then((res) => res.data),

  getUserRating: (movieId: number) =>
    catalogApi
      .get<UserRatingDto>(`/movies/${movieId}/rating`)
      .then((res) => res.data)
      .catch((error) => {
        if (error?.response?.status === 204) {
          return null;
        }
        throw error;
      }),

  setUserRating: (movieId: number, score: number) =>
    catalogApi.post<UserRatingDto>(`/movies/${movieId}/rating`, { score }).then((res) => res.data),

  removeUserRating: (movieId: number) => catalogApi.delete(`/movies/${movieId}/rating`),

  importFromTmdb: (payload: Record<string, unknown>) =>
    catalogApi.post('/internal/import/tmdb', payload).then((res) => res.data)
};
