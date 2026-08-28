const presence = new Map<string, number>();

export function bumpPresence(userId: string): void {
  presence.set(userId, Date.now());
}

export function getPresenceMs(userId: string): number | undefined {
  return presence.get(userId);
}

export function getPresenceIso(userId: string): string | null {
  const ts = presence.get(userId);
  return ts ? new Date(ts).toISOString() : null;
}