import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import './movie-admin.css';
import {
  MOVIE_STATUSES,
  Movie,
  MovieAssetType,
  MoviePayload,
  MovieStatus,
  movieService
} from '../api/movieService';
import { useTranslation } from '../i18n/translations';
import { useUserStore } from '../context/userStore';
import { notifyError, notifyInfo, notifySuccess } from '../context/notificationStore';

type StatusFilter = MovieStatus | 'ALL';

type MovieFormState = {
  title: string;
  originalTitle: string;
  originalLanguage: string;
  status: MovieStatus;
  releaseDate: string;
  releaseYear: string;
  durationMinutes: string;
  ageRating: string;
  tagline: string;
  budget: string;
  revenue: string;
  description: string;
  synopsis: string;
  posterUrl: string;
  backdropUrl: string;
  trailerUrl: string;
  genresText: string;
  countriesText: string;
  tagsText: string;
};

const defaultFormState = (): MovieFormState => ({
  title: '',
  originalTitle: '',
  originalLanguage: '',
  status: 'DRAFT',
  releaseDate: '',
  releaseYear: '',
  durationMinutes: '',
  ageRating: '',
  tagline: '',
  budget: '',
  revenue: '',
  description: '',
  synopsis: '',
  posterUrl: '',
  backdropUrl: '',
  trailerUrl: '',
  genresText: '',
  countriesText: '',
  tagsText: ''
});

const MovieAdminPage = () => {
  const user = useUserStore((state) => state.user);
  const { t } = useTranslation();

  const [searchText, setSearchText] = useState('');
  const [filters, setFilters] = useState<{ query: string; status: StatusFilter }>({ query: '', status: 'ALL' });
  const [movies, setMovies] = useState<Movie[]>([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [formState, setFormState] = useState<MovieFormState>(() => defaultFormState());
  const [editingMovie, setEditingMovie] = useState<Movie | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const statusLabels = useMemo(
    () => ({
      DRAFT: t('movieStatusDraft'),
      READY: t('movieStatusReady'),
      PUBLISHED: t('movieStatusPublished'),
      ARCHIVED: t('movieStatusArchived')
    }),
    [t]
  );

  const loadMovies = useCallback(
    (page = 0) => {
      setLoading(true);
      const statuses = filters.status === 'ALL' ? MOVIE_STATUSES : [filters.status];
      movieService
        .search({
          query: filters.query || undefined,
          statuses,
          page,
          limit: 20
        })
        .then((response) => {
          setMovies(response.items);
          setPageInfo({ page: response.page, totalPages: response.totalPages, totalElements: response.totalElements });
        })
        .catch((error) => {
          console.error('Failed to load movies', error);
          notifyError(t('adminMoviesLoadError'));
        })
        .finally(() => setLoading(false));
    },
    [filters, t]
  );

  useEffect(() => {
    loadMovies(0);
  }, [loadMovies]);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  const openCreateDrawer = () => {
    setEditingMovie(null);
    setFormState(defaultFormState());
    setDrawerOpen(true);
  };

  const openEditDrawer = (movie: Movie) => {
    setEditingMovie(movie);
    setFormState(fromMovie(movie));
    setDrawerOpen(true);
  };

  const closeDrawer = () => {
    if (saving) return;
    setDrawerOpen(false);
    setEditingMovie(null);
    setFormState(defaultFormState());
  };

  const handleSearch = (event: FormEvent) => {
    event.preventDefault();
    setFilters((prev) => ({ ...prev, query: searchText.trim() }));
  };

  const handleStatusChange = (status: StatusFilter) => {
    setFilters((prev) => ({ ...prev, status }));
  };

  const goToPage = (page: number) => {
    if (page < 0 || page >= pageInfo.totalPages) return;
    loadMovies(page);
  };

  const updateField = <K extends keyof MovieFormState>(field: K, value: MovieFormState[K]) => {
    setFormState((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = (event: FormEvent) => {
    event.preventDefault();
    if (!formState.title.trim()) {
      notifyError(t('movieFormTitleRequired'));
      return;
    }
    const payload = toPayload(formState);
    setSaving(true);
    const action = editingMovie ? movieService.updateMovie(editingMovie.id, payload) : movieService.createMovie(payload);
    action
      .then((saved) => {
        notifySuccess(t('movieFormSaved'));
        setEditingMovie(saved);
        setFormState(fromMovie(saved));
        loadMovies(pageInfo.page);
      })
      .catch((error) => {
        console.error('Failed to save movie', error);
        notifyError(t('movieFormSaveError'));
      })
      .finally(() => setSaving(false));
  };

  const handleDelete = () => {
    if (!editingMovie) return;
    if (!window.confirm(t('movieFormConfirmDelete'))) {
      return;
    }
    setDeleting(true);
    movieService
      .deleteMovie(editingMovie.id)
      .then(() => {
        notifySuccess(t('movieFormDeleted'));
        closeDrawer();
        loadMovies(0);
      })
      .catch((error) => {
        console.error('Failed to delete movie', error);
        notifyError(t('movieFormDeleteError'));
      })
      .finally(() => setDeleting(false));
  };

  const assetMovieId = editingMovie?.id;

  return (
    <div className="movie-admin">
      <header className="movie-admin__header">
        <div>
          <p className="eyebrow">{t('adminMoviesSubtitle')}</p>
          <h1>{t('adminMoviesTitle')}</h1>
        </div>
        <button type="button" className="primary" onClick={openCreateDrawer}>
          {t('adminMoviesCreate')}
        </button>
      </header>

      <form className="movie-admin__filters" onSubmit={handleSearch}>
        <input
          type="search"
          placeholder={t('adminMoviesSearchPlaceholder')}
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
        />
        <button type="submit" className="ghost-button">
          {t('searchMovies')}
        </button>
      </form>

      <div className="movie-admin__status">
        <span>{t('adminMoviesStatusFilter')}</span>
        <div className="status-chip-list">
          {(['ALL', ...MOVIE_STATUSES] as StatusFilter[]).map((status) => (
            <button
              key={status}
              type="button"
              className={`status-chip ${filters.status === status ? 'is-active' : ''}`}
              onClick={() => handleStatusChange(status)}
            >
              {status === 'ALL' ? t('adminMoviesStatusAll') : statusLabels[status]}
            </button>
          ))}
        </div>
      </div>

      <section className="movie-admin__list">
        {loading && <p>{t('catalogLoading')}</p>}
        {!loading && movies.length === 0 && <p>{t('adminMoviesEmpty')}</p>}
        {!loading &&
          movies.map((movie) => (
            <article key={movie.id} className="movie-admin__card" onClick={() => openEditDrawer(movie)}>
              <div>
                <p className={`status-chip status-chip--inline status-${movie.status.toLowerCase()}`}>{statusLabels[movie.status]}</p>
                <h3>{movie.title}</h3>
                <p>
                  {movie.releaseYear ?? '—'} • {movie.genres?.slice(0, 3).join(', ')}
                </p>
              </div>
            </article>
          ))}
      </section>

      {pageInfo.totalPages > 1 && (
        <div className="movie-admin__pagination">
          <button type="button" onClick={() => goToPage(pageInfo.page - 1)} disabled={pageInfo.page === 0}>
            {t('paginationPrev')}
          </button>
          <span>
            {pageInfo.page + 1}/{pageInfo.totalPages}
          </span>
          <button type="button" onClick={() => goToPage(pageInfo.page + 1)} disabled={pageInfo.page >= pageInfo.totalPages - 1}>
            {t('paginationNext')}
          </button>
        </div>
      )}

      <div className={`movie-drawer ${drawerOpen ? 'is-open' : ''}`}>
        <div className="movie-drawer__header">
          <div>
            <p className="eyebrow">{editingMovie ? t('movieFormDrawerEdit') : t('movieFormDrawerCreate')}</p>
            <h2>{formState.title || t('movieFormTitle')}</h2>
          </div>
          <button type="button" className="ghost-button" onClick={closeDrawer}>
            {t('movieFormCancel')}
          </button>
        </div>

        <form className="movie-drawer__form" onSubmit={handleSave}>
          <div className="movie-form__grid">
            <label>
              {t('movieFormTitle')}
              <input type="text" value={formState.title} onChange={(event) => updateField('title', event.target.value)} required />
            </label>
            <label>
              {t('movieFormOriginalTitle')}
              <input type="text" value={formState.originalTitle} onChange={(event) => updateField('originalTitle', event.target.value)} />
            </label>
            <label>
              {t('movieFormOriginalLanguage')}
              <input type="text" value={formState.originalLanguage} onChange={(event) => updateField('originalLanguage', event.target.value)} />
            </label>
            <label>
              {t('movieFormStatus')}
              <select value={formState.status} onChange={(event) => updateField('status', event.target.value as MovieStatus)}>
                {MOVIE_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {statusLabels[status]}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {t('movieFormReleaseDate')}
              <input type="date" value={formState.releaseDate} onChange={(event) => updateField('releaseDate', event.target.value)} />
            </label>
            <label>
              {t('movieFormReleaseYear')}
              <input type="number" value={formState.releaseYear} onChange={(event) => updateField('releaseYear', event.target.value)} />
            </label>
            <label>
              {t('movieFormDuration')}
              <input type="number" value={formState.durationMinutes} onChange={(event) => updateField('durationMinutes', event.target.value)} />
            </label>
            <label>
              {t('movieFormAgeRating')}
              <input type="text" value={formState.ageRating} onChange={(event) => updateField('ageRating', event.target.value)} />
            </label>
            <label>
              {t('movieFormTagline')}
              <input type="text" value={formState.tagline} onChange={(event) => updateField('tagline', event.target.value)} />
            </label>
            <label>
              {t('movieFormBudget')}
              <input type="number" value={formState.budget} onChange={(event) => updateField('budget', event.target.value)} />
            </label>
            <label>
              {t('movieFormRevenue')}
              <input type="number" value={formState.revenue} onChange={(event) => updateField('revenue', event.target.value)} />
            </label>
            <label>
              {t('movieFormTrailer')}
              <input type="url" value={formState.trailerUrl} onChange={(event) => updateField('trailerUrl', event.target.value)} />
            </label>
            <label>
              {t('movieFormGenres')}
              <input type="text" value={formState.genresText} onChange={(event) => updateField('genresText', event.target.value)} placeholder="Drama, Action" />
            </label>
            <label>
              {t('movieFormCountries')}
              <input type="text" value={formState.countriesText} onChange={(event) => updateField('countriesText', event.target.value)} placeholder="USA, UK" />
            </label>
            <label>
              {t('movieFormTags')}
              <input type="text" value={formState.tagsText} onChange={(event) => updateField('tagsText', event.target.value)} placeholder="IMAX, Streaming" />
            </label>
          </div>

          <label>
            {t('movieFormDescription')}
            <textarea rows={3} value={formState.description} onChange={(event) => updateField('description', event.target.value)} />
          </label>

          <label>
            {t('movieFormSynopsis')}
            <textarea rows={3} value={formState.synopsis} onChange={(event) => updateField('synopsis', event.target.value)} />
          </label>

          <AssetUploadField
            label={t('movieFormPoster')}
            movieId={assetMovieId}
            type="POSTER"
            value={formState.posterUrl}
            onChange={(value) => updateField('posterUrl', value)}
          />
          <AssetUploadField
            label={t('movieFormBackdrop')}
            movieId={assetMovieId}
            type="BACKDROP"
            value={formState.backdropUrl}
            onChange={(value) => updateField('backdropUrl', value)}
          />

          <div className="movie-form__actions">
            {editingMovie && (
              <button type="button" className="danger" onClick={handleDelete} disabled={deleting}>
                {deleting ? t('catalogLoading') : t('movieFormDelete')}
              </button>
            )}
            <div className="movie-form__spacer" />
            <button type="button" onClick={closeDrawer} disabled={saving}>
              {t('movieFormCancel')}
            </button>
            <button type="submit" className="primary" disabled={saving}>
              {saving ? t('catalogLoading') : t('movieFormSave')}
            </button>
          </div>
        </form>
      </div>
      <div className={`drawer-backdrop ${drawerOpen ? 'is-visible' : ''}`} onClick={closeDrawer} />
    </div>
  );
};

const splitList = (value: string) =>
  value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);

const parseNumber = (value: string) => {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const fromMovie = (movie: Movie): MovieFormState => ({
  title: movie.title,
  originalTitle: movie.originalTitle ?? '',
  originalLanguage: movie.originalLanguage ?? '',
  status: movie.status,
  releaseDate: movie.releaseDate ?? '',
  releaseYear: movie.releaseYear ? String(movie.releaseYear) : '',
  durationMinutes: movie.durationMinutes ? String(movie.durationMinutes) : '',
  ageRating: movie.ageRating ?? '',
  tagline: movie.tagline ?? '',
  budget: movie.budget ? String(movie.budget) : '',
  revenue: movie.revenue ? String(movie.revenue) : '',
  description: movie.description ?? '',
  synopsis: movie.synopsis ?? '',
  posterUrl: movie.posterUrl ?? '',
  backdropUrl: movie.backdropUrl ?? '',
  trailerUrl: movie.trailerUrl ?? '',
  genresText: movie.genres?.join(', ') ?? '',
  countriesText: movie.countries?.join(', ') ?? '',
  tagsText: movie.tags?.join(', ') ?? ''
});

const toPayload = (state: MovieFormState): MoviePayload => ({
  title: state.title.trim(),
  originalTitle: state.originalTitle.trim() || undefined,
  originalLanguage: state.originalLanguage.trim() || undefined,
  status: state.status,
  releaseDate: state.releaseDate || undefined,
  releaseYear: parseNumber(state.releaseYear),
  durationMinutes: parseNumber(state.durationMinutes),
  ageRating: state.ageRating.trim() || undefined,
  tagline: state.tagline.trim() || undefined,
  budget: parseNumber(state.budget),
  revenue: parseNumber(state.revenue),
  description: state.description.trim() || undefined,
  synopsis: state.synopsis.trim() || undefined,
  posterUrl: state.posterUrl.trim() || undefined,
  backdropUrl: state.backdropUrl.trim() || undefined,
  trailerUrl: state.trailerUrl.trim() || undefined,
  genres: splitList(state.genresText),
  countries: splitList(state.countriesText),
  tags: splitList(state.tagsText)
});

type AssetUploadFieldProps = {
  label: string;
  movieId?: number;
  value?: string;
  type: MovieAssetType;
  onChange: (value: string) => void;
};

const AssetUploadField = ({ label, movieId, value, type, onChange }: AssetUploadFieldProps) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleSelect = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!movieId) {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      const objectUrl = URL.createObjectURL(file);
      setPreviewUrl(objectUrl);
      onChange(objectUrl);
      notifyInfo(t('movieFormUploadHint'));
      return;
    }
    try {
      setUploading(true);
      const asset = await movieService.uploadAsset(movieId, file, type);
      onChange(asset.url);
      setPreviewUrl(null);
      notifySuccess(t('movieFormUploadSuccess'));
    } catch (error) {
      console.error('Failed to upload asset', error);
      notifyError(t('movieFormUploadError'));
    } finally {
      setUploading(false);
    }
  };

  const preview = previewUrl || value;

  return (
    <div className="asset-field">
      <div className="asset-field__preview">
        {preview ? <img src={preview} alt={`${label} preview`} /> : <div className="asset-field__placeholder">{t('movieFormPreview')}</div>}
      </div>
      <div className="asset-field__body">
        <span>{label}</span>
        <input type="url" value={value ?? ''} onChange={(event) => onChange(event.target.value)} placeholder="https://…" />
        <button type="button" className="ghost-button" onClick={handleSelect} disabled={uploading}>
          {uploading ? t('movieFormUploadPending') : t('movieFormUpload')}
        </button>
        {!movieId && <p className="asset-field__hint">{t('movieFormUploadHint')}</p>}
      </div>
      <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleFileChange} />
    </div>
  );
};

export default MovieAdminPage;
