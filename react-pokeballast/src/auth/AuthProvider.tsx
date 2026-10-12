import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { setUnauthorizedListener } from '../api/httpClient';
import type { AuthResponse, Role } from '../api/types';
import { AuthContext, type AuthContextValue } from './AuthContext';
import { loadSession, saveSession, toStoredSession, type StoredSession } from './session';

/**
 * Holds the JWT session. It is persisted in localStorage so a reload keeps the user signed in
 * (trade-off documented in the README: an httpOnly cookie would be safer against XSS).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [stored, setStored] = useState<StoredSession | null>(loadSession);

  const signOut = useCallback(() => {
    saveSession(null);
    setStored(null);
  }, []);

  const signIn = useCallback((response: AuthResponse) => {
    const next = toStoredSession(response);
    saveSession(next);
    setStored(next);
  }, []);

  // any 401 on an authenticated call means the token is no longer valid
  useEffect(() => {
    setUnauthorizedListener(signOut);
    return () => setUnauthorizedListener(null);
  }, [signOut]);

  // sign out when the token expires while the tab is open
  useEffect(() => {
    if (!stored) return;
    const timer = window.setTimeout(signOut, Math.max(new Date(stored.expiresAt).getTime() - Date.now(), 0));
    return () => window.clearTimeout(timer);
  }, [stored, signOut]);

  const value = useMemo<AuthContextValue>(() => ({
    user: stored?.user ?? null,
    session: stored ? { token: stored.token, username: stored.user.username } : null,
    hasRole: (...roles: Role[]) => stored !== null && roles.includes(stored.user.role),
    signIn,
    signOut,
  }), [stored, signIn, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
