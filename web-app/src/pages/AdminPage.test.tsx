import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AdminPage from './AdminPage';
import { useUserStore } from '../context/userStore';
import { adminUserService } from '../api/adminUserService';
import adminReviewService from '../api/adminReviewService';
import { adminAnalyticsService } from '../api/adminAnalyticsService';
import { movieService } from '../api/movieService';

vi.mock('../i18n/translations', () => ({
  useTranslation: () => ({ t: (key: string) => key })
}));

vi.mock('../api/adminUserService', () => ({
  adminUserService: {
    listUsers: vi.fn(),
    getComplaints: vi.fn(),
    getAuditLog: vi.fn(),
    updateBlockStatus: vi.fn(),
    updateRole: vi.fn(),
    deleteUser: vi.fn()
  }
}));

vi.mock('../api/adminReviewService', () => ({
  default: {
    listReviews: vi.fn(),
    updateStatus: vi.fn(),
    bulkAction: vi.fn()
  }
}));

vi.mock('../api/adminAnalyticsService', () => ({
  adminAnalyticsService: {
    summary: vi.fn(),
    popularity: vi.fn(),
    activity: vi.fn(),
    recommendations: vi.fn()
  }
}));

vi.mock('../api/movieService', () => ({
  movieService: {
    importFromTmdb: vi.fn()
  }
}));

const mockListUsers = adminUserService.listUsers as vi.Mock;
const mockGetComplaints = adminUserService.getComplaints as vi.Mock;
const mockGetAuditLog = adminUserService.getAuditLog as vi.Mock;
const mockUpdateBlock = adminUserService.updateBlockStatus as vi.Mock;
const mockDeleteUser = adminUserService.deleteUser as vi.Mock;
const mockListReviews = adminReviewService.listReviews as vi.Mock;
const mockReviewUpdate = adminReviewService.updateStatus as vi.Mock;
const mockReviewBulk = adminReviewService.bulkAction as vi.Mock;
const mockAnalyticsSummary = adminAnalyticsService.summary as vi.Mock;
const mockAnalyticsPopularity = adminAnalyticsService.popularity as vi.Mock;
const mockAnalyticsActivity = adminAnalyticsService.activity as vi.Mock;
const mockAnalyticsRecommendations = adminAnalyticsService.recommendations as vi.Mock;
const mockImportFromTmdb = movieService.importFromTmdb as vi.Mock;

describe('AdminPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });
  beforeEach(() => {
    useUserStore.setState({
      user: { id: 1, name: 'Admin', email: 'admin@example.com', role: 'ADMIN' },
      token: 'token'
    } as any);
    vi.clearAllMocks();
    const generatedAt = new Date().toISOString();
    mockAnalyticsSummary.mockResolvedValue({
      period: 'WEEK',
      generatedAt,
      totalEvents: 0,
      activeUsers: 0,
      avgEventsPerUser: 0,
      recommendationClicks: 0,
      recommendationStarts: 0,
      recommendationConversion: 0,
      trend: []
    });
    mockAnalyticsPopularity.mockResolvedValue({
      period: 'WEEK',
      generatedAt,
      topMovies: [],
      trend: []
    });
    mockAnalyticsActivity.mockResolvedValue({
      period: 'WEEK',
      generatedAt,
      activeUsers: 0,
      avgEventsPerUser: 0,
      segments: [],
      trend: [],
      hourlyDistribution: []
    });
    mockAnalyticsRecommendations.mockResolvedValue({
      period: 'WEEK',
      generatedAt,
      clicks: 0,
      watchStarts: 0,
      watchCompletions: 0,
      ratings: 0,
      conversionRate: 0,
      completionRate: 0,
      algorithms: [],
      trend: []
    });
    mockListUsers.mockResolvedValue({
      items: [],
      page: 0,
      size: 10,
      totalPages: 0,
      totalElements: 0,
      hasNext: false
    });
    mockGetComplaints.mockResolvedValue({ openCount: 0, reviewingCount: 0, resolvedCount: 0, complaints: [] });
    mockGetAuditLog.mockResolvedValue([]);
    mockUpdateBlock.mockResolvedValue(null);
    mockDeleteUser.mockResolvedValue(null);
    mockListReviews.mockResolvedValue({
      items: [],
      page: 0,
      size: 6,
      totalPages: 0,
      totalElements: 0,
      hasNext: false,
      stats: { total: 0, published: 0, spam: 0, deleted: 0, flagged: 0 }
    });
    mockReviewUpdate.mockResolvedValue(null);
    mockReviewBulk.mockResolvedValue(null);
    mockImportFromTmdb.mockResolvedValue({
      requestedPages: 0,
      processedPages: 0,
      importedMovies: 0,
      updatedMovies: 0,
      skippedMovies: 0
    });
  });

  it('renders table rows returned from API', async () => {
    mockListUsers.mockResolvedValue({
      items: [
        {
          id: 10,
          name: 'Тестовый пользователь',
          email: 'test@example.com',
          role: 'USER',
          blocked: false,
          complaintsCount: 2,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ],
      page: 0,
      size: 10,
      totalPages: 1,
      totalElements: 1,
      hasNext: false
    });

    render(
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    );

    const rows = await screen.findAllByTestId('admin-user-row');
    expect(rows).toHaveLength(1);
    expect(screen.getByText('Тестовый пользователь')).toBeInTheDocument();
    expect(mockListUsers).toHaveBeenCalledWith({
      query: undefined,
      role: undefined,
      blocked: undefined,
      page: 0,
      size: 10
    });
  });

  it('loads complaints and audit log for selected user and toggles block state', async () => {
    const testUser = {
      id: 5,
      name: 'Moderator',
      email: 'moderator@example.com',
      role: 'USER',
      blocked: false,
      complaintsCount: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    mockListUsers.mockResolvedValue({
      items: [testUser],
      page: 0,
      size: 10,
      totalPages: 1,
      totalElements: 1,
      hasNext: false
    });
    mockGetComplaints.mockResolvedValue({
      openCount: 1,
      reviewingCount: 0,
      resolvedCount: 0,
      complaints: [
        {
          id: 1,
          category: 'Spam',
          description: 'Много ссылок',
          status: 'PENDING',
          reporterName: 'User',
          reporterEmail: 'user@example.com',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ]
    });
    mockGetAuditLog.mockResolvedValue([
      { id: 1, action: 'BLOCK_UPDATED', details: 'blocked', createdAt: new Date().toISOString() }
    ]);
    mockUpdateBlock.mockResolvedValue({ ...testUser, blocked: true });

    render(
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Moderator')).toBeInTheDocument();
    await waitFor(() => expect(mockGetComplaints).toHaveBeenCalledWith(5));
    await waitFor(() => expect(mockGetAuditLog).toHaveBeenCalledWith(5));
    const blockButton = await screen.findByRole('button', { name: 'Заблокировать' });
    await userEvent.click(blockButton);
    expect(mockUpdateBlock).toHaveBeenCalledWith(5, { blocked: true, reason: undefined });
  });

  it('deletes a user after confirmation', async () => {
    mockListUsers.mockResolvedValue({
      items: [
        {
          id: 9,
          name: 'Delete Me',
          email: 'delete@example.com',
          role: 'USER',
          blocked: false,
          complaintsCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ],
      page: 0,
      size: 10,
      totalPages: 1,
      totalElements: 1,
      hasNext: false
    });
    mockGetComplaints.mockResolvedValue({ openCount: 0, reviewingCount: 0, resolvedCount: 0, complaints: [] });
    mockGetAuditLog.mockResolvedValue([]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    );

    const deleteButton = await screen.findByRole('button', { name: 'Удалить' });
    await userEvent.click(deleteButton);
    expect(mockDeleteUser).toHaveBeenCalledWith(9);
  });

  it('loads reviews feed and allows moderation actions', async () => {
    const now = new Date().toISOString();
    mockListReviews.mockResolvedValue({
      items: [
        {
          id: 100,
          movieId: 50,
          movieTitle: 'Demo Movie',
          userId: 77,
          userName: 'Critic',
          userEmail: 'critic@example.com',
          score: 6,
          content: 'Слишком много шума',
          status: 'PUBLISHED',
          flagged: true,
          createdAt: now,
          updatedAt: now
        }
      ],
      page: 0,
      size: 6,
      totalPages: 1,
      totalElements: 1,
      hasNext: false,
      stats: { total: 1, published: 1, spam: 0, deleted: 0, flagged: 1 }
    });
    mockReviewUpdate.mockResolvedValue({
      id: 100,
      movieId: 50,
      movieTitle: 'Demo Movie',
      userId: 77,
      userName: 'Critic',
      userEmail: 'critic@example.com',
      score: 6,
      content: 'Слишком много шума',
      status: 'SPAM',
      flagged: true,
      createdAt: now,
      updatedAt: now
    });

    render(
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    );

    const reviewRows = await screen.findAllByTestId('admin-review-row');
    expect(reviewRows).toHaveLength(1);
    const spamButton = within(reviewRows[0]).getByRole('button', { name: 'Спам' });
    await userEvent.click(spamButton);
    expect(mockReviewUpdate).toHaveBeenCalledWith(100, { status: 'SPAM', reason: undefined });
  });

  it('applies bulk moderation to selected reviews', async () => {
    const now = new Date().toISOString();
    mockListReviews.mockResolvedValue({
      items: [
        {
          id: 200,
          movieId: 51,
          movieTitle: 'Bulk Movie',
          userId: 80,
          userName: 'Bulk User',
          userEmail: 'bulk@example.com',
          score: 5,
          content: 'bulk',
          status: 'PUBLISHED',
          flagged: false,
          createdAt: now,
          updatedAt: now
        }
      ],
      page: 0,
      size: 6,
      totalPages: 1,
      totalElements: 1,
      hasNext: false,
      stats: { total: 1, published: 1, spam: 0, deleted: 0, flagged: 0 }
    });

    render(
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    );

    const checkbox = await screen.findByLabelText('Выбрать отзыв 200');
    await userEvent.click(checkbox);
    const bulkPanel = await screen.findByTestId('review-bulk-actions');
    const deleteButton = within(bulkPanel).getByRole('button', { name: 'Удалить' });
    await userEvent.click(deleteButton);
    expect(mockReviewBulk).toHaveBeenCalledWith({ ids: [200], status: 'DELETED', reason: undefined });
  });

  it('redirects non-admin users away from admin page', async () => {
    useUserStore.setState({
      user: { id: 2, name: 'User', email: 'user@example.com', role: 'USER' },
      token: 'token'
    } as any);

    render(
      <MemoryRouter>
        <AdminPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.queryByTestId('admin-page')).not.toBeInTheDocument();
    });
  });
});
