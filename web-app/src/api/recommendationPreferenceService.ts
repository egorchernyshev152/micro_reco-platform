import api from './client';

export interface RecommendationPreference {
  userId: number;
  boostGenres: string[];
  muteGenres: string[];
  freshnessBias: number;
  discoveryBias: number;
  updatedAt?: string;
}

export interface RecommendationPreferencePayload {
  boostGenres: string[];
  muteGenres: string[];
  freshnessBias: number;
  discoveryBias: number;
}

export const recommendationPreferenceService = {
  get: (userId: number) =>
    api.get<RecommendationPreference>(`/api/v1/users/${userId}/preferences`).then((res) => res.data),
  update: (userId: number, payload: RecommendationPreferencePayload) =>
    api.put<RecommendationPreference>(`/api/v1/users/${userId}/preferences`, payload).then((res) => res.data)
};
