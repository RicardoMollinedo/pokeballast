import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { localPokemonApi } from '../../../api/localPokemonApi';
import { pokemonApi } from '../../../api/pokemonApi';
import { queryKeys } from '../../../api/queryKeys';
import { useAuth } from '../../../auth/useAuth';

/** US01: one page of the PokeAPI catalog; the previous page stays visible while the next loads. */
export function usePokemonPage(page: number) {
  return useQuery({
    queryKey: queryKeys.catalogPage(page),
    queryFn: ({ signal }) => pokemonApi.list(page, signal),
    placeholderData: keepPreviousData,
  });
}

/** US02: full detail of one pokemon. */
export function usePokemonDetail(idOrName: string) {
  return useQuery({
    queryKey: queryKeys.catalogDetail(idOrName),
    queryFn: ({ signal }) => pokemonApi.detail(idOrName, signal),
  });
}

/**
 * US03 from the detail page: is this pokemon already stored, and how to import it.
 * Asking first avoids an expected 409 showing up as an error in the browser console.
 */
export function useSaveToCollection(pokeApiId: number, name: string) {
  const { session } = useAuth();
  const queryClient = useQueryClient();
  const filter = { page: 0, name };

  const existing = useQuery({
    queryKey: queryKeys.collectionList(filter),
    queryFn: ({ signal }) => localPokemonApi.list(filter, signal),
    enabled: session !== null,
    select: (result) => result.content.some((pokemon) => pokemon.pokeApiId === pokeApiId),
  });

  const save = useMutation({
    mutationFn: () => localPokemonApi.import(String(pokeApiId), session!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.collection }),
  });

  return { canSave: session !== null, alreadySaved: existing.data === true, checking: existing.isPending, save };
}
