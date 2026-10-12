import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/httpClient';

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000, // PokeAPI data barely changes: avoid refetching on every navigation
        refetchOnWindowFocus: false,
        // never retry client errors (4xx); retry server and network errors twice
        retry: (failureCount, error) =>
          !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 2,
      },
    },
  });
}
