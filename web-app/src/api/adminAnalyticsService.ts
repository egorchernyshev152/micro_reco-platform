import api from './client';

export type AnalyticsPeriod = 'DAY' | 'WEEK' | 'MONTH';

export interface DailyMetricPoint {
  day: string;
  events: number;
  activeUsers?: number | null;
}

export interface PopularMovie {
  movieId: number;
  title: string;
  posterUrl?: string | null;
  events: number;
  share: number;
  eventTypes: Record<string, number>;
}

export interface PopularityTrendPoint {
  day: string;
  movieId: number;
  events: number;
}

export interface AdminAnalyticsSummary {
  period: AnalyticsPeriod;
  generatedAt: string;
  totalEvents: number;
  activeUsers: number;
  avgEventsPerUser: number;
  recommendationClicks: number;
  recommendationStarts: number;
  recommendationConversion: number;
  trend: DailyMetricPoint[];
}

export interface PopularityAnalytics {
  period: AnalyticsPeriod;
  generatedAt: string;
  topMovies: PopularMovie[];
  trend: PopularityTrendPoint[];
}

export interface ActivitySegment {
  segment: string;
  users: number;
  avgEvents: number;
}

export interface HourlyMetricPoint {
  hour: string;
  events: number;
}

export interface ActivityAnalytics {
  period: AnalyticsPeriod;
  generatedAt: string;
  activeUsers: number;
  avgEventsPerUser: number;
  segments: ActivitySegment[];
  trend: DailyMetricPoint[];
  hourlyDistribution: HourlyMetricPoint[];
}

export interface RecommendationAlgorithmBreakdown {
  algorithm: string;
  views: number;
  starts: number;
  finishes: number;
  ratings: number;
}

export interface RecommendationTrendPoint {
  day: string;
  views: number;
  starts: number;
  finishes: number;
}

export interface RecommendationAnalytics {
  period: AnalyticsPeriod;
  generatedAt: string;
  clicks: number;
  watchStarts: number;
  watchCompletions: number;
  ratings: number;
  conversionRate: number;
  completionRate: number;
  algorithms: RecommendationAlgorithmBreakdown[];
  trend: RecommendationTrendPoint[];
}

const buildParams = (period?: AnalyticsPeriod) => (period ? { period } : undefined);

export const adminAnalyticsService = {
  summary: (period?: AnalyticsPeriod) =>
    api.get<AdminAnalyticsSummary>('/api/admin/analytics/summary', { params: buildParams(period) }).then((res) => res.data),
  popularity: (period?: AnalyticsPeriod, limit = 5) =>
    api
      .get<PopularityAnalytics>('/api/admin/analytics/popularity', {
        params: { ...(buildParams(period) || {}), limit }
      })
      .then((res) => res.data),
  activity: (period?: AnalyticsPeriod) =>
    api.get<ActivityAnalytics>('/api/admin/analytics/activity', { params: buildParams(period) }).then((res) => res.data),
  recommendations: (period?: AnalyticsPeriod) =>
    api.get<RecommendationAnalytics>('/api/admin/analytics/recommendations', { params: buildParams(period) }).then((res) => res.data)
};
