const TTL_MS = 10 * 60 * 1000;
const MAX_CACHE_SIZE = 500;

interface CacheEntry {
  value: string;
  expires: number;
}

// Map maintains insertion order, providing strict O(1) LRU eviction
const cache = new Map<string, CacheEntry>();
const inflight = new Map<string, Promise<string>>();

export function cached(key: string): string | null {
  const hit = cache.get(key);
  if (!hit) return null;
  if (Date.now() > hit.expires) {
    cache.delete(key);
    return null;
  }
  // Refresh recency in O(1)
  cache.delete(key);
  cache.set(key, hit);
  return hit.value;
}

export function setCache(key: string, value: string): void {
  // If key exists, delete first to refresh position
  if (cache.has(key)) {
    cache.delete(key);
  } else if (cache.size >= MAX_CACHE_SIZE) {
    // Evict oldest (Least Recently Used) entry in O(1)
    const oldestKey = cache.keys().next().value;
    if (oldestKey !== undefined) {
      cache.delete(oldestKey);
    }
  }
  cache.set(key, { value, expires: Date.now() + TTL_MS });
}

export async function getOrExtract(key: string, extract: () => Promise<string>): Promise<string> {
  const hit = cached(key);
  if (hit) return hit;

  const pending = inflight.get(key);
  if (pending) return pending;

  const run = extract()
    .then((value) => {
      setCache(key, value);
      return value;
    })
    .finally(() => {
      inflight.delete(key);
    });
  inflight.set(key, run);
  return run;
}