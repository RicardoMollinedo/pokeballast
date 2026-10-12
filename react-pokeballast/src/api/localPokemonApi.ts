import { request, type Session } from './httpClient';
import type { CollectionFilter } from './queryKeys';
import type { LocalPokemon, Page, UpdateLocalPokemon } from './types';

export const COLLECTION_PAGE_SIZE = 20;

/** LocalPokemonController: the local replica (US03, US04). */
export const localPokemonApi = {
  list: ({ page, name, tag }: CollectionFilter, signal?: AbortSignal) => {
    const params = new URLSearchParams({ page: String(page), size: String(COLLECTION_PAGE_SIZE) });
    if (name) params.set('name', name);
    if (tag) params.set('tag', tag);
    return request<Page<LocalPokemon>>(`/local-pokemon?${params.toString()}`, { signal });
  },
  get: (id: number, signal?: AbortSignal) => request<LocalPokemon>(`/local-pokemon/${id}`, { signal }),
  import: (idOrName: string, session: Session) =>
    request<LocalPokemon>('/local-pokemon', { method: 'POST', body: { idOrName }, session }),
  refresh: (id: number, session: Session) =>
    request<LocalPokemon>(`/local-pokemon/${id}/refresh`, { method: 'POST', session }),
  update: (id: number, body: UpdateLocalPokemon, session: Session) =>
    request<LocalPokemon>(`/local-pokemon/${id}`, { method: 'PUT', body, session }),
  /** 204 No Content on success. */
  remove: (id: number, session: Session) => request<void>(`/local-pokemon/${id}`, { method: 'DELETE', session }),
};
