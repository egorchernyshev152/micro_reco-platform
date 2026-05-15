import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { movieService, Movie, CastMember, ActorDetails } from '../api/movieService';
import MovieCard from '../components/MovieCard';
import './actor.css';
import { useTranslation } from '../i18n/translations';
import { resolveImageUrl } from '../utils/imageUrl';
import { formatBiography } from '../utils/text';
import { useUserStore } from '../context/userStore';
import { useActorFavoriteStore } from '../context/actorFavoriteStore';

type ActorState = {
  actor?: CastMember;
};

type ActorInfo = {
  name: string;
  tmdbId?: number;
  profileUrl?: string;
  roles: string[];
};

const ActorPage = () => {
  const { t } = useTranslation();
  const params = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const userId = useUserStore((state) => state.user?.id);
  const actorFavoritesLoadedFor = useActorFavoriteStore((state) => state.loadedUserId);
  const actorFavoritesMap = useActorFavoriteStore((state) => state.favorites);
  const setActorFavorites = useActorFavoriteStore((state) => state.setFavorites);
  const addActorFavoriteLocal = useActorFavoriteStore((state) => state.addFavorite);
  const removeActorFavoriteLocal = useActorFavoriteStore((state) => state.removeFavorite);
  const resetActorFavorites = useActorFavoriteStore((state) => state.reset);
  const encodedName = params.actorName ?? '';
  const decodedName = decodeURIComponent(encodedName);
  const initialActor = (location.state as ActorState | undefined)?.actor;
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actorInfo, setActorInfo] = useState<ActorInfo | null>(() =>
    decodedName
      ? {
          name: decodedName,
          tmdbId: initialActor?.tmdbId,
          profileUrl: initialActor ? resolveImageUrl(initialActor.profileUrl) : undefined,
          roles: initialActor?.character ? [initialActor.character] : []
        }
      : null
  );

  const [favoriteBusy, setFavoriteBusy] = useState(false);

  useEffect(() => {
    if (!decodedName) {
      setError(t('actorMissing'));
      setLoading(false);
      return;
    }
    let active = true;
    setLoading(true);
    const normalized = decodedName.toLowerCase();
    movieService
      .search({ cast: [decodedName], limit: 20 })
      .then((page) => {
        if (!active) return;
        setMovies(page.items);
        if (!page.items.length) {
          setError(t('actorEmpty'));
        } else {
          setError(null);
          const found = page.items
            .map((movie) => movie.cast ?? [])
            .flat()
            .find((member) => member.name.toLowerCase() === normalized);
          if (found) {
            setActorInfo((prev) => {
              const next: ActorInfo = {
                name: found.name,
                tmdbId: found.tmdbId ?? prev?.tmdbId,
                profileUrl: resolveImageUrl(found.profileUrl) ?? prev?.profileUrl,
                roles: prev?.roles?.length ? prev.roles : found.character ? [found.character] : []
              };
              return next;
            });
          }
        }
      })
      .catch(() => {
        if (!active) return;
        setError(t('actorError'));
        setMovies([]);
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [decodedName, t]);

  const [details, setDetails] = useState<ActorDetails | null>(null);

  useEffect(() => {
    if (!actorInfo?.tmdbId) {
      setDetails(null);
      return;
    }
    let active = true;
    movieService
      .getActorDetails(actorInfo.tmdbId)
      .then((data) => {
        if (!active) return;
        setDetails(data);
        if (data?.profileUrl) {
          setActorInfo((prev) => (prev ? { ...prev, profileUrl: prev.profileUrl ?? data.profileUrl } : prev));
        }
      })
      .catch(() => active && setDetails(null));
    return () => {
      active = false;
    };
  }, [actorInfo?.tmdbId]);

  useEffect(() => {
    if (!userId) {
      resetActorFavorites();
      return;
    }
    if (actorFavoritesLoadedFor === userId) return;
    let active = true;
    movieService
      .getFavoriteActors(userId)
      .then((data) => {
        if (active) {
          setActorFavorites(userId, data);
        }
      })
      .catch(() => active && setActorFavorites(userId, []));
    return () => {
      active = false;
    };
  }, [userId, actorFavoritesLoadedFor, setActorFavorites, resetActorFavorites]);

  const actorFacts = useMemo(() => {
    const facts: { label: string; value: string }[] = [];
    if (details?.birthday) {
      facts.push({ label: t('actorBirthdate'), value: details.birthday });
    }
    if (details?.placeOfBirth) {
      facts.push({ label: t('actorBirthplace'), value: details.placeOfBirth });
    }
    if (details?.knownForDepartment) {
      facts.push({ label: t('actorKnownFor'), value: details.knownForDepartment });
    }
    if (details?.alsoKnownAs?.length) {
      facts.push({ label: t('actorAltNames'), value: details.alsoKnownAs.join(', ') });
    }
    if (details?.popularity) {
      facts.push({ label: t('actorPopularity'), value: details.popularity.toFixed(1) });
    }
    return facts;
  }, [details, t]);

  const { bioText, awardText, projectText, triviaText } = useMemo(() => {
    const raw = details?.biography ?? '';
    const emojiRegex = /[\u{1F300}-\u{1F6FF}\u{1F900}-\u{1F9FF}\u2600-\u26FF]/gu;
    const cleaned = raw.replace(/\r/g, '\n');
    const markers = [
      { key: 'awards', label: 'главные награды' },
      { key: 'projects', label: 'главные проекты' },
      { key: 'trivia', label: 'интересный факт' }
    ];
    const lower = cleaned.toLowerCase();
    const sections: Record<string, { start: number; contentStart: number }> = {};
    markers.forEach((marker) => {
      const index = lower.indexOf(marker.label);
      if (index >= 0) {
        const afterLabel = index + marker.label.length;
        const colonIndex = cleaned.indexOf(':', afterLabel);
        const contentStart = colonIndex >= 0 ? colonIndex + 1 : afterLabel;
        sections[marker.key] = { start: index, contentStart };
      }
    });
    const sortedMarkers = Object.entries(sections)
      .map(([key, value]) => ({ key, ...value }))
      .sort((a, b) => a.start - b.start);

    const sanitizeBlock = (input: string) =>
      input
        .replace(emojiRegex, ' ')
        .split(/\n+/)
        .map((line) => line.trim())
        .filter(Boolean)
        .join(' ');

    let cursor = 0;
    const bioParts: string[] = [];
    const result: Record<'awards' | 'projects' | 'trivia', string | null> = {
      awards: null,
      projects: null,
      trivia: null
    };

    sortedMarkers.forEach((marker, idx) => {
      if (cursor < marker.start) {
        bioParts.push(sanitizeBlock(cleaned.slice(cursor, marker.start)));
      }
      const nextStart = sortedMarkers[idx + 1]?.start ?? cleaned.length;
      const content = sanitizeBlock(cleaned.slice(marker.contentStart, nextStart));
      result[marker.key as keyof typeof result] = content || null;
      cursor = nextStart;
    });

    if (cursor < cleaned.length) {
      bioParts.push(sanitizeBlock(cleaned.slice(cursor, cleaned.length)));
    }

    const bioText = bioParts.filter(Boolean).join('\n\n');
    return {
      bioText,
      awardText: result.awards,
      projectText: result.projects,
      triviaText: result.trivia
    };
  }, [details?.biography]);

  const knownForItems = details?.knownFor ?? [];
  const currentFavorite = actorInfo?.tmdbId ? actorFavoritesMap[actorInfo.tmdbId] : undefined;

  if (!decodedName) {
    return (
      <div className="actor-page">
        <p className="actor-page__status actor-page__status--error">{t('actorMissing')}</p>
      </div>
    );
  }

  if (loading && !movies.length) {
    return <p className="actor-page__status">{t('catalogLoading')}</p>;
  }

  return (
    <div className="actor-page">
      <div className="actor-hero">
        <div className="actor-portrait">
          {actorInfo?.profileUrl ? (
            <img src={actorInfo.profileUrl} alt={actorInfo.name} loading="lazy" referrerPolicy="no-referrer" />
          ) : (
            <span>🎭</span>
          )}
        </div>
        <div className="actor-hero__info">
          <p className="eyebrow">{t('actorTitle')}</p>
          <h1>{actorInfo?.name ?? decodedName}</h1>
          {actorInfo?.roles.length ? <p className="actor-roles">{actorInfo.roles.join(' • ')}</p> : null}
        </div>
        {actorInfo?.tmdbId && (
          <div className="actor-hero__actions">
            <button
              type="button"
              className={`actor-likeBtn ${currentFavorite ? 'is-active' : ''}`}
              disabled={favoriteBusy}
              aria-label={currentFavorite ? t('actorFavoriteRemove') : t('actorFavoriteAdd')}
              onClick={() => {
                if (!userId) {
                  navigate('/login');
                  return;
                }
                const actorTmdbId = actorInfo.tmdbId;
                setFavoriteBusy(true);
                if (currentFavorite) {
                  movieService
                    .removeFavoriteActor(userId, actorTmdbId)
                    .then(() => removeActorFavoriteLocal(actorTmdbId))
                    .catch(() => alert(t('actorFavoriteError')))
                    .finally(() => setFavoriteBusy(false));
                } else {
                  movieService
                    .addFavoriteActor(userId, actorTmdbId, {
                      actorName: actorInfo.name,
                      profileUrl: actorInfo.profileUrl
                    })
                    .then((favorite) => addActorFavoriteLocal(favorite))
                    .catch(() => alert(t('actorFavoriteError')))
                    .finally(() => setFavoriteBusy(false));
                }
              }}
              title={currentFavorite ? t('actorFavoriteRemove') : t('actorFavoriteAdd')}
            >
              {currentFavorite ? '♥' : '♡'}
            </button>
          </div>
        )}
      </div>

      <section className="actor-features">
        {[
          { title: t('actorBioTitle'), text: bioText || '—' },
          { title: t('actorAwardsTitle'), text: awardText || '—' },
          { title: t('actorProjectsTitle'), text: projectText || '—' },
          { title: t('actorTriviaTitle'), text: triviaText || '—' }
        ].map((card) => (
          <div key={card.title} className="actor-feature-card">
            <h3>{card.title}</h3>
            <p>{card.text}</p>
          </div>
        ))}
      </section>
      {(actorFacts.length || details?.highlights?.length || knownForItems.length) && (
        <section className="actor-info-grid">
          {actorFacts.length > 0 && (
            <div>
              <h4>{t('actorFactsTitle')}</h4>
              <ul>
                {actorFacts.map((fact) => (
                  <li key={fact.label}>
                    <span>{fact.label}</span>
                    <strong>{fact.value}</strong>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {details?.highlights?.length ? (
            <div>
              <h4>{t('actorHighlightsTitle')}</h4>
              <ul>
                {details.highlights.map((item, index) => (
                  <li key={`highlight-${index}`}>{item}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {knownForItems.length ? (
            <div>
              <h4>{t('actorKnownForTitle')}</h4>
              <div className="actor-knownfor">
                  {knownForItems.map((item, index) => {
                    const preferredRating = item.catalogRating ?? item.voteAverage ?? null;
                    const subtitle = [item.year, preferredRating ? `${preferredRating.toFixed(1)}/10` : null, item.character]
                      .filter(Boolean)
                      .join(' • ');
                  const content = (
                    <>
                      <strong>{item.title}</strong>
                      {subtitle && <span>{subtitle}</span>}
                    </>
                  );
                  return item.movieId ? (
                    <Link key={`knownfor-${index}`} to={`/movie/${item.movieId}`} className="actor-knownfor__item">
                      {content}
                    </Link>
                  ) : (
                    <div key={`knownfor-${index}`} className="actor-knownfor__item">
                      {content}
                    </div>
                  );
                })}
              </div>
            </div>
          ) : null}
        </section>
      )}

      {error && <p className="actor-page__status actor-page__status--error">{error}</p>}

      {movies.length > 0 && (
        <section className="actor-body">
          <div className="actor-body__header">
            <h2>{t('actorMovies')}</h2>
            <span>
              {t('actorMoviesCount')} {movies.length}
            </span>
          </div>
          <div className="actor-movie-grid">
            {movies.map((movie) => (
              <MovieCard key={movie.id} movie={movie} showMeta={false} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
};

export default ActorPage;
