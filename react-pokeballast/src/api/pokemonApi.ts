import { request } from './httpClient';
import type { Page, PokemonDetail, PokemonSummary } from './types';

export const CATALOG_PAGE_SIZE = 20;

/** PokemonController: read-only PokeAPI catalog (US01, US02). */
export const pokemonApi = {
  list: (page: number, signal?: AbortSignal) =>
    request<Page<PokemonSummary>>(`/pokemon?page=${page}&size=${CATALOG_PAGE_SIZE}`, { signal }),
  detail: (idOrName: string, signal?: AbortSignal) =>
    request<PokemonDetail>(`/pokemon/${encodeURIComponent(idOrName)}`, { signal }),
};
