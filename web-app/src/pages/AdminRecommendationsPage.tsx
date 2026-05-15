import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useUserStore } from '../context/userStore';
import { recommendationAdminService, RecommendationConfig, RecommendationConfigPayload, RecommendationRebuildLog, RecommendationRebuildStatus } from '../api/recommendationAdminService';
import { movieService, RecommendationResponse } from '../api/movieService';
import MovieCard from '../components/MovieCard';
import { notifyError, notifyInfo, notifySuccess } from '../context/notificationStore';
import { useTranslation } from '../i18n/translations';
import './admin-reco.css';

const trainingPeriods: RecommendationConfig['trainingPeriod'][] = ['DAY', 'WEEK', 'MONTH'];
const algorithmOptions: RecommendationConfig['defaultAlgorithm'][] = ['POPULARITY', 'CO_OCCURRENCE', 'CONTENT_BASED', 'HYBRID', 'ML_EMBEDDING'];

const AdminRecommendationsPage = () => {
  const user = useUserStore((state) => state.user);
  const { t } = useTranslation();
  const [config, setConfig] = useState<RecommendationConfig | null>(null);
  const [form, setForm] = useState<RecommendationConfigPayload | null>(null);
  const [loadingConfig, setLoadingConfig] = useState(true);
  const [saving, setSaving] = useState(false);
  const [rebuilding, setRebuilding] = useState(false);
  const [recommendations, setRecommendations] = useState<RecommendationResponse | null>(null);
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);
  const [userIdInput, setUserIdInput] = useState('');
  const [limitInput, setLimitInput] = useState('12');

  const loadConfig = useCallback(() => {
    setLoadingConfig(true);
    recommendationAdminService
      .getConfig()
      .then((cfg) => {
        setConfig(cfg);
        setForm({
          enabled: cfg.enabled,
          trainingPeriod: cfg.trainingPeriod,
          defaultAlgorithm: cfg.defaultAlgorithm,
          defaultStrategyId: cfg.defaultStrategyId,
          recommendationLimit: cfg.recommendationLimit,
          rebuildBatchSize: cfg.rebuildBatchSize,
          maxUsersPerJob: cfg.maxUsersPerJob
        });
      })
      .catch(() => notifyError(t('adminRecoLoadError')))
      .finally(() => setLoadingConfig(false));
  }, [t]);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  useEffect(() => {
    if (!config?.activeRebuild || config.activeRebuild.status === 'COMPLETED' || config.activeRebuild.status === 'FAILED') {
      return;
    }
    const handle = window.setInterval(() => {
      recommendationAdminService.getConfig().then((cfg) => setConfig(cfg));
    }, 3000);
    return () => window.clearInterval(handle);
  }, [config?.activeRebuild?.status]);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  const handleField = <K extends keyof RecommendationConfigPayload>(key: K, value: RecommendationConfigPayload[K]) => {
    setForm((prev) => (prev ? { ...prev, [key]: value } : prev));
  };

  const handleSave = (event: FormEvent) => {
    event.preventDefault();
    if (!form) return;
    setSaving(true);
    recommendationAdminService
      .updateConfig(form)
      .then((cfg) => {
        setConfig(cfg);
        notifySuccess(t('adminRecoConfigSaved'));
      })
      .catch(() => notifyError(t('adminRecoSaveError')))
      .finally(() => setSaving(false));
  };

  const handleRebuild = () => {
    if (!config) return;
    setRebuilding(true);
    recommendationAdminService
      .triggerRebuild({
        initiator: user.email
      })
      .then((log) => {
        notifyInfo(t('adminRecoRebuildStarted'));
        setConfig((prev) => (prev ? { ...prev, activeRebuild: log } : prev));
      })
      .catch(() => notifyError(t('adminRecoRebuildError')))
      .finally(() => {
        setRebuilding(false);
        loadConfig();
      });
  };

  const handlePreview = (event: FormEvent) => {
    event.preventDefault();
    const parsedId = Number(userIdInput);
    if (!parsedId || Number.isNaN(parsedId)) {
      notifyError(t('adminRecoUserInvalid'));
      return;
    }
    const parsedLimit = Number(limitInput) || config?.recommendationLimit || 10;
    setLoadingRecommendations(true);
    movieService
      .forUser(parsedId, {
        limit: parsedLimit,
        period: config?.trainingPeriod,
        algo: config?.defaultAlgorithm
      })
      .then((response) => {
        setRecommendations(response);
        notifySuccess(t('adminRecoPreviewReady'));
      })
      .catch(() => {
        setRecommendations(null);
        notifyError(t('adminRecoPreviewError'));
      })
      .finally(() => setLoadingRecommendations(false));
  };

  const activeLog = config?.activeRebuild;
  const progressPercent = useMemo(() => {
    if (!activeLog || !activeLog.totalUsers || activeLog.totalUsers === 0) {
      return 0;
    }
    return Math.round(((activeLog.processedUsers ?? 0) / activeLog.totalUsers) * 100);
  }, [activeLog]);

  const statusLabels = useMemo(
    () =>
      ({
        SCHEDULED: t('adminRecoStatusScheduled'),
        RUNNING: t('adminRecoStatusRunning'),
        COMPLETED: t('adminRecoStatusCompleted'),
        FAILED: t('adminRecoStatusFailed')
      } satisfies Record<RecommendationRebuildStatus, string>),
    [t]
  );

  return (
    <div className="admin-reco">
      <header className="admin-reco__header">
        <div>
          <p className="eyebrow">{t('adminRecoSubtitle')}</p>
          <h1>{t('adminRecoTitle')}</h1>
        </div>
      </header>

      {loadingConfig && <p>{t('catalogLoading')}</p>}

      {!loadingConfig && form && (
        <>
          <section className="admin-reco__grid">
            <form className="admin-reco__card" onSubmit={handleSave}>
              <h3>{t('adminRecoSettingsCard')}</h3>
              <div className="admin-reco__field">
                <label>{t('adminRecoEnabled')}</label>
                <button type="button" className={`toggle ${form.enabled ? 'is-on' : ''}`} onClick={() => handleField('enabled', !form.enabled)}>
                  {form.enabled ? t('adminRecoEnabledOn') : t('adminRecoEnabledOff')}
                </button>
              </div>
              <label className="admin-reco__field">
                {t('adminRecoTrainingPeriod')}
                <select value={form.trainingPeriod} onChange={(event) => handleField('trainingPeriod', event.target.value as RecommendationConfig['trainingPeriod'])}>
                  {trainingPeriods.map((period) => (
                    <option key={period} value={period}>
                      {t(`adminRecoPeriod${period}` as const)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="admin-reco__field">
                {t('adminRecoAlgorithm')}
                <select value={form.defaultAlgorithm} onChange={(event) => handleField('defaultAlgorithm', event.target.value as RecommendationConfig['defaultAlgorithm'])}>
                  {algorithmOptions.map((algo) => (
                    <option key={algo} value={algo}>
                      {algo}
                    </option>
                  ))}
                </select>
              </label>
              <label className="admin-reco__field">
                {t('adminRecoLimit')}
                <input type="number" min={1} max={100} value={form.recommendationLimit} onChange={(event) => handleField('recommendationLimit', Number(event.target.value))} />
              </label>
              <label className="admin-reco__field">
                {t('adminRecoBatch')}
                <input type="number" min={1} value={form.rebuildBatchSize} onChange={(event) => handleField('rebuildBatchSize', Number(event.target.value))} />
              </label>
              <label className="admin-reco__field">
                {t('adminRecoMaxUsers')}
                <input type="number" min={1} value={form.maxUsersPerJob} onChange={(event) => handleField('maxUsersPerJob', Number(event.target.value))} />
              </label>
              <div className="admin-reco__actions">
                <button type="submit" className="primary" disabled={saving}>
                  {saving ? t('catalogLoading') : t('adminRecoSave')}
                </button>
              </div>
            </form>

            <div className="admin-reco__card">
              <h3>{t('adminRecoRebuildCard')}</h3>
              {activeLog ? (
                <div className="reco-progress">
                  <p>{statusLabels[activeLog.status]}</p>
                  <div className="reco-progress__bar">
                    <span style={{ width: `${progressPercent}%` }} />
                  </div>
                  <p>
                    {activeLog.processedUsers ?? 0}/{activeLog.totalUsers ?? 0} ({progressPercent}%)
                  </p>
                </div>
              ) : (
                <p>{t('adminRecoRebuildIdle')}</p>
              )}
              {config?.lastRebuild && <RebuildSummary log={config.lastRebuild} statusLabels={statusLabels} />}
              <button type="button" className="primary" onClick={handleRebuild} disabled={rebuilding || !!activeLog}>
                {rebuilding ? t('catalogLoading') : t('adminRecoRebuildStart')}
              </button>
            </div>
          </section>

          <section className="admin-reco__card">
            <h3>{t('adminRecoUserPreview')}</h3>
            <form className="admin-reco__filters" onSubmit={handlePreview}>
              <label>
                {t('adminRecoUserId')}
                <input type="number" value={userIdInput} onChange={(event) => setUserIdInput(event.target.value)} placeholder="123" />
              </label>
              <label>
                {t('adminRecoLimit')}
                <input type="number" min={1} max={100} value={limitInput} onChange={(event) => setLimitInput(event.target.value)} />
              </label>
              <button type="submit" className="primary" disabled={loadingRecommendations}>
                {loadingRecommendations ? t('catalogLoading') : t('adminRecoPreview')}
              </button>
            </form>
            {loadingRecommendations && <p>{t('catalogLoading')}</p>}
            {!loadingRecommendations && recommendations?.items?.length ? (
              <div className="admin-reco__movies">
                {recommendations.items.map((item) => item.movie && <MovieCard key={item.movie.id} movie={item.movie} showMeta={false} />)}
              </div>
            ) : (
              !loadingRecommendations && <p>{t('adminRecoPreviewEmpty')}</p>
            )}
          </section>
        </>
      )}
    </div>
  );
};

const RebuildSummary = ({ log, statusLabels }: { log: RecommendationRebuildLog; statusLabels: Record<RecommendationRebuildStatus, string> }) => (
  <div className="reco-summary">
    <p>
      <strong>{statusLabels[log.status]}</strong>
    </p>
    <p>
      {log.totalUsers ?? 0} {log.trainingPeriod && `(${log.trainingPeriod})`}
    </p>
    {log.finishedAt && <p>{new Date(log.finishedAt).toLocaleString()}</p>}
  </div>
);

export default AdminRecommendationsPage;
