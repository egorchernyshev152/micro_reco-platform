const META_PREFIXES = [/^жанры:/i, /^страны:/i, /^genres:/i, /^countries:/i, /^в ролях:/i];

const emojiPattern = /[\u{1F300}-\u{1F6FF}\u{1F900}-\u{1F9FF}\u2600-\u26FF]/gu;

export const sanitizeSynopsisParagraphs = (raw?: string): string[] => {
  if (!raw) return [];
  return raw
    .split(/\n{1,2}/)
    .map((part) => part.replace(emojiPattern, '').trim())
    .filter((part) => part.length > 0)
    .filter((part) => !META_PREFIXES.some((prefix) => prefix.test(part)));
};

export const buildSynopsisPreview = (raw?: string): string | null => {
  const paragraphs = sanitizeSynopsisParagraphs(raw);
  if (!paragraphs.length) return null;
  if (paragraphs.length === 1) return paragraphs[0];
  return `${paragraphs[0]} ${paragraphs[1]}`.trim();
};

export const formatBiography = (raw?: string): string[] => {
  if (!raw) return [];
  const normalized = raw
    .replace(/[•·▪︎]/g, '\n')
    .replace(/[\u{1F300}-\u{1F6FF}\u{1F900}-\u{1F9FF}\u2600-\u26FF]/gu, '\n')
    .replace(/\r/g, '\n');
  return normalized
    .split(/\n+/)
    .map((line) => {
      const withoutMeta = META_PREFIXES.reduce((acc, regex) => acc.replace(regex, ''), line);
      return withoutMeta.trim();
    })
    .map((line) => line.replace(/^[-–:*]+/, '').trim())
    .filter((line) => line.length > 0);
};
