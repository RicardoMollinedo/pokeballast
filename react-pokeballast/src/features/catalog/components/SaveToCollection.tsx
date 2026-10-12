import { Link, useLocation } from 'react-router';
import { ErrorCode } from '../../../api/errorCodes';
import { ApiError } from '../../../api/httpClient';
import { useAuth } from '../../../auth/useAuth';
import { useSaveToCollection } from '../hooks/useCatalog';

/** US03 entry point: replicate the pokemon being viewed into the local database. */
export function SaveToCollection({ pokeApiId, name }: { pokeApiId: number; name: string }) {
  const { hasRole } = useAuth();
  const location = useLocation();
  const { canSave, alreadySaved, checking, save } = useSaveToCollection(pokeApiId, name);

  if (!canSave) {
    return (
      <p className="muted">
        <Link to="/login" state={{ from: location.pathname }}>Sign in</Link> to save this Pokémon to the collection.
      </p>
    );
  }
  if (!hasRole('TRAINER', 'ADMIN')) return null;

  const conflict = save.error instanceof ApiError && save.error.code === ErrorCode.POKEMON_ALREADY_IMPORTED;
  if ((alreadySaved && !save.isSuccess) || conflict) {
    return (
      <p className="muted" role="status">
        Already in the collection. <Link to={`/collection?name=${encodeURIComponent(name)}`}>View it</Link>
      </p>
    );
  }
  if (save.isSuccess) {
    return (
      <p className="success-text" role="status">
        Saved to the collection. <Link to="/collection">Go to the collection</Link>
      </p>
    );
  }
  return (
    <div>
      <button type="button" className="primary" onClick={() => save.mutate()} disabled={checking || save.isPending}>
        {save.isPending ? 'Saving…' : 'Save to collection'}
      </button>
      {save.isError && <p className="error-text" role="alert">{save.error.message}</p>}
    </div>
  );
}
