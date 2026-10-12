import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ErrorCode } from '../../../api/errorCodes';
import { ApiError } from '../../../api/httpClient';
import { localPokemonApi } from '../../../api/localPokemonApi';
import { queryKeys, type CollectionFilter } from '../../../api/queryKeys';
import type { UpdateLocalPokemon } from '../../../api/types';
import { useAuth } from '../../../auth/useAuth';

export function useCollectionPage(filter: CollectionFilter) {
  return useQuery({
    queryKey: queryKeys.collectionList(filter),
    queryFn: ({ signal }) => localPokemonApi.list(filter, signal),
    placeholderData: keepPreviousData,
  });
}

export function useLocalPokemon(id: number) {
  return useQuery({
    queryKey: queryKeys.collectionItem(id),
    queryFn: ({ signal }) => localPokemonApi.get(id, signal),
    enabled: Number.isInteger(id) && id > 0,
  });
}

/** Every collection mutation invalidates the whole ['collection'] prefix: lists and items refetch. */
function useInvalidateCollection() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.collection });
}

export function useImportPokemon() {
  const { session } = useAuth();
  const invalidate = useInvalidateCollection();
  return useMutation({
    mutationFn: (idOrName: string) => localPokemonApi.import(idOrName, session!),
    onSuccess: invalidate,
  });
}

export function useRefreshPokemon(id: number) {
  const { session } = useAuth();
  const invalidate = useInvalidateCollection();
  return useMutation({ mutationFn: () => localPokemonApi.refresh(id, session!), onSuccess: invalidate });
}

export function useDeletePokemon(id: number) {
  const { session } = useAuth();
  const invalidate = useInvalidateCollection();
  return useMutation({ mutationFn: () => localPokemonApi.remove(id, session!), onSuccess: invalidate });
}

export function isVersionConflict(error: unknown): error is ApiError {
  return error instanceof ApiError
    && (error.code === ErrorCode.STALE_VERSION || error.code === ErrorCode.CONCURRENT_MODIFICATION);
}

export function useUpdatePokemon(id: number) {
  const { session } = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: UpdateLocalPokemon) => localPokemonApi.update(id, values, session!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.collection }),
    onError: (error) => {
      // someone saved first: reload so the form re-mounts with the latest version
      if (isVersionConflict(error)) void queryClient.invalidateQueries({ queryKey: queryKeys.collectionItem(id) });
    },
  });
}
