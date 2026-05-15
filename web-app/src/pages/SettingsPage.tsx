import { CSSProperties, FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUserStore } from '../context/userStore';
import { usePreferencesStore } from '../context/preferencesStore';
import { AppLanguage } from '../context/preferencesStore';
import { movieService } from '../api/movieService';
import { recommendationPreferenceService, RecommendationPreference } from '../api/recommendationPreferenceService';
import { useTranslation } from '../i18n/translations';
import { notifyError, notifySuccess } from '../context/notificationStore';
import './settings.css';

type SettingsSwitchProps = {
  label: string;
  checked: boolean;
  onToggle: (checked: boolean) => void;
  disabled?: boolean;
};

const SettingsSwitch = ({ label, checked, onToggle, disabled }: SettingsSwitchProps) => (
  <div className="settings-toggle">
    <span className="settings-toggle__label">{label}</span>
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      className={`settings-switch ${checked ? 'is-active' : ''}`}
      onClick={() => {
        if (disabled) return;
        onToggle(!checked);
      }}
      disabled={disabled}
    >
      <span className="settings-switch__thumb" />
    </button>
  </div>
);

const MAX_TASTE_GENRES = 5;

const SettingsPage = () => {
  const user = useUserStore((state) => state.user);
  const userId = user?.id;
  const setUser = useUserStore((state) => state.setUser);
  const logout = useUserStore((state) => state.logout);
  const navigate = useNavigate();
  const { t } = useTranslation();

  const buildDefaultPreference = useCallback(
    (id?: number): RecommendationPreference => ({
      userId: id ?? 0,
      boostGenres: [],
      muteGenres: [],
      freshnessBias: 0.5,
      discoveryBias: 0.5
    }),
    []
  );

  const [name, setName] = useState(user?.name ?? 'Киноман');
  const email = useMemo(() => user?.email ?? '—', [user?.email]);

  const avatar = usePreferencesStore((state) => state.avatar);
  const setAvatar = usePreferencesStore((state) => state.setAvatar);
  const language = usePreferencesStore((state) => state.language);
  const setLanguage = usePreferencesStore((state) => state.setLanguage);
  const theme = usePreferencesStore((state) => state.theme);
  const setTheme = usePreferencesStore((state) => state.setTheme);
  const parentalControl = usePreferencesStore((state) => state.parentalControl);
  const setParentalControl = usePreferencesStore((state) => state.setParentalControl);
  const [profilePrivate, setProfilePrivate] = useState(false);
  const [privacyLoading, setPrivacyLoading] = useState(false);
  const [tastePreference, setTastePreference] = useState<RecommendationPreference>(buildDefaultPreference(userId));
  const [tasteLoading, setTasteLoading] = useState(false);
  const [tasteSaving, setTasteSaving] = useState(false);
  const [availableGenres, setAvailableGenres] = useState<string[]>([]);
  const [genresLoading, setGenresLoading] = useState(false);
  const [preferenceOpen, setPreferenceOpen] = useState(false);
  const preferenceRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!userId) {
      setProfilePrivate(false);
      setTastePreference(buildDefaultPreference());
      return;
    }
    let active = true;
    setPrivacyLoading(true);
    movieService
      .getProfile(userId)
      .then((profile) => {
        if (!active) return;
        setProfilePrivate(profile.profilePrivate ?? false);
      })
      .catch(() => {
        if (!active) return;
        setProfilePrivate(false);
      })
      .finally(() => active && setPrivacyLoading(false));
    return () => {
      active = false;
    };
  }, [userId, buildDefaultPreference]);

  useEffect(() => {
    let active = true;
    setGenresLoading(true);
    movieService
      .filters()
      .then((filters) => {
        if (!active) return;
        setAvailableGenres(filters?.genres ?? []);
      })
      .catch(() => {
        if (!active) return;
        setAvailableGenres([]);
      })
      .finally(() => active && setGenresLoading(false));
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!userId) {
      setTastePreference(buildDefaultPreference());
      return;
    }
    let active = true;
    setTasteLoading(true);
    recommendationPreferenceService
      .get(userId)
      .then((preferences) => {
        if (!active) return;
        setTastePreference(preferences);
      })
      .catch(() => {
        if (!active) return;
        setTastePreference(buildDefaultPreference(userId));
      })
      .finally(() => active && setTasteLoading(false));
    return () => {
      active = false;
    };
  }, [userId, buildDefaultPreference]);

  useEffect(() => {
    setPreferenceOpen(false);
  }, [userId]);

  useEffect(() => {
    if (preferenceOpen && preferenceRef.current) {
      window.requestAnimationFrame(() => {
        preferenceRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    }
  }, [preferenceOpen]);

  const sliderStyle = (value: number): CSSProperties => {
    const clamped = Math.max(0, Math.min(1, value));
    return {
      '--slider-progress': `${(clamped * 100).toFixed(2)}%`
    } as CSSProperties;
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (user) {
      setUser({ ...user, name });
    }
    if (userId) {
      movieService.updateProfilePrivacy(userId, profilePrivate).catch(() => {});
    }
    alert(t('settingsSaved'));
    navigate('/profile');
  };

  const exit = () => {
    logout();
    navigate('/login');
  };

  const handleGenreToggle = (bucket: 'boost' | 'mute', genre: string) => {
    if (!genre) return;
    setTastePreference((prev) => {
      const targetKey = bucket === 'boost' ? 'boostGenres' : 'muteGenres';
      const otherKey = bucket === 'boost' ? 'muteGenres' : 'boostGenres';
      const isSelected = prev[targetKey].includes(genre);
      if (!isSelected && prev[targetKey].length >= MAX_TASTE_GENRES) {
        notifyError(t('settingsPreferenceLimit'));
        return prev;
      }
      const nextTarget = isSelected ? prev[targetKey].filter((item) => item !== genre) : [...prev[targetKey], genre];
      const nextOther = prev[otherKey].filter((item) => item !== genre);
      return {
        ...prev,
        [targetKey]: nextTarget,
        [otherKey]: nextOther
      };
    });
  };

  const handleBiasChange = (key: 'freshnessBias' | 'discoveryBias', value: number) => {
    const clamped = Math.max(0, Math.min(1, value));
    setTastePreference((prev) => ({
      ...prev,
      [key]: clamped
    }));
  };

  const handlePreferenceSave = () => {
    if (!userId) return;
    setTasteSaving(true);
    recommendationPreferenceService
      .update(userId, {
        boostGenres: tastePreference.boostGenres,
        muteGenres: tastePreference.muteGenres,
        freshnessBias: Number(tastePreference.freshnessBias.toFixed(2)),
        discoveryBias: Number(tastePreference.discoveryBias.toFixed(2))
      })
      .then((updated) => {
        setTastePreference(updated);
        notifySuccess(t('settingsPreferenceSaved'));
        setPreferenceOpen(false);
        window.requestAnimationFrame(() => {
          window.scrollTo({ top: 0, behavior: 'smooth' });
        });
      })
      .catch(() => notifyError(t('settingsPreferenceError')))
      .finally(() => setTasteSaving(false));
  };

  return (
    <div className="settings-page">
      <h1>{t('settingsTitle')}</h1>
      <form className="settings-card" onSubmit={onSubmit}>
        <label>
          {t('settingsName')}
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          {t('settingsEmail')}
          <input value={email} disabled readOnly />
        </label>
        <label>
          {t('settingsAvatar')}
          <select value={avatar} onChange={(e) => setAvatar(e.target.value)}>
            <option value="🙂">🙂</option>
            <option value="🎬">🎬</option>
            <option value="🦸">🦸</option>
            <option value="🐉">🐉</option>
          </select>
        </label>
        <label>
          {t('settingsLanguage')}
          <select value={language} onChange={(e) => setLanguage(e.target.value as AppLanguage)}>
            <option value="ru">{t('settingsLanguageRu')}</option>
            <option value="en">{t('settingsLanguageEn')}</option>
          </select>
        </label>
        <SettingsSwitch label={t('settingsTheme')} checked={theme === 'light'} onToggle={(checked) => setTheme(checked ? 'light' : 'dark')} />
        <SettingsSwitch label={t('settingsPrivacy')} checked={profilePrivate} onToggle={setProfilePrivate} disabled={privacyLoading} />
        <div className="settings-toggle">
          <div className="settings-toggle__labelGroup">
            <span className="settings-toggle__label">{t('settingsParental')}</span>
            <button type="button" className="settings-helpIcon" aria-label={t('settingsParentalHelp')} title={t('settingsParentalHelp')}>
              ?
            </button>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={parentalControl}
            aria-label={t('settingsParental')}
            className={`settings-switch ${parentalControl ? 'is-active' : ''}`}
            onClick={() => setParentalControl(!parentalControl)}
          >
            <span className="settings-switch__thumb" />
          </button>
        </div>
        {userId && (
          <button
            type="button"
            className={`settings-preferenceToggle ${preferenceOpen ? 'is-active' : ''}`}
            onClick={() => setPreferenceOpen((prev) => !prev)}
          >
            <span>{t('settingsPreferenceTitle')}</span>
            <span className="settings-preferenceToggle__status">
              {preferenceOpen ? t('settingsPreferenceToggleHide') : t('settingsPreferenceToggleShow')}
            </span>
          </button>
        )}

        <div className="settings-actions">
          <button type="submit">{t('settingsSave')}</button>
          <button type="button" className="ghost-button" onClick={exit}>
            {t('settingsLogout')}
          </button>
        </div>
      </form>

      {userId && preferenceOpen && (
        <section ref={preferenceRef} className="settings-card settings-card--wide settings-card--preferences">
          <div className="settings-preferences__header">
            <div>
              <h2>{t('settingsPreferenceTitle')}</h2>
              <p>{t('settingsPreferenceSubtitle')}</p>
            </div>
          </div>

          <div className="settings-preferences__group">
            <div className="settings-preferences__groupHeader">
              <h3>{t('settingsPreferenceBoostTitle')}</h3>
              <p>{t('settingsPreferenceBoostHint')}</p>
            </div>
            {genresLoading && <p className="settings-preferences__hint">{t('settingsPreferenceGenresLoading')}</p>}
            {!genresLoading && (
              <div className="settings-chipGroup">
                {availableGenres.map((genre) => {
                  const isActive = tastePreference.boostGenres.includes(genre);
                  const isMuted = tastePreference.muteGenres.includes(genre);
                  return (
                    <button
                      key={`boost-${genre}`}
                      type="button"
                      className={`settings-chip ${isActive ? 'is-active' : ''} ${isMuted ? 'is-disabled' : ''}`}
                      onClick={() => handleGenreToggle('boost', genre)}
                      disabled={tasteLoading}
                    >
                      {genre}
                    </button>
                  );
                })}
              </div>
            )}
            {!genresLoading && !availableGenres.length && (
              <p className="settings-preferences__hint">{t('settingsPreferenceGenresEmpty')}</p>
            )}
          </div>

          <div className="settings-preferences__group">
            <div className="settings-preferences__groupHeader">
              <h3>{t('settingsPreferenceMuteTitle')}</h3>
              <p>{t('settingsPreferenceMuteHint')}</p>
            </div>
            {genresLoading && <p className="settings-preferences__hint">{t('settingsPreferenceGenresLoading')}</p>}
            {!genresLoading && (
              <div className="settings-chipGroup">
                {availableGenres.map((genre) => {
                  const isActive = tastePreference.muteGenres.includes(genre);
                  const isBoosted = tastePreference.boostGenres.includes(genre);
                  return (
                    <button
                      key={`mute-${genre}`}
                      type="button"
                      className={`settings-chip ${isActive ? 'is-active' : ''} ${isBoosted ? 'is-disabled' : ''}`}
                      onClick={() => handleGenreToggle('mute', genre)}
                      disabled={tasteLoading}
                    >
                      {genre}
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <div className="settings-preferences__sliders">
            <div className="settings-slider">
              <div className="settings-slider__header">
                <div>
                  <strong>{t('settingsPreferenceFreshness')}</strong>
                  <p>{t('settingsPreferenceFreshnessHint')}</p>
                </div>
                <span>{Math.round(tastePreference.freshnessBias * 100)}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="1"
                step="0.1"
                value={tastePreference.freshnessBias}
                onChange={(event) => handleBiasChange('freshnessBias', Number(event.target.value))}
                style={sliderStyle(tastePreference.freshnessBias)}
                disabled={tasteLoading}
              />
              <div className="settings-slider__labels">
                <span>{t('settingsPreferenceFreshnessPast')}</span>
                <span>{t('settingsPreferenceFreshnessBalanced')}</span>
                <span>{t('settingsPreferenceFreshnessNew')}</span>
              </div>
            </div>

            <div className="settings-slider">
              <div className="settings-slider__header">
                <div>
                  <strong>{t('settingsPreferenceDiscovery')}</strong>
                  <p>{t('settingsPreferenceDiscoveryHint')}</p>
                </div>
                <span>{Math.round(tastePreference.discoveryBias * 100)}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="1"
                step="0.1"
                value={tastePreference.discoveryBias}
                onChange={(event) => handleBiasChange('discoveryBias', Number(event.target.value))}
                style={sliderStyle(tastePreference.discoveryBias)}
                disabled={tasteLoading}
              />
              <div className="settings-slider__labels">
                <span>{t('settingsPreferenceDiscoverySafe')}</span>
                <span>{t('settingsPreferenceDiscoveryBalanced')}</span>
                <span>{t('settingsPreferenceDiscoveryBold')}</span>
              </div>
            </div>
          </div>

          <div className="settings-preferences__actions">
            <button type="button" onClick={handlePreferenceSave} disabled={tasteSaving || tasteLoading}>
              {tasteSaving ? t('settingsPreferenceSaving') : t('settingsPreferenceSave')}
            </button>
          </div>
        </section>
      )}
    </div>
  );
};

export default SettingsPage;
