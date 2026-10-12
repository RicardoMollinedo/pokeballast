import { Link, useNavigate, useParams } from 'react-router';
import { ApiError } from '../../../api/httpClient';
import { ErrorState, Loading } from '../../../shared/components/Status';
import { formatName } from '../../../shared/utils/format';
import styles from '../components/Collection.module.css';
import { EditPokemonForm } from '../components/EditPokemonForm';
import { isVersionConflict, useLocalPokemon, useUpdatePokemon } from '../hooks/useCollection';

/** US04: edit the proprietary fields, with optimistic-locking conflict handling. */
export function EditPokemonPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const { data: pokemon, isPending, isError, error } = useLocalPokemon(id);
  const update = useUpdatePokemon(id);

  if (!Number.isInteger(id) || id <= 0) return <ErrorState message="Invalid id." />;
  if (isPending) return <Loading />;
  if (isError) return <ErrorState error={error} action={<Link to="/collection">Back to the collection</Link>} />;

  const conflict = isVersionConflict(update.error) ? update.error : null;
  const serverErrors = update.error instanceof ApiError ? update.error.fieldErrors : {};
  const otherError = update.isError && !conflict && Object.keys(serverErrors).length === 0 ? update.error : null;

  return (
    <section className="narrow">
      <Link to="/collection" className="back-link">← Back to the collection</Link>
      <h1>Edit {pokemon.localizedName ?? formatName(pokemon.name)}</h1>
      <p className={`muted ${styles.readOnly}`}>
        {formatName(pokemon.name)} · {pokemon.category ?? 'Unknown category'} · {pokemon.types.join(', ')} ·{' '}
        {pokemon.weightKg} kg. Data from PokeAPI is read-only: re-sync refreshes it.
      </p>
      {conflict && (
        <p className={`status-error ${styles.conflict}`} role="alert">
          {conflict.message} The form now shows the latest saved values.
        </p>
      )}
      {otherError && <p className="error-text" role="alert">{otherError.message}</p>}
      {/* key: re-mount with fresh values when a newer version is loaded */}
      <EditPokemonForm key={pokemon.version} pokemon={pokemon} submitting={update.isPending}
        serverErrors={serverErrors}
        onSubmit={(values) => update.mutate(values, { onSuccess: () => navigate('/collection') })} />
    </section>
  );
}
