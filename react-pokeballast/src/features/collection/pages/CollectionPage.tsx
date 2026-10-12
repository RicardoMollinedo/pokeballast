import { useSearchParams } from 'react-router';
import { useAuth } from '../../../auth/useAuth';
import { Pagination } from '../../../shared/components/Pagination';
import { EmptyState, ErrorState, Loading } from '../../../shared/components/Status';
import { CollectionFilters } from '../components/CollectionFilters';
import styles from '../components/Collection.module.css';
import { ImportForm } from '../components/ImportForm';
import { LocalPokemonCard } from '../components/LocalPokemonCard';
import { useCollectionPage } from '../hooks/useCollection';

/** US03/US04: the local replica. Reading is public; actions depend on the role. */
export function CollectionPage() {
  const { hasRole } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const filter = {
    page: Math.max(0, Number(searchParams.get('page')) || 0),
    name: searchParams.get('name') ?? undefined,
    tag: searchParams.get('tag') ?? undefined,
  };
  const { data, isPending, isError, error, isPlaceholderData, refetch } = useCollectionPage(filter);

  const updateParams = (next: { page?: number; name?: string; tag?: string }) => {
    const params = new URLSearchParams();
    if (next.name) params.set('name', next.name);
    if (next.tag) params.set('tag', next.tag);
    if (next.page) params.set('page', String(next.page));
    setSearchParams(params);
  };

  return (
    <section>
      <div className="page-heading">
        <h1>Collection</h1>
        <p className="muted">Pokémon stored in our database, with localized names, regions and internal tags.</p>
      </div>

      {hasRole('TRAINER', 'ADMIN') && <ImportForm />}
      {/* key: re-mount the inputs when the URL filters change (e.g. clicking a #tag link) */}
      <CollectionFilters key={`${filter.name ?? ''}|${filter.tag ?? ''}`} name={filter.name} tag={filter.tag}
        onApply={updateParams} />

      {isPending && <Loading />}
      {isError && <ErrorState error={error} onRetry={() => void refetch()} />}
      {data && data.content.length === 0 && <EmptyState>No Pokémon match these filters.</EmptyState>}
      {data && data.content.length > 0 && (
        <>
          <div className={`grid ${styles.grid}`} aria-busy={isPlaceholderData}>
            {data.content.map((pokemon) => <LocalPokemonCard key={pokemon.id} pokemon={pokemon} />)}
          </div>
          <Pagination page={filter.page} totalPages={data.totalPages}
            onChange={(next) => updateParams({ ...filter, page: next })} disabled={isPlaceholderData} />
        </>
      )}
    </section>
  );
}
