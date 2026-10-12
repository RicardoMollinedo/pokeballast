import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import type { Role } from '../api/types';
import { ErrorState } from '../shared/components/Status';
import { useAuth } from './useAuth';

interface RequireAuthProps {
  children: ReactNode;
  /** When given, the user must have one of these roles. */
  roles?: Role[];
}

/** Route guard: anonymous users go to the login page and come back afterwards. */
export function RequireAuth({ children, roles }: RequireAuthProps) {
  const { user, hasRole } = useAuth();
  const location = useLocation();
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (roles && !hasRole(...roles)) {
    return <ErrorState message="You don't have permission to see this page." />;
  }
  return children;
}
