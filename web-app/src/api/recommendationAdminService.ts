import api from './client';

export type RecommendationRebuildStatus = 'SCHEDULED' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export type RecommendationTrainingPeriod = 'DAY' | 'WEEK' | 'MONTH';

export type RecommendationAlgorithm = 'POPULARITY' | 'CO_OCCURRENCE' | 'CONTENT_BASED' | 'HYBRID' | 'ML_EMBEDDING';

export interface RecommendationRebuildLog {
  id: number;
  status: RecommendationRebuildStatus;
  processedUsers: number;
  totalUsers: number;
  startedAt: string;
  finishedAt?: string;
  initiator?: string;
  trainingPeriod?: RecommendationTrainingPeriod;
  message?: string;
}

export interface RecommendationConfig {
  enabled: boolean;
  trainingPeriod: RecommendationTrainingPeriod;
  defaultAlgorithm: RecommendationAlgorithm;
  defaultStrategyId?: number;
  recommendationLimit: number;
  rebuildBatchSize: number;
  maxUsersPerJob: number;
  createdAt: string;
  updatedAt: string;
  activeRebuild?: RecommendationRebuildLog;
  lastRebuild?: RecommendationRebuildLog;
}

export interface RecommendationConfigPayload {
  enabled: boolean;
  trainingPeriod: RecommendationTrainingPeriod;
  defaultAlgorithm: RecommendationAlgorithm;
  defaultStrategyId?: number;
  recommendationLimit: number;
  rebuildBatchSize: number;
  maxUsersPerJob: number;
}

export interface RecommendationRebuildRequest {
  initiator?: string;
}

export const recommendationAdminService = {
  getConfig: () => api.get<RecommendationConfig>('/api/v1/recommendations/config').then((res) => res.data),

  updateConfig: (payload: RecommendationConfigPayload) =>
    api.put<RecommendationConfig>('/api/v1/recommendations/config', payload).then((res) => res.data),

  triggerRebuild: (payload: RecommendationRebuildRequest) =>
    api.post<RecommendationRebuildLog>('/api/v1/recommendations/rebuild', payload).then((res) => res.data)
};
