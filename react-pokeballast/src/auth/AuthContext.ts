import { createContext } from 'react';
import type { Session } from '../api/httpClient';
import type { AuthResponse, Role, User } from '../api/types';

export interface AuthContextValue {
  user: User | null;
  /** Credentials for the API layer; null when signed out. */
  session: Session | null;
  hasRole: (...roles: Role[]) => boolean;
  signIn: (response: AuthResponse) => void;
  signOut: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
