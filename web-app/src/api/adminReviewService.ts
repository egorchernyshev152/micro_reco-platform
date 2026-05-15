import catalogApi from './catalogClient';

export type ReviewStatus = 'PENDING' | 'PUBLISHED' | 'SPAM' | 'DELETED';

export interface AdminReview {
  id: number;
  movieId: number;
  movieTitle: string;
  userId: number;
  userName: string;
  userEmail: string;
  score: number;
  content: string;
  status: ReviewStatus;
  flagged: boolean;
  lastModerationReason?: string | null;
  moderatedBy?: string | null;
  moderatedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminReviewStats {
  total: number;
  pending: number;
  published: number;
  spam: number;
  deleted: number;
  flagged: number;
}

export interface AdminReviewPage {
  items: AdminReview[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  stats: AdminReviewStats;
}

export interface AdminReviewQuery {
  query?: string;
  movieId?: number;
  userId?: number;
  status?: ReviewStatus;
  flagged?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ReviewStatusPayload {
  status: ReviewStatus;
  reason?: string;
}

export interface ReviewBulkActionPayload extends ReviewStatusPayload {
  ids: number[];
}

const adminReviewService = {
  listReviews: (query: AdminReviewQuery = {}) => {
    const params: Record<string, string | number | boolean> = {};
    if (query.query) params.query = query.query;
    if (typeof query.movieId === 'number') params.movieId = query.movieId;
    if (typeof query.userId === 'number') params.userId = query.userId;
    if (query.status) params.status = query.status;
    if (typeof query.flagged === 'boolean') params.flagged = query.flagged;
    if (typeof query.page === 'number') params.page = query.page;
    if (typeof query.size === 'number') params.size = query.size;
    if (query.sort) params.sort = query.sort;

    return catalogApi.get<AdminReviewPage>('/api/admin/reviews', { params }).then((res) => res.data);
  },
  updateStatus: (reviewId: number, payload: ReviewStatusPayload) =>
    catalogApi.patch<AdminReview>(`/api/admin/reviews/${reviewId}/status`, payload).then((res) => res.data),
  bulkAction: (payload: ReviewBulkActionPayload) =>
    catalogApi.post('/api/admin/reviews/bulk-action', payload).then((res) => res.data)
};

export default adminReviewService;
