import { useSearchParams } from 'react-router';
import { ErrorState, Loading } from '../../../shared/components/Status';
import { Pagination } from '../../../shared/components/Pagination';
import { PokemonCard } from '../components/PokemonCard';
import { usePokemonPage } from '../hooks/useCatalog';

/** US01: paginated list of pokemon; the page lives in the URL (shareable, back button works). */
export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(0, Number(searchParams.get('page')) || 0);
  const { data, isPending, isError, error, isPlaceholderData, refetch } = usePokemonPage(page);

  const goTo = (next: number) => {
    setSearchParams(next === 0 ? {} : { page: String(next) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <section>
      <div className="page-heading">
        <h1>Browse Pokémon</h1>
        {data && <p className="muted">{data.totalElements.toLocaleString('en-US')} Pokémon from PokeAPI</p>}
      </div>
      {isPending && <Loading label="Loading Pokémon…" />}
      {isError && <ErrorState error={error} onRetry={() => void refetch()} />}
      {data && (
        <>
          <div className="grid" aria-busy={isPlaceholderData}>
            {data.content.map((pokemon) => <PokemonCard key={pokemon.id} pokemon={pokemon} />)}
          </div>
          <Pagination page={page} totalPages={data.totalPages} onChange={goTo} disabled={isPlaceholderData} />
        </>
      )}
    </section>
  );
}
