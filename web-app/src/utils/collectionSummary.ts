import { CollectionSummary, CollectionType } from '../api/movieService';

export const toSummaryMap = (summary: CollectionSummary[] = []) => {
  const map: Record<number, CollectionType[]> = {};
  summary.forEach((item) => {
    map[item.movieId] = item.types;
  });
  return map;
};
