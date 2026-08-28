const TTL_MS = 10 * 60 * 1000;

const cache = new Map<string, { value: string; expires: number }>();
const inflight = new Map<string, Promise<string>>();

export function cached(key: string): string | null {
  const hit = cache.get(key);
  if (!hit) return null;
  if (Date.now() > hit.expires) {
    cache.delete(key);
    return null;
  }
  return hit.value;
}

export function setCache(key: string, value: string): void {
  cache.set(key, { value, expires: Date.now() + TTL_MS });
  if (cache.size > 500) {
    const now = Date.now();
    for (const [k, v] of cache) {
      if (now > v.expires) cache.delete(k);
    }
  }
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