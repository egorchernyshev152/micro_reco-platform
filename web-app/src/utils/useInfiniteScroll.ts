import { MutableRefObject, useCallback, useEffect, useRef } from 'react';

export const useInfiniteScroll = <T extends HTMLElement>(
  loadMore: () => void,
  options?: {
    enabled?: boolean;
    rootMargin?: string;
  }
): MutableRefObject<T | null> => {
  const targetRef = useRef<T | null>(null);
  const latestLoad = useRef(loadMore);
  latestLoad.current = loadMore;

  const observerCallback = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const entry = entries[0];
      if (!entry?.isIntersecting) return;
      latestLoad.current();
    },
    []
  );

  useEffect(() => {
    if (!options?.enabled) return;
    const target = targetRef.current;
    if (!target) return;
    const observer = new IntersectionObserver(observerCallback, {
      rootMargin: options?.rootMargin ?? '120px'
    });
    observer.observe(target);
    return () => observer.disconnect();
  }, [observerCallback, options?.enabled, options?.rootMargin]);

  return targetRef;
};
