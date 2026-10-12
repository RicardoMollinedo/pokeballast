import type { AuthResponse, User } from '../api/types';

export const SESSION_STORAGE_KEY = 'pokeballast.session';

export interface StoredSession {
  token: string;
  expiresAt: string;
  user: User;
}

export function toStoredSession(response: AuthResponse): StoredSession {
  return { token: response.accessToken, expiresAt: response.expiresAt, user: response.user };
}

/** Reads the persisted session; expired or corrupt data counts as signed out. */
export function loadSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as StoredSession;
    return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
  } catch {
    return null;
  }
}

export function saveSession(session: StoredSession | null): void {
  if (session) localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  else localStorage.removeItem(SESSION_STORAGE_KEY);
}
