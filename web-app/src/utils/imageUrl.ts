const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p';

const isAbsolute = (input: string) => /^https?:\/\//i.test(input) || input.startsWith('data:');

export const resolveImageUrl = (raw?: string | null, fallbackSize = 'w500') => {
  if (!raw) return undefined;
  const trimmed = raw.trim();
  if (!trimmed) return undefined;
  if (isAbsolute(trimmed)) {
    return trimmed;
  }
  let normalized = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  if (!/^\/(w|h|original)/.test(normalized)) {
    normalized = `/${fallbackSize}${normalized}`;
  }
  return `${TMDB_IMAGE_BASE}${normalized}`;
};
