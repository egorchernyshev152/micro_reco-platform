import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useUserStore } from '../context/userStore';
import { usePreferencesStore } from '../context/preferencesStore';
import './admin.css';
import { useTranslation } from '../i18n/translations';
import {
  adminUserService,
  type AdminUser,
  type AdminUserComplaints,
  type UserAuditLogEntry,
  type AuditAction,
  type UserComplaint,
  type ComplaintStatus
} from '../api/adminUserService';
import {
  adminAnalyticsService,
  type ActivityAnalytics,
  type AdminAnalyticsSummary,
  type AnalyticsPeriod,
  type PopularityAnalytics,
  type RecommendationAnalytics
} from '../api/adminAnalyticsService';
import { notifyError, notifySuccess } from '../context/notificationStore';
import adminReviewService, {
  type AdminReview,
  type AdminReviewStats,
  type ReviewStatus
} from '../api/adminReviewService';
import { ResponsiveContainer, LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, BarChart, Bar } from 'recharts';
import { movieService, type ImportMoviesPayload, type MovieImportResponse } from '../api/movieService';

type RoleFilter = 'ALL' | 'USER' | 'ADMIN';
type BlockedFilter = 'ALL' | 'BLOCKED' | 'ACTIVE';
type ActionType = 'block' | 'role' | 'delete' | null;
type ReviewStatusFilter = 'ALL' | ReviewStatus;
type PanelKey = 'import' | 'analytics' | 'users' | 'reviews';
const COMPLAINT_PREVIEW_LIMIT = 3;
const COMPLAINTS_PAGE_SIZE = 10;
const AUDIT_PAGE_SIZE = 10;
const LANGUAGE_OPTIONS = [
  { value: 'en-US', label: 'English (US)' },
  { value: 'ru-RU', label: 'Русский (RU)' },
  { value: 'es-ES', label: 'Español (ES)' },
  { value: 'fr-FR', label: 'Français (FR)' },
  { value: 'de-DE', label: 'Deutsch (DE)' }
];
const ORIGINAL_LANGUAGE_OPTIONS = [
  { value: 'en', label: 'English' },
  { value: 'ru', label: 'Русский' },
  { value: 'es', label: 'Español' },
  { value: 'fr', label: 'Français' },
  { value: 'de', label: 'Deutsch' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' }
];
const COUNTRY_OPTIONS = [
  { value: 'US', label: 'United States' },
  { value: 'GB', label: 'United Kingdom' },
  { value: 'CA', label: 'Canada' },
  { value: 'DE', label: 'Germany' },
  { value: 'FR', label: 'France' },
  { value: 'JP', label: 'Japan' },
  { value: 'KR', label: 'South Korea' },
  { value: 'IN', label: 'India' }
];
const TMDB_GENRES = [
  { id: 28, ru: 'Боевик', en: 'Action' },
  { id: 12, ru: 'Приключения', en: 'Adventure' },
  { id: 16, ru: 'Анимация', en: 'Animation' },
  { id: 35, ru: 'Комедия', en: 'Comedy' },
  { id: 80, ru: 'Криминал', en: 'Crime' },
  { id: 99, ru: 'Документальный', en: 'Documentary' },
  { id: 18, ru: 'Драма', en: 'Drama' },
  { id: 10751, ru: 'Семейный', en: 'Family' },
  { id: 14, ru: 'Фэнтези', en: 'Fantasy' },
  { id: 36, ru: 'История', en: 'History' },
  { id: 27, ru: 'Ужасы', en: 'Horror' },
  { id: 10402, ru: 'Музыка', en: 'Music' },
  { id: 9648, ru: 'Детектив', en: 'Mystery' },
  { id: 10749, ru: 'Мелодрама', en: 'Romance' },
  { id: 878, ru: 'Фантастика', en: 'Sci-Fi' },
  { id: 10770, ru: 'ТВ-фильм', en: 'TV Movie' },
  { id: 53, ru: 'Триллер', en: 'Thriller' },
  { id: 10752, ru: 'Военный', en: 'War' },
  { id: 37, ru: 'Вестерн', en: 'Western' }
] as const;

const AdminPage = () => {
  const user = useUserStore((state) => state.user);
  const { t } = useTranslation();
  const navigate = useNavigate();
  const appLanguage = usePreferencesStore((state) => state.language);
  const [panels, setPanels] = useState<Record<PanelKey, boolean>>({
    import: false,
    analytics: true,
    users: true,
    reviews: false
  });
  const importSectionRef = useRef<HTMLDivElement | null>(null);

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL');
  const [blockedFilter, setBlockedFilter] = useState<BlockedFilter>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const selectedUser = useMemo(
    () => (selectedUserId ? users.find((u) => u.id === selectedUserId) ?? null : null),
    [users, selectedUserId]
  );
  const [moderationNote, setModerationNote] = useState('');
  const [complaintsPanel, setComplaintsPanel] = useState<AdminUserComplaints | null>(null);
  const [complaintsDrawerOpen, setComplaintsDrawerOpen] = useState(false);
  const [auditDrawerOpen, setAuditDrawerOpen] = useState(false);
  const [complaintsPageIndex, setComplaintsPageIndex] = useState(0);
  const [auditPageIndex, setAuditPageIndex] = useState(0);
  const [complaintActionId, setComplaintActionId] = useState<number | null>(null);
  const [auditLog, setAuditLog] = useState<UserAuditLogEntry[]>([]);
  const [complaintsLoading, setComplaintsLoading] = useState(false);
  const [auditLoading, setAuditLoading] = useState(false);
  const [actionInProgress, setActionInProgress] = useState<ActionType>(null);
  const [reviews, setReviews] = useState<AdminReview[]>([]);
  const [reviewStats, setReviewStats] = useState<AdminReviewStats | null>(null);
  const [reviewQuery, setReviewQuery] = useState('');
  const [appliedReviewQuery, setAppliedReviewQuery] = useState('');
  const [reviewStatusFilter, setReviewStatusFilter] = useState<ReviewStatusFilter>('ALL');
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewPageSize] = useState(6);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [reviewError, setReviewError] = useState<string | null>(null);
  const [reviewLoading, setReviewLoading] = useState(true);
  const [reviewActionInProgress, setReviewActionInProgress] = useState<ReviewStatus | null>(null);
  const analyticsPeriods: AnalyticsPeriod[] = ['DAY', 'WEEK', 'MONTH'];
  const [analyticsPeriod, setAnalyticsPeriod] = useState<AnalyticsPeriod>('WEEK');
  const [summaryAnalytics, setSummaryAnalytics] = useState<AdminAnalyticsSummary | null>(null);
  const [popularityAnalytics, setPopularityAnalytics] = useState<PopularityAnalytics | null>(null);
  const [activityAnalytics, setActivityAnalytics] = useState<ActivityAnalytics | null>(null);
  const [recommendationAnalytics, setRecommendationAnalytics] = useState<RecommendationAnalytics | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(true);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);
  const [importForm, setImportForm] = useState<ImportMoviesPayload>(() => ({
    pages: 5,
    language: 'en-US',
    originalLanguage: 'en',
    originCountry: 'US',
    yearFrom: 1990,
    yearTo: 2026,
    includeAdult: false,
    minVoteAverage: 6.5,
    minVoteCount: 200,
    genreIds: []
  }));
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<MovieImportResponse | null>(null);
  const [importError, setImportError] = useState<string | null>(null);

  const togglePanel = (key: PanelKey) => {
    setPanels((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const focusImportPanel = () => {
    setPanels((prev) => ({ ...prev, import: true }));
    importSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleComplaintResolve = (complaintId: number) => {
    if (!selectedUser) return;
    setComplaintActionId(complaintId);
    adminUserService
      .updateComplaintStatus(selectedUser.id, complaintId, 'RESOLVED')
      .then((updated) => {
        updateComplaintInState(updated);
        notifySuccess(t('adminComplaintsResolveSuccess'));
      })
      .catch(() => notifyError(t('adminComplaintsResolveError')))
      .finally(() => setComplaintActionId(null));
  };

  const openComplaintsDrawer = () => {
    setComplaintsDrawerOpen(true);
    setComplaintsPageIndex(0);
  };

  const closeComplaintsDrawer = () => setComplaintsDrawerOpen(false);

  const changeComplaintsPage = (delta: number) => {
    setComplaintsPageIndex((prev) => {
      const next = prev + delta;
      if (next < 0) return 0;
      if (next >= complaintsPageCount) return complaintsPageCount - 1;
      return next;
    });
  };

  const openAuditDrawer = () => {
    setAuditDrawerOpen(true);
    setAuditPageIndex(0);
  };

  const closeAuditDrawer = () => setAuditDrawerOpen(false);

  const changeAuditPage = (delta: number) => {
    setAuditPageIndex((prev) => {
      const next = prev + delta;
      if (next < 0) return 0;
      if (next >= auditPageCount) return auditPageCount - 1;
      return next;
    });
  };

  const loadUsers = useCallback(() => {
    setLoading(true);
    setError(null);
    let active = true;
    adminUserService
      .listUsers({
        query: appliedQuery || undefined,
        role: roleFilter === 'ALL' ? undefined : roleFilter,
        blocked: blockedFilter === 'ALL' ? undefined : blockedFilter,
        page,
        size
      })
      .then((data) => {
        if (!active) return;
        setUsers(data.items);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        if (!data.items.length) {
          setSelectedUserId(null);
          return;
        }
        if (!selectedUserId) {
          setSelectedUserId(data.items[0].id);
          return;
        }
        const existsOnPage = data.items.some((item) => item.id === selectedUserId);
        if (!existsOnPage) {
          setSelectedUserId(data.items[0].id);
        }
      })
      .catch((err) => {
        if (!active) return;
        setError(err?.response?.data?.message || 'Не удалось загрузить пользователей');
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [appliedQuery, roleFilter, blockedFilter, page, size, selectedUserId]);

  const loadReviews = useCallback(() => {
    setReviewLoading(true);
    setReviewError(null);
    let active = true;
    adminReviewService
      .listReviews({
        query: appliedReviewQuery || undefined,
        status: reviewStatusFilter === 'ALL' ? undefined : reviewStatusFilter,
        page: reviewPage,
        size: reviewPageSize,
        sort: 'createdAt,desc'
      })
      .then((data) => {
        if (!active) return;
        const visibleReviews = data.items.filter((item) => item.status !== 'DELETED' && item.status !== 'SPAM');
        setReviews(visibleReviews);
        setReviewStats(data.stats);
        setReviewTotalPages(data.totalPages);
      })
      .catch((err) => {
        if (!active) return;
        setReviewError(err?.response?.data?.message || 'Не удалось загрузить отзывы');
      })
      .finally(() => {
        if (active) {
          setReviewLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [appliedReviewQuery, reviewStatusFilter, reviewPage, reviewPageSize]);

  const loadAnalytics = useCallback(() => {
    let active = true;
    setAnalyticsLoading(true);
    setAnalyticsError(null);
    Promise.all([
      adminAnalyticsService.summary(analyticsPeriod),
      adminAnalyticsService.popularity(analyticsPeriod, 5),
      adminAnalyticsService.activity(analyticsPeriod),
      adminAnalyticsService.recommendations(analyticsPeriod)
    ])
      .then(([summary, popularity, activity, recommendations]) => {
        if (!active) return;
        setSummaryAnalytics(summary);
        setPopularityAnalytics(popularity);
        setActivityAnalytics(activity);
        setRecommendationAnalytics(recommendations);
      })
      .catch((err) => {
        if (!active) return;
        setAnalyticsError(err?.response?.data?.message || t('adminAnalyticsError'));
      })
      .finally(() => {
        if (active) {
          setAnalyticsLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [analyticsPeriod, t]);

  useEffect(() => {
    const dispose = loadUsers();
    return () => {
      dispose?.();
    };
  }, [loadUsers]);

  useEffect(() => {
    const dispose = loadReviews();
    return () => {
      dispose?.();
    };
  }, [loadReviews]);

  useEffect(() => {
    const dispose = loadAnalytics();
    return () => {
      dispose?.();
    };
  }, [loadAnalytics]);

  useEffect(() => {
    setModerationNote('');
    setComplaintsDrawerOpen(false);
    setAuditDrawerOpen(false);
    setComplaintsPageIndex(0);
    setAuditPageIndex(0);
  }, [selectedUser?.id]);

  const complaintStatsFromList = useCallback((items: UserComplaint[]) => {
    const stats: Record<ComplaintStatus, number> = {
      PENDING: 0,
      REVIEWING: 0,
      RESOLVED: 0
    };
    items.forEach((item) => {
      stats[item.status] = (stats[item.status] ?? 0) + 1;
    });
    return stats;
  }, []);

  const updateComplaintInState = useCallback(
    (updated: UserComplaint) => {
      setComplaintsPanel((prev) => {
        if (!prev) return prev;
        const complaints = prev.complaints.map((item) => (item.id === updated.id ? updated : item));
        const stats = complaintStatsFromList(complaints);
        const activeCount = stats.PENDING + stats.REVIEWING;
        setUsers((usersList) =>
          usersList.map((userItem) =>
            userItem.id === selectedUserId ? { ...userItem, complaintsCount: activeCount } : userItem
          )
        );
        return {
          ...prev,
          complaints,
          openCount: stats.PENDING,
          reviewingCount: stats.REVIEWING,
          resolvedCount: stats.RESOLVED
        };
      });
    },
    [complaintStatsFromList, selectedUserId]
  );

  const formatComplaintStatus = useCallback(
    (status: ComplaintStatus) => (status === 'RESOLVED' ? t('adminComplaintsStatusResolved') : t('adminComplaintsStatusReviewing')),
    [t]
  );

  const complaintBadgeClass = (status: ComplaintStatus) => (status === 'RESOLVED' ? 'admin-badge--success' : 'admin-badge--user');
  const pendingComplaints = (complaintsPanel?.openCount ?? 0) + (complaintsPanel?.reviewingCount ?? 0);

  useEffect(() => {
    setComplaintsPageIndex((prev) => {
      const total = complaintsPanel?.complaints.length ?? 0;
      const maxIndex = Math.max(Math.ceil(total / COMPLAINTS_PAGE_SIZE) - 1, 0);
      return Math.min(prev, maxIndex);
    });
  }, [complaintsPanel?.complaints.length]);

  useEffect(() => {
    setAuditPageIndex((prev) => {
      const maxIndex = Math.max(Math.ceil(auditLog.length / AUDIT_PAGE_SIZE) - 1, 0);
      return Math.min(prev, maxIndex);
    });
  }, [auditLog.length]);

  useEffect(() => {
    if (!selectedUser) {
      setComplaintsPanel(null);
      setAuditLog([]);
      return;
    }
    let active = true;
    setComplaintsLoading(true);
    setAuditLoading(true);
    adminUserService
      .getComplaints(selectedUser.id)
      .then((data) => {
        if (active) {
          setComplaintsPanel(data);
        }
      })
      .catch((err) => active && notifyError(err?.response?.data?.message || 'Не удалось загрузить жалобы'))
      .finally(() => {
        if (active) {
          setComplaintsLoading(false);
        }
      });

    adminUserService
      .getAuditLog(selectedUser.id)
      .then((data) => {
        if (active) {
          setAuditLog(data);
        }
      })
      .catch((err) => active && notifyError(err?.response?.data?.message || 'Не удалось загрузить аудит-лог'))
      .finally(() => {
        if (active) {
          setAuditLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [selectedUser?.id]);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  const periodLabels = useMemo(
    () =>
      ({
        DAY: t('adminAnalyticsPeriodDAY'),
        WEEK: t('adminAnalyticsPeriodWEEK'),
        MONTH: t('adminAnalyticsPeriodMONTH')
      }) satisfies Record<AnalyticsPeriod, string>,
    [t]
  );

  const topMoviesData = useMemo(
    () =>
      (popularityAnalytics?.topMovies || []).map((movie) => ({
        name: movie.title,
        events: movie.events,
        share: Math.round(movie.share * 100)
      })),
    [popularityAnalytics]
  );

  const complaintPreview = useMemo(
    () => (complaintsPanel?.complaints || []).slice(0, COMPLAINT_PREVIEW_LIMIT),
    [complaintsPanel]
  );
  const hasMoreComplaints = (complaintsPanel?.complaints.length ?? 0) > COMPLAINT_PREVIEW_LIMIT;
  const paginatedComplaints = useMemo(() => {
    if (!complaintsPanel) return [];
    const start = complaintsPageIndex * COMPLAINTS_PAGE_SIZE;
    return complaintsPanel.complaints.slice(start, start + COMPLAINTS_PAGE_SIZE);
  }, [complaintsPanel, complaintsPageIndex]);
  const complaintsPageCount = Math.max(1, Math.ceil((complaintsPanel?.complaints.length ?? 0) / COMPLAINTS_PAGE_SIZE));

  const auditPreview = useMemo(() => auditLog.slice(0, COMPLAINT_PREVIEW_LIMIT), [auditLog]);
  const paginatedAudit = useMemo(() => {
    const start = auditPageIndex * AUDIT_PAGE_SIZE;
    return auditLog.slice(start, start + AUDIT_PAGE_SIZE);
  }, [auditLog, auditPageIndex]);
  const auditPageCount = Math.max(1, Math.ceil(auditLog.length / AUDIT_PAGE_SIZE));

  const heroStats = useMemo(() => {
    const blocked = users.filter((u) => u.blocked).length;
    const admins = users.filter((u) => u.role === 'ADMIN').length;
    const complaints = users.reduce((sum, item) => sum + item.complaintsCount, 0);
    return [
      { label: 'Всего', value: totalElements },
      { label: 'Активных жалоб', value: complaints },
      { label: 'Заблокировано', value: blocked },
      { label: 'Администраторы', value: admins }
    ];
  }, [users, totalElements]);

  const recommendationTrend = recommendationAnalytics?.trend || [];
  const summaryTrend = summaryAnalytics?.trend || [];
  const hourlyDistribution = activityAnalytics?.hourlyDistribution || [];
  const activitySegments = activityAnalytics?.segments || [];

  const handleSearch = (event: FormEvent) => {
    event.preventDefault();
    setPage(0);
    setAppliedQuery(query.trim());
  };

  const handleRoleChange = (value: RoleFilter) => {
    setRoleFilter(value);
    setPage(0);
  };

  const handleBlockedChange = (value: BlockedFilter) => {
    setBlockedFilter(value);
    setPage(0);
  };

  const handleReviewSearch = (event: FormEvent) => {
    event.preventDefault();
    setReviewPage(0);
    setAppliedReviewQuery(reviewQuery.trim());
  };

  const handleReviewStatusChange = (value: ReviewStatusFilter) => {
    setReviewStatusFilter(value);
    setReviewPage(0);
  };

  const goToPage = (next: number) => {
    if (next < 0 || next >= totalPages) return;
    setPage(next);
  };

  const goToReviewPage = (next: number) => {
    if (next < 0 || next >= reviewTotalPages) return;
    setReviewPage(next);
  };

  const updateImportField = <K extends keyof ImportMoviesPayload>(key: K, value: ImportMoviesPayload[K]) => {
    setImportForm((prev) => ({ ...prev, [key]: value }));
  };

  const toggleImportGenre = (genreId: number) => {
    setImportForm((prev) => {
      const next = new Set(prev.genreIds ?? []);
      if (next.has(genreId)) {
        next.delete(genreId);
      } else {
        next.add(genreId);
      }
      return { ...prev, genreIds: Array.from(next) };
    });
  };

  const runImport = (event?: FormEvent) => {
    event?.preventDefault();
    setImporting(true);
    setImportError(null);
    setImportResult(null);
    movieService
      .importFromTmdb(importForm)
      .then((result) => {
        setImportResult(result);
        notifySuccess(t('adminImportSuccess'));
      })
      .catch((err) => {
        const message = err?.response?.data?.message || t('adminImportError');
        setImportError(message);
        notifyError(message);
      })
      .finally(() => setImporting(false));
  };

  const formatImportResult = (result: MovieImportResponse) => {
    const template = t('adminImportResult');
    return template
      .replace('{imported}', String(result.importedMovies))
      .replace('{updated}', String(result.updatedMovies))
      .replace('{skipped}', String(result.skippedMovies))
      .replace('{processed}', String(result.processedPages))
      .replace('{requested}', String(result.requestedPages));
  };

  const handleActionError = (err: any, fallback: string) => {
    const message = err?.response?.data?.message || fallback;
    notifyError(message);
  };

  const moderationReason = moderationNote.trim() || undefined;

  const toggleBlock = (target: AdminUser) => {
    setActionInProgress('block');
    const nextBlocked = !target.blocked;
    adminUserService
      .updateBlockStatus(target.id, { blocked: nextBlocked, reason: moderationReason })
      .then((updated) => {
        setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
        notifySuccess(nextBlocked ? 'Пользователь заблокирован' : 'Пользователь разблокирован');
      })
      .catch((err) => handleActionError(err, 'Не удалось изменить статус блокировки'))
      .finally(() => setActionInProgress(null));
  };

  const changeRole = (target: AdminUser, role: 'USER' | 'ADMIN') => {
    if (target.role === role) return;
    setActionInProgress('role');
    adminUserService
      .updateRole(target.id, { role, reason: moderationReason })
      .then((updated) => {
        setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
        notifySuccess(role === 'ADMIN' ? 'Назначен администратором' : 'Роль пользователя возвращена');
      })
      .catch((err) => handleActionError(err, 'Не удалось сменить роль'))
      .finally(() => setActionInProgress(null));
  };

  const handleDelete = (target: AdminUser) => {
    if (!window.confirm(`Удалить пользователя ${target.name}?`)) {
      return;
    }
    setActionInProgress('delete');
    adminUserService
      .deleteUser(target.id)
      .then(() => {
        notifySuccess('Пользователь удален');
        setUsers((prev) => prev.filter((u) => u.id !== target.id));
        setSelectedUserId(null);
        loadUsers();
      })
      .catch((err) => handleActionError(err, 'Не удалось удалить пользователя'))
      .finally(() => setActionInProgress(null));
  };

  const reviewActionSuccess = (status: ReviewStatus) => {
    switch (status) {
      case 'DELETED':
        return 'Отзыв удален';
      case 'PUBLISHED':
      default:
        return 'Отзыв опубликован';
    }
  };

  const moderateReview = (target: AdminReview, status: ReviewStatus) => {
    setReviewActionInProgress(status);
    adminReviewService
      .updateStatus(target.id, { status })
      .then((updated) => {
        if (status === 'DELETED') {
          setReviews((prev) => prev.filter((item) => item.id !== target.id));
        } else {
          setReviews((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
        }
        notifySuccess(reviewActionSuccess(status));
        loadReviews();
      })
      .catch((err) => handleActionError(err, 'Не удалось обновить отзыв'))
      .finally(() => setReviewActionInProgress(null));
  };

  const formatAuditAction = (action: AuditAction) => {
    switch (action) {
      case 'BLOCK_UPDATED':
        return 'Изменение блокировки';
      case 'ROLE_UPDATED':
        return 'Смена роли';
      case 'USER_DELETED':
        return 'Удаление пользователя';
      default:
        return action;
    }
  };

  const formatReviewStatus = (status: ReviewStatus) => {
    if (status === 'PUBLISHED') return 'Опубликован';
    return 'На модерации';
  };

  const reviewStatusBadge = (status: ReviewStatus) => {
    if (status === 'PUBLISHED') return 'admin-badge--success';
    return 'admin-badge--user';
  };

  const formatDateTime = (value?: string) => {
    if (!value) return '';
    return new Date(value).toLocaleString();
  };

  return (
    <div className="admin-page" data-testid="admin-page">
      <header className="admin-hero">
        <div>
          <p className="eyebrow">{t('navAdmin')}</p>

          <p>Контролируйте пользователей и безопасность платформы в реальном времени.</p>
        </div>
        <div className="admin-hero__stats">
          {heroStats.map((stat) => (
            <div key={stat.label}>
              <strong>{stat.value}</strong>
              <span>{stat.label}</span>
            </div>
          ))}
        </div>
      </header>

      <section className="admin-quick">
        <button type="button" className="admin-quick__item" onClick={() => navigate('/admin/movies')}>
          <div>
            <p>{t('adminQuickContent')}</p>
            <strong>{t('adminQuickContentAction')}</strong>
          </div>
          <span>→</span>
        </button>
        <button type="button" className="admin-quick__item" onClick={() => navigate('/admin/reco')}>
          <div>
            <p>{t('adminQuickReco')}</p>
            <strong>{t('adminQuickRecoAction')}</strong>
          </div>
          <span>⚙</span>
        </button>
        <button type="button" className="admin-quick__item" onClick={focusImportPanel}>
          <div>
            <p>{t('adminQuickImport')}</p>
            <strong>{t('adminQuickImportAction')}</strong>
          </div>
          <span>↻</span>
        </button>
      </section>

      <section ref={importSectionRef} className={`admin-panel ${panels.import ? 'is-open' : 'is-collapsed'}`}>
        <button
          type="button"
          className="admin-panel__toggle"
          onClick={() => togglePanel('import')}
          aria-expanded={panels.import}
        >
          <div>
            <p className="eyebrow">{t('adminImportTitle')}</p>
            <strong>{t('adminImportSubtitle')}</strong>
          </div>
          <span>{panels.import ? t('adminPanelCollapse') : t('adminPanelExpand')}</span>
        </button>
        <div className="admin-panel__body">
          <div className="admin-section admin-section--import">
            <form className="admin-import" onSubmit={runImport}>
              <label>
                {t('importPages')}
                <input
                  type="number"
                  min={1}
                  max={20}
                  value={importForm.pages}
                  onChange={(event) => updateImportField('pages', Math.max(1, Number(event.target.value) || 1))}
                />
              </label>
              <label>
                {t('importLanguage')}
                <select value={importForm.language ?? ''} onChange={(event) => updateImportField('language', event.target.value)}>
                  {LANGUAGE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                {t('importOriginalLanguage')}
                <select
                  value={importForm.originalLanguage ?? ''}
                  onChange={(event) => updateImportField('originalLanguage', event.target.value || undefined)}
                >
                  <option value="">{t('importAny')}</option>
                  {ORIGINAL_LANGUAGE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                {t('importOriginCountry')}
                <select
                  value={importForm.originCountry ?? ''}
                  onChange={(event) => updateImportField('originCountry', event.target.value || undefined)}
                >
                  <option value="">{t('importAny')}</option>
                  {COUNTRY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                {t('importYearFrom')}
                <input
                  type="number"
                  value={importForm.yearFrom ?? ''}
                  onChange={(event) => updateImportField('yearFrom', event.target.value ? Number(event.target.value) : undefined)}
                />
              </label>
              <label>
                {t('importYearTo')}
                <input
                  type="number"
                  value={importForm.yearTo ?? ''}
                  onChange={(event) => updateImportField('yearTo', event.target.value ? Number(event.target.value) : undefined)}
                />
              </label>
              <label>
                {t('importMinRating')}
                <input
                  type="number"
                  step="0.1"
                  value={importForm.minVoteAverage ?? ''}
                  onChange={(event) =>
                    updateImportField('minVoteAverage', event.target.value ? Number(event.target.value) : undefined)
                  }
                />
              </label>
              <label>
                {t('importMinVotes')}
                <input
                  type="number"
                  value={importForm.minVoteCount ?? ''}
                  onChange={(event) =>
                    updateImportField('minVoteCount', event.target.value ? Number(event.target.value) : undefined)
                  }
                />
              </label>
              <div className="admin-import__genresRow admin-import__genresRow--horizontal">
                {TMDB_GENRES.map((genre) => {
                  const active = (importForm.genreIds ?? []).includes(genre.id);

                  return (
                    <button
                      key={genre.id}
                      type="button"
                      className={`admin-genreChip ${active ? 'is-active' : 'is-inactive'}`}
                      aria-pressed={active}
                      onClick={() => toggleImportGenre(genre.id)}
                    >
                      {appLanguage === 'ru' ? genre.ru : genre.en}
                    </button>
                  );
                })}
                <button
                  type="button"
                  className={`admin-genreChip admin-genreChip--toggle ${importForm.includeAdult ? 'is-active' : 'is-inactive'}`}
                  aria-pressed={importForm.includeAdult}
                  onClick={() => updateImportField('includeAdult', !importForm.includeAdult)}
                >
                  {t('importIncludeAdult')}
                </button>
              </div>
              <button type="submit" disabled={importing}>
                {importing ? t('settingsAdminRunning') : t('adminImportButton')}
              </button>
            </form>
            {importError && <p className="admin-import__status admin-import__status--error">{importError}</p>}
            {importResult && <p className="admin-import__status">{formatImportResult(importResult)}</p>}
          </div>
        </div>
      </section>

      <section className={`admin-panel ${panels.analytics ? 'is-open' : 'is-collapsed'}`}>
        <button
          type="button"
          className="admin-panel__toggle"
          onClick={() => togglePanel('analytics')}
          aria-expanded={panels.analytics}
        >
          <div>
            <p className="eyebrow">{t('adminAnalyticsTitle')}</p>
            <strong>{t('adminAnalyticsDescription')}</strong>
          </div>
          <span>{panels.analytics ? t('adminPanelCollapse') : t('adminPanelExpand')}</span>
        </button>
        <div className="admin-panel__body">
        <div className="admin-section admin-section--analytics">
        <div className="admin-section__header admin-section__header--analytics">
          <label className="admin-analytics__period">
            {t('adminAnalyticsPeriodLabel')}
            <select value={analyticsPeriod} onChange={(event) => setAnalyticsPeriod(event.target.value as AnalyticsPeriod)}>
              {analyticsPeriods.map((periodOption) => (
                <option key={periodOption} value={periodOption}>
                  {periodLabels[periodOption]}
                </option>
              ))}
            </select>
          </label>
        </div>
        {analyticsError && <div className="admin-alert admin-alert--error">{analyticsError}</div>}
        {analyticsLoading && <p>{t('catalogLoading')}</p>}
        {!analyticsLoading && summaryAnalytics && activityAnalytics && popularityAnalytics && recommendationAnalytics && (
          <>
            <div className="admin-analytics">
              <div className="admin-analytics__card">
                <p>{t('adminAnalyticsKpiEvents')}</p>
                <strong>{summaryAnalytics.totalEvents}</strong>
              </div>
              <div className="admin-analytics__card">
                <p>{t('adminAnalyticsKpiUsers')}</p>
                <strong>{summaryAnalytics.activeUsers}</strong>
              </div>
              <div className="admin-analytics__card">
                <p>{t('adminAnalyticsKpiAverage')}</p>
                <strong>{summaryAnalytics.avgEventsPerUser.toFixed(1)}</strong>
              </div>
              <div className="admin-analytics__card">
                <p>{t('adminAnalyticsKpiConversion')}</p>
                <strong>{(summaryAnalytics.recommendationConversion * 100).toFixed(0)}%</strong>
              </div>
            </div>
            <div className="admin-analytics__charts">
              <div className="admin-card admin-card--chart">
                <h3 className="admin-chartTitle">{t('adminAnalyticsTrendTitle')}</h3>
                {summaryTrend.length ? (
                  <ResponsiveContainer width="100%" height={220}>
                    <LineChart data={summaryTrend}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                      <XAxis dataKey="day" stroke="rgba(255,255,255,0.6)" />
                      <YAxis stroke="rgba(255,255,255,0.6)" />
                      <Tooltip />
                      <Line type="monotone" dataKey="events" stroke="#20bf63" strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <p className="admin-empty">{t('adminAnalyticsEmpty')}</p>
                )}
              </div>
              <div className="admin-card admin-card--chart">
                <h3 className="admin-chartTitle">{t('adminAnalyticsTopMovies')}</h3>
                {topMoviesData.length ? (
                  <>
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart data={topMoviesData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                        <XAxis dataKey="name" stroke="rgba(255,255,255,0.6)" />
                        <YAxis stroke="rgba(255,255,255,0.6)" />
                        <Tooltip />
                        <Bar dataKey="events" fill="#20bf63" radius={[6, 6, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                    <div className="admin-analytics__meta">
                      {topMoviesData.slice(0, 3).map((movie) => (
                        <span key={movie.name}>
                          {movie.name}: {movie.share}%
                        </span>
                      ))}
                    </div>
                  </>
                ) : (
                  <p className="admin-empty">{t('adminAnalyticsEmpty')}</p>
                )}
              </div>
              <div className="admin-card admin-card--chart">
                <h3 className="admin-chartTitle">{t('adminAnalyticsRecTrend')}</h3>
                {recommendationTrend.length ? (
                  <ResponsiveContainer width="100%" height={220}>
                    <LineChart data={recommendationTrend}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                      <XAxis dataKey="day" stroke="rgba(255,255,255,0.6)" />
                      <YAxis stroke="rgba(255,255,255,0.6)" />
                      <Tooltip />
                      <Line type="monotone" dataKey="views" name={t('adminAnalyticsMetricViews')} stroke="#20bf63" strokeWidth={2} dot={false} />
                      <Line type="monotone" dataKey="starts" name={t('adminAnalyticsMetricStarts')} stroke="#f7d778" strokeWidth={2} dot={false} />
                      <Line type="monotone" dataKey="finishes" name={t('adminAnalyticsMetricFinishes')} stroke="#f45b69" strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <p className="admin-empty">{t('adminAnalyticsEmpty')}</p>
                )}
              </div>
              <div className="admin-card admin-card--chart">
                <h3 className="admin-chartTitle">{t('adminAnalyticsHourly')}</h3>
                {hourlyDistribution.length ? (
                  <>
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart data={hourlyDistribution}>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                        <XAxis dataKey="hour" stroke="rgba(255,255,255,0.6)" />
                        <YAxis stroke="rgba(255,255,255,0.6)" />
                        <Tooltip />
                        <Bar dataKey="events" fill="#8f7ef3" radius={[6, 6, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                    <div className="admin-analytics__meta admin-analytics__meta--segments">
                      {activitySegments.map((segment) => (
                        <span key={segment.segment}>
                          {t(`adminAnalyticsSegment${segment.segment}` as const)}: {segment.users}
                        </span>
                      ))}
                    </div>
                  </>
                ) : (
                  <p className="admin-empty">{t('adminAnalyticsEmpty')}</p>
                )}
              </div>
            </div>
          </>
        )}
          </div>
        </div>
      </section>

      <section className={`admin-panel ${panels.users ? 'is-open' : 'is-collapsed'}`}>
        <button type="button" className="admin-panel__toggle" onClick={() => togglePanel('users')} aria-expanded={panels.users}>
          <div>
            <p className="eyebrow">Модерация</p>
            <strong>Управление пользователями</strong>
          </div>
          <span>{panels.users ? t('adminPanelCollapse') : t('adminPanelExpand')}</span>
        </button>
        <div className="admin-panel__body">
          <div className="admin-section admin-section--users">
        <div className="admin-section__header">
          <div>
            <h2>Управление пользователями</h2>
            <p>Живые данные по ролям, статусу блокировки и жалобам.</p>
          </div>
          <span className="admin-section__meta">Записей: {totalElements}</span>
        </div>

        <form className="admin-filters" onSubmit={handleSearch}>
          <label>
            Поиск
            <input
              name="query"
              placeholder="Имя или email"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
          <label>
            Роль
            <select value={roleFilter} onChange={(event) => handleRoleChange(event.target.value as RoleFilter)}>
              <option value="ALL">Все</option>
              <option value="USER">Пользователи</option>
              <option value="ADMIN">Администраторы</option>
            </select>
          </label>
          <label>
            Статус
            <select value={blockedFilter} onChange={(event) => handleBlockedChange(event.target.value as BlockedFilter)}>
              <option value="ALL">Все</option>
              <option value="ACTIVE">Активные</option>
              <option value="BLOCKED">Заблокированные</option>
            </select>
          </label>
          <button type="submit" className="admin-filterSubmit">
            Найти
          </button>
        </form>

        {error && <div className="admin-alert admin-alert--error">{error}</div>}

        <div className="admin-table admin-table--users">
          <div className="admin-table__head">
            <span>Пользователь</span>
            <span>Роль</span>
            <span>Статус</span>
            <span>Жалобы</span>
            <span>Действия</span>
          </div>
          {users.map((u) => (
            <div
              key={u.id}
              className={`admin-table__row ${selectedUserId === u.id ? 'admin-table__row--selected' : ''}`}
              data-testid="admin-user-row"
              role="button"
              tabIndex={0}
              onClick={() => setSelectedUserId(u.id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  setSelectedUserId(u.id);
                }
              }}
            >
              <div>
                <strong>{u.name}</strong>
                <small>{u.email}</small>
              </div>
              <span className={`admin-badge admin-badge--${u.role.toLowerCase()}`}>{u.role}</span>
              <span className={`admin-badge ${u.blocked ? 'admin-badge--danger' : 'admin-badge--success'}`}>
                {u.blocked ? 'Заблокирован' : 'Активен'}
              </span>
              <span className={u.complaintsCount > 0 ? 'text-warning' : ''}>{u.complaintsCount}</span>
              <div className="admin-table__actions">
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    setSelectedUserId(u.id);
                  }}
                >
                  Детали
                </button>
              </div>
            </div>
          ))}
          {!users.length && !loading && <div className="admin-empty">Нет пользователей по заданным фильтрам.</div>}
        </div>

        <div className="admin-pagination">
          <button type="button" onClick={() => goToPage(page - 1)} disabled={page === 0}>
            Назад
          </button>
          <span>
            Страница {totalPages ? page + 1 : 0} из {totalPages}
          </span>
          <button type="button" onClick={() => goToPage(page + 1)} disabled={page + 1 >= totalPages}>
            Вперёд
          </button>
        </div>
        {loading && (
          <div className="admin-loader">
            <span>Загружаем пользователей…</span>
          </div>
        )}
      </div>
          <div className="admin-section admin-section--details">
        <div className="admin-section__header">
          <div>
            <h2>Модерация пользователя</h2>
            <p>Жалобы, история действий и быстрые операции.</p>
          </div>
        </div>
        {!selectedUser && <div className="admin-empty">Выберите пользователя для просмотра деталей.</div>}
        {selectedUser && (
          <div className="admin-details">
            <div className="admin-details__user">
              <div>
                <strong>{selectedUser.name}</strong>
                <small>{selectedUser.email}</small>
              </div>
              <div className="admin-details__meta">
                <span>Роль: {selectedUser.role}</span>
                <span>Статус: {selectedUser.blocked ? 'Заблокирован' : 'Активен'}</span>
                <span>Жалоб: {selectedUser.complaintsCount}</span>
              </div>
            </div>
            <label className="admin-noteField">
              Комментарий к действию
              <input
                type="text"
                placeholder="Например: повторные нарушения"
                value={moderationNote}
                onChange={(event) => setModerationNote(event.target.value)}
              />
            </label>
            <div className="admin-inlineButtons admin-inlineButtons--actions">
              <button
                type="button"
                onClick={() => changeRole(selectedUser, selectedUser.role === 'ADMIN' ? 'USER' : 'ADMIN')}
                disabled={actionInProgress === 'role'}
              >
                {selectedUser.role === 'ADMIN' ? 'Понизить до USER' : 'Назначить ADMIN'}
              </button>
              <button type="button" onClick={() => toggleBlock(selectedUser)} disabled={actionInProgress === 'block'}>
                {selectedUser.blocked ? 'Разблокировать' : 'Заблокировать'}
              </button>
              <button
                type="button"
                className="danger"
                onClick={() => handleDelete(selectedUser)}
                disabled={actionInProgress === 'delete'}
              >
                Удалить
              </button>
            </div>
            <div className="admin-details__panels">
              <div className="admin-card admin-card--list">
                <div className="admin-card__header">
                  <h3>Жалобы</h3>
                  <div className="admin-complaints__stats">
                    <div>
                      <strong>{pendingComplaints}</strong>
                      <span>{t('adminComplaintsStatusReviewing')}</span>
                    </div>
                    <div>
                      <strong>{complaintsPanel?.resolvedCount ?? 0}</strong>
                      <span>{t('adminComplaintsStatusResolved')}</span>
                    </div>
                  </div>
                </div>
                {complaintsLoading && <div className="admin-loader">Загружаем жалобы…</div>}
                {!complaintsLoading && !complaintsPanel?.complaints.length && (
                  <div className="admin-empty">Жалобы отсутствуют.</div>
                )}
                {!complaintsLoading && complaintsPanel?.complaints.length && (
                  <>
                    <ul className="admin-complaintList">
                      {complaintPreview.map((complaint) => (
                        <li key={complaint.id}>
                          <div className="admin-complaint__header">
                            <div>
                              <strong>{complaint.category}</strong>
                              <small>{complaint.reporterName}</small>
                            </div>
                            <span className={`admin-badge ${complaintBadgeClass(complaint.status)}`}>
                              {formatComplaintStatus(complaint.status)}
                            </span>
                          </div>
                          <p>{complaint.description}</p>
                          <div className="admin-complaint__meta">
                            <small>{formatDateTime(complaint.createdAt)}</small>
                            {complaint.reporterEmail && <small>{complaint.reporterEmail}</small>}
                          </div>
                          {complaint.status !== 'RESOLVED' && (
                            <div className="admin-complaint__actions">
                              <button
                                type="button"
                                onClick={() => handleComplaintResolve(complaint.id)}
                                disabled={complaintActionId === complaint.id}
                              >
                                {t('adminComplaintsMarkResolved')}
                              </button>
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>
                    {hasMoreComplaints && (
                      <button type="button" className="admin-link" onClick={openComplaintsDrawer}>
                        {t('adminComplaintsShowAll')}
                      </button>
                    )}
                  </>
                )}
              </div>
              <div className="admin-card admin-card--list">
                <div className="admin-card__header">
                  <h3>Аудит действий</h3>
                </div>
                {auditLoading && <div className="admin-loader">Загружаем аудит…</div>}
                {!auditLoading && !auditLog.length && <div className="admin-empty">История пуста.</div>}
                {!auditLoading && auditLog.length > 0 && (
                  <>
                    <ul className="admin-complaintList">
                      {auditPreview.map((entry) => (
                        <li key={entry.id}>
                          <div className="admin-complaint__header">
                            <div>
                              <strong>{formatAuditAction(entry.action)}</strong>
                              <small>{entry.performedByEmail ?? entry.performedByName}</small>
                            </div>
                            <small>{formatDateTime(entry.createdAt)}</small>
                          </div>
                          {entry.details && <p>{entry.details}</p>}
                        </li>
                      ))}
                    </ul>
                    {auditLog.length > COMPLAINT_PREVIEW_LIMIT && (
                      <button type="button" className="admin-link" onClick={openAuditDrawer}>
                        {t('adminAuditShowAll')}
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        )}
          </div>
        </div>
      </section>

      <section className={`admin-panel ${panels.reviews ? 'is-open' : 'is-collapsed'}`}>
        <button
          type="button"
          className="admin-panel__toggle"
          onClick={() => togglePanel('reviews')}
          aria-expanded={panels.reviews}
        >
          <div>
            <p className="eyebrow">Контент</p>
            <strong>Отзывы и модерация</strong>
          </div>
          <span>{panels.reviews ? t('adminPanelCollapse') : t('adminPanelExpand')}</span>
        </button>
        <div className="admin-panel__body">
          <div className="admin-section admin-section--reviews">
        <div className="admin-section__header">
          <div>
            <h2>Отзывы и модерация</h2>
            <p>Лента подозрительных отзывов и быстрые действия.</p>
          </div>
          {reviewStats && (
            <div className="admin-complaints__stats">
              <div>
                <strong>{reviewStats.total}</strong>
                <span>Всего</span>
              </div>
              <div>
                <strong>{reviewStats.published}</strong>
                <span>Опубликовано</span>
              </div>
              <div>
                <strong>{reviewStats.pending}</strong>
                <span>На модерации</span>
              </div>
            </div>
          )}
        </div>

        <form className="admin-filters admin-filters--reviews" onSubmit={handleReviewSearch}>
          <label>
            Поиск
            <input
              name="reviewQuery"
              placeholder="Автор, текст или фильм"
              value={reviewQuery}
              onChange={(event) => setReviewQuery(event.target.value)}
            />
          </label>
          <label>
            Статус
            <select value={reviewStatusFilter} onChange={(event) => handleReviewStatusChange(event.target.value as ReviewStatusFilter)}>
              <option value="ALL">Все</option>
              <option value="PENDING">На модерации</option>
              <option value="PUBLISHED">Опубликованные</option>
            </select>
          </label>
          <button type="submit" className="admin-filterSubmit">
            Обновить
          </button>
        </form>

        {reviewError && <div className="admin-alert admin-alert--error">{reviewError}</div>}

        <div className="admin-table admin-table--reviews">
          <div className="admin-table__head">
            <span>Отзыв</span>
            <span>Статус</span>
            <span>Текст</span>
            <span>Действия</span>
          </div>
          {reviews.map((review) => (
            <div className="admin-table__row" key={review.id} data-testid="admin-review-row">
              <div className="admin-review__author">
                <div>
                  <strong>{review.userName}</strong>
                  <small>{review.userEmail}</small>
                  <small>Фильм: {review.movieTitle}</small>
                  <small>Создан: {formatDateTime(review.createdAt)}</small>
                </div>
              </div>
              <div className="admin-review__status">
                <span className={`admin-badge ${reviewStatusBadge(review.status)}`}>
                  {formatReviewStatus(review.status)}
                </span>
                <small>Оценка: {review.score}/10</small>
                {review.moderatedBy && <small>Модератор: {review.moderatedBy}</small>}
              </div>
              <div className="admin-review__text">
                <p>{review.content}</p>
                {review.updatedAt && <small>Обновлено: {formatDateTime(review.updatedAt)}</small>}
              </div>
              <div className="admin-inlineButtons admin-inlineButtons--actions admin-review__actions">
                <button
                  type="button"
                  className="danger"
                  onClick={() => moderateReview(review, 'DELETED')}
                  disabled={!!reviewActionInProgress}
                >
                  Удалить
                </button>
                <button
                  type="button"
                  onClick={() => moderateReview(review, 'PUBLISHED')}
                  disabled={!!reviewActionInProgress}
                >
                  Опубликовать
                </button>
              </div>
            </div>
          ))}
          {!reviews.length && !reviewLoading && <div className="admin-empty">Нет отзывов по заданным фильтрам.</div>}
        </div>

        <div className="admin-pagination">
          <button type="button" onClick={() => goToReviewPage(reviewPage - 1)} disabled={reviewPage === 0}>
            Назад
          </button>
          <span>
            Страница {reviewTotalPages ? reviewPage + 1 : 0} из {reviewTotalPages}
          </span>
          <button
            type="button"
            onClick={() => goToReviewPage(reviewPage + 1)}
            disabled={reviewPage + 1 >= reviewTotalPages}
          >
            Вперёд
          </button>
        </div>
        {reviewLoading && (
          <div className="admin-loader">
            <span>Загружаем отзывы…</span>
          </div>
        )}
          </div>
        </div>
      </section>
      {complaintsDrawerOpen && (
        <div className="admin-drawer" role="dialog" aria-modal="true">
          <div className="admin-drawer__content">
            <div className="admin-drawer__header">
              <h3>{t('adminComplaintsDrawerTitle')}</h3>
              <button type="button" onClick={closeComplaintsDrawer}>
                {t('adminDrawerClose')}
              </button>
            </div>
            <div className="admin-drawer__body">
              {paginatedComplaints.length ? (
                <ul className="admin-complaintList admin-complaintList--expanded">
                  {paginatedComplaints.map((complaint) => (
                    <li key={`drawer-complaint-${complaint.id}`}>
                      <div className="admin-complaint__header">
                        <div>
                          <strong>{complaint.category}</strong>
                          <small>{complaint.reporterName}</small>
                        </div>
                        <span className={`admin-badge ${complaintBadgeClass(complaint.status)}`}>
                          {formatComplaintStatus(complaint.status)}
                        </span>
                      </div>
                      <p>{complaint.description}</p>
                      <div className="admin-complaint__meta">
                        <small>{formatDateTime(complaint.createdAt)}</small>
                        {complaint.reporterEmail && <small>{complaint.reporterEmail}</small>}
                      </div>
                      {complaint.status !== 'RESOLVED' && (
                        <div className="admin-complaint__actions">
                          <button
                            type="button"
                            onClick={() => handleComplaintResolve(complaint.id)}
                            disabled={complaintActionId === complaint.id}
                          >
                            {t('adminComplaintsMarkResolved')}
                          </button>
                        </div>
                      )}
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="admin-empty">Жалобы отсутствуют.</div>
              )}
            </div>
            {complaintsPageCount > 1 && (
              <div className="admin-drawer__pagination">
                <button type="button" onClick={() => changeComplaintsPage(-1)} disabled={complaintsPageIndex === 0}>
                  {t('adminDrawerPrev')}
                </button>
                <span>
                  {complaintsPageIndex + 1}/{complaintsPageCount}
                </span>
                <button
                  type="button"
                  onClick={() => changeComplaintsPage(1)}
                  disabled={complaintsPageIndex + 1 >= complaintsPageCount}
                >
                  {t('adminDrawerNext')}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
      {auditDrawerOpen && (
        <div className="admin-drawer" role="dialog" aria-modal="true">
          <div className="admin-drawer__content">
            <div className="admin-drawer__header">
              <h3>{t('adminAuditDrawerTitle')}</h3>
              <button type="button" onClick={closeAuditDrawer}>
                {t('adminDrawerClose')}
              </button>
            </div>
            <div className="admin-drawer__body">
              {paginatedAudit.length ? (
                <ul className="admin-complaintList admin-complaintList--expanded">
                  {paginatedAudit.map((entry) => (
                    <li key={`drawer-audit-${entry.id}`}>
                      <div className="admin-complaint__header">
                        <div>
                          <strong>{formatAuditAction(entry.action)}</strong>
                          <small>{entry.performedByEmail ?? entry.performedByName}</small>
                        </div>
                        <small>{formatDateTime(entry.createdAt)}</small>
                      </div>
                      {entry.details && <p>{entry.details}</p>}
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="admin-empty">{t('adminAnalyticsEmpty')}</div>
              )}
            </div>
            {auditPageCount > 1 && (
              <div className="admin-drawer__pagination">
                <button type="button" onClick={() => changeAuditPage(-1)} disabled={auditPageIndex === 0}>
                  {t('adminDrawerPrev')}
                </button>
                <span>
                  {auditPageIndex + 1}/{auditPageCount}
                </span>
                <button type="button" onClick={() => changeAuditPage(1)} disabled={auditPageIndex + 1 >= auditPageCount}>
                  {t('adminDrawerNext')}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPage;
