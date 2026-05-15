import api from './client';
import catalogApi from './catalogClient';
import { usePreferencesStore } from '../context/preferencesStore';

export type MovieStatus = 'DRAFT' | 'READY' | 'PUBLISHED' | 'ARCHIVED';

export const MOVIE_STATUSES: MovieStatus[] = ['DRAFT', 'READY', 'PUBLISHED', 'ARCHIVED'];

export type MovieAssetType = 'POSTER' | 'BACKDROP' | 'GALLERY' | 'TRAILER_STILL';

export interface MovieAsset {
  id: number;
  movieId: number;
  type: MovieAssetType;
  url: string;
  fileName?: string;
  contentType?: string;
  size?: number;
  storage: string;
  label?: string;
  createdAt: string;
}

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
  status: MovieStatus;
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
  importedRating?: number;
  cast?: CastMember[];
  budget?: number;
  revenue?: number;
}

export interface MoviePayload {
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
  status?: MovieStatus;
  posterUrl?: string;
  backdropUrl?: string;
  trailerUrl?: string;
  budget?: number;
  revenue?: number;
  genres?: string[];
  countries?: string[];
  tags?: string[];
  cast?: CastMember[];
}

export interface MovieDetail extends Movie {
  trailerUrl?: string;
  budget?: number;
  revenue?: number;
}

export interface UserProfile {
  id: number;
  name: string;
  profilePrivate: boolean;
  favoritesCount?: number;
  watchlistCount?: number;
  watchedCount?: number;
  followersCount?: number;
  followingCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserPrivacyUpdateRequest {
  profilePrivate: boolean;
}

export interface MovieSearchFilters {
  query?: string;
  genres?: string[];
  countries?: string[];
  tags?: string[];
  cast?: string[];
  statuses?: MovieStatus[];
  ratingFrom?: number;
  ratingTo?: number;
  releaseYearFrom?: number;
  releaseYearTo?: number;
  durationFrom?: number;
  durationTo?: number;
  sort?: string;
  page?: number;
  limit?: number;
  excludeAdult?: boolean;
}

const shouldHideAdult = () => usePreferencesStore.getState().parentalControl;

const isAdultMovie = (movie?: { ageRating?: string; tags?: string[] }) => {
  if (!movie) return false;
  const rating = (movie.ageRating ?? '').toUpperCase();
  if (rating.includes('18+') || rating.startsWith('18') || rating.includes('NC-17') || rating.includes('NC17') || rating.includes('R18') || rating === 'X' || rating === 'XX' || rating === 'XXX') {
    return true;
  }
  if (movie.tags && movie.tags.length) {
    return movie.tags.some((tag) => {
      const normalized = (tag ?? '').toUpperCase();
      return normalized.includes('18+') || normalized.includes('ADULT') || normalized.includes('ЭРОТИКА') || normalized.includes('NUDE');
    });
  }
  return false;
};

const filterMovieList = <T extends { ageRating?: string; tags?: string[] }>(items: T[] | null | undefined): T[] => {
  if (!items) return [] as T[];
  if (!shouldHideAdult()) {
    return items;
  }
  return items.filter((item) => !isAdultMovie(item));
};

const sanitizeRecommendation = (response: RecommendationResponse) => {
  if (!shouldHideAdult() || !response?.items) {
    return response;
  }
  const items = response.items.filter((item) => !isAdultMovie(item?.movie));
  return { ...response, items };
};

const sanitizeMoviePage = (page: MoviePageResponse) => {
  if (!shouldHideAdult()) {
    return page;
  }
  return { ...page, items: filterMovieList(page.items) };
};

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
  statuses?: MovieStatus[];
  minYear?: number;
  maxYear?: number;
}

export type CollectionType = 'FAVORITE' | 'WATCHLIST' | 'WATCHED';

export interface CollectionSummary {
  movieId: number;
  types: CollectionType[];
}

export type ReviewStatus = 'PENDING' | 'PUBLISHED' | 'SPAM' | 'DELETED';

export interface MovieReview {
  id: number;
  movieId: number;
  authorId: number;
  authorName: string;
  score: number;
  content: string;
  status: ReviewStatus;
  flagged: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewPageResponse {
  items: MovieReview[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ReviewPayload {
  score: number;
  content: string;
}

export interface FollowStatus {
  followersCount: number;
  followingCount: number;
  following: boolean;
}

export interface FollowUser {
  id: number;
  name: string;
  profilePrivate: boolean;
  mutual: boolean;
  followedAt: string;
}

export interface ActorDetails {
  tmdbId: number;
  name: string;
  biography?: string;
  birthday?: string;
  deathday?: string;
  placeOfBirth?: string;
  profileUrl?: string;
  knownForDepartment?: string;
  popularity?: number;
  alsoKnownAs: string[];
  highlights: string[];
  knownFor: ActorKnownForEntry[];
}

export interface ActorKnownForEntry {
  title: string;
  year?: number;
  voteAverage?: number;
  catalogRating?: number;
  tmdbId?: number;
  movieId?: number;
  mediaType?: string;
  character?: string;
}

export interface ActorFavorite {
  actorTmdbId: number;
  actorName: string;
  profileUrl?: string;
  createdAt: string;
}

export interface ActorFavoritePayload {
  actorName: string;
  profileUrl?: string;
}

export interface UserRatingDto {
  id?: number;
  userId?: number;
  movieId?: number;
  score: number;
}

export interface MovieImportResponse {
  requestedPages: number;
  processedPages: number;
  importedMovies: number;
  updatedMovies: number;
  skippedMovies: number;
}

export interface ImportMoviesPayload {
  pages: number;
  language: string;
  originalLanguage?: string;
  originCountry?: string;
  yearFrom?: number;
  yearTo?: number;
  minVoteCount?: number;
  minVoteAverage?: number;
  includeAdult?: boolean;
  genreIds?: number[];
}

const buildSearchParams = (filters?: MovieSearchFilters) => {
  if (!filters) return undefined;
  const params: Record<string, any> = {};
  if (filters.query) params.query = filters.query;
  if (filters.genres?.length) params.genres = filters.genres;
  if (filters.countries?.length) params.countries = filters.countries;
  if (filters.tags?.length) params.tags = filters.tags;
  if (filters.cast?.length) params.cast = filters.cast;
  if (filters.statuses?.length) params.statuses = filters.statuses;
  if (filters.ratingFrom) params.ratingFrom = filters.ratingFrom;
  if (filters.ratingTo) params.ratingTo = filters.ratingTo;
  if (filters.releaseYearFrom) params.releaseYearFrom = filters.releaseYearFrom;
  if (filters.releaseYearTo) params.releaseYearTo = filters.releaseYearTo;
  if (filters.durationFrom) params.durationFrom = filters.durationFrom;
  if (filters.durationTo) params.durationTo = filters.durationTo;
  if (typeof filters.excludeAdult === 'boolean') params.excludeAdult = filters.excludeAdult;
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
      .then((res) => sanitizeRecommendation(res.data)),

  trending: (period?: string, limit = 10) =>
    api
      .get<RecommendationResponse>('/api/v1/movies/trending', {
        params: { period, limit }
      })
      .then((res) => sanitizeRecommendation(res.data)),

  forUser: (userId: number, options: { limit?: number; period?: string; algo?: string; strategyId?: number } = {}) =>
    api
      .get<RecommendationResponse>(`/api/v1/movies/user/${userId}`, {
        params: {
          limit: options.limit ?? 10,
          period: options.period,
          algo: options.algo,
          strategyId: options.strategyId
        }
      })
      .then((res) => sanitizeRecommendation(res.data)),

  similar: (movieId: number, limit = 8) =>
    api
      .get<RecommendationResponse>(`/api/v1/movies/similar/${movieId}`, { params: { limit } })
      .then((res) => sanitizeRecommendation(res.data)),

  search: (filters?: MovieSearchFilters) => {
    return catalogApi
      .get<MoviePageResponse>('/movies', {
        params: buildSearchParams(filters),
        paramsSerializer: { indexes: null }
      })
      .then((res) => sanitizeMoviePage(res.data));
  },

  filters: () => catalogApi.get<CatalogFilters>('/catalog/filters').then((res) => res.data),

  details: (movieId: number) => catalogApi.get<MovieDetail>(`/movies/${movieId}`).then((res) => res.data),

  getCollection: (userId: number, type: CollectionType) =>
    catalogApi.get<Movie[]>(`/users/${userId}/collections/${type}`).then((res) => filterMovieList(res.data)),
  getPublicCollection: (userId: number, type: CollectionType) =>
    catalogApi.get<Movie[]>(`/public/users/${userId}/collections/${type}`).then((res) => filterMovieList(res.data)),

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

  getReviews: (movieId: number, page = 0, size = 6) =>
    catalogApi
      .get<ReviewPageResponse>(`/movies/${movieId}/reviews`, { params: { page, size } })
      .then((res) => res.data),

  getMyReview: (movieId: number) =>
    catalogApi
      .get<MovieReview>(`/movies/${movieId}/reviews/my`)
      .then((res) => res.data)
      .catch((error) => {
        if (error?.response?.status === 204) {
          return null;
        }
        throw error;
      }),

  submitReview: (movieId: number, payload: ReviewPayload) =>
    catalogApi.post<MovieReview>(`/movies/${movieId}/reviews`, payload).then((res) => res.data),

  followUser: (targetUserId: number) =>
    catalogApi.post<FollowStatus>(`/users/${targetUserId}/follow`).then((res) => res.data),

  unfollowUser: (targetUserId: number) =>
    catalogApi.delete<FollowStatus>(`/users/${targetUserId}/follow`).then((res) => res.data),

  followStatus: (targetUserId: number) =>
    catalogApi.get<FollowStatus>(`/users/${targetUserId}/follow/status`).then((res) => res.data),

  getFollowingUsers: (userId: number, limit = 12) =>
    catalogApi
      .get<FollowUser[]>(`/users/${userId}/following`, { params: { limit } })
      .then((res) => res.data),

  getFollowerUsers: (userId: number, limit = 12) =>
    catalogApi
      .get<FollowUser[]>(`/users/${userId}/followers`, { params: { limit } })
      .then((res) => res.data),

  importFromTmdb: (payload: ImportMoviesPayload) =>
    catalogApi.post<MovieImportResponse>('/internal/import/tmdb', payload).then((res) => res.data),
  getProfile: (userId: number) => catalogApi.get<UserProfile>(`/users/${userId}/profile`).then((res) => res.data),
  updateProfilePrivacy: (userId: number, profilePrivate: boolean) =>
    catalogApi.put<UserProfile>(`/users/${userId}/profile/privacy`, { profilePrivate } satisfies UserPrivacyUpdateRequest).then((res) => res.data),
  getPublicProfile: (userId: number) => catalogApi.get<UserProfile>(`/public/users/${userId}`).then((res) => res.data),

  createMovie: (payload: MoviePayload) => catalogApi.post<Movie>('/movies', payload).then((res) => res.data),

  updateMovie: (movieId: number, payload: MoviePayload) =>
    catalogApi.put<Movie>(`/movies/${movieId}`, payload).then((res) => res.data),

  deleteMovie: (movieId: number) => catalogApi.delete(`/movies/${movieId}`),

  uploadAsset: (movieId: number, file: File, type: MovieAssetType, label?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (type) {
      formData.append('type', type);
    }
    if (label) {
      formData.append('label', label);
    }
    return catalogApi
      .post<MovieAsset>(`/movies/${movieId}/assets`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then((res) => res.data);
  },

  getFavoriteActors: (userId: number) =>
    catalogApi.get<ActorFavorite[]>(`/users/${userId}/favorite-actors`).then((res) => res.data),

  addFavoriteActor: (userId: number, actorTmdbId: number, payload: ActorFavoritePayload) =>
    catalogApi.post<ActorFavorite>(`/users/${userId}/favorite-actors/${actorTmdbId}`, payload).then((res) => res.data),

  removeFavoriteActor: (userId: number, actorTmdbId: number) =>
    catalogApi.delete(`/users/${userId}/favorite-actors/${actorTmdbId}`),

  getActorDetails: (tmdbId: number, language = 'ru-RU') =>
    catalogApi.get<ActorDetails>(`/actors/${tmdbId}`, { params: { language } }).then((res) => res.data)
};
