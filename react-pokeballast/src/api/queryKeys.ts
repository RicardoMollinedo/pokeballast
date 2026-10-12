export interface CollectionFilter {
  page: number;
  name?: string;
  tag?: string;
}

/** Every TanStack Query key, in one place: easy to reason about caching and invalidation. */
export const queryKeys = {
  catalogPage: (page: number) => ['catalog', 'page', page] as const,
  catalogDetail: (idOrName: string) => ['catalog', 'detail', idOrName] as const,
  /** Prefix of every collection query: invalidate it after any collection mutation. */
  collection: ['collection'] as const,
  collectionList: (filter: CollectionFilter) => ['collection', 'list', filter] as const,
  collectionItem: (id: number) => ['collection', 'item', id] as const,
};
