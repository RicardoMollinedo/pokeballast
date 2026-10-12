import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import type { AuthResponse } from '../api/types';
import { AuthProvider } from '../auth/AuthProvider';
import { SESSION_STORAGE_KEY } from '../auth/session';

interface Options {
  route?: string;
  path?: string;
  session?: AuthResponse;
}

/** Shows the current location, so tests can assert on navigations. */
function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
}

export function renderWithProviders(ui: ReactElement, { route = '/', path = '/', session }: Options = {}) {
  if (session) {
    localStorage.setItem(SESSION_STORAGE_KEY,
      JSON.stringify({ token: session.accessToken, expiresAt: session.expiresAt, user: session.user }));
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <MemoryRouter initialEntries={[route]}>
          <Routes>
            <Route path={path} element={ui} />
            <Route path="*" element={null} />
          </Routes>
          <LocationProbe />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}
