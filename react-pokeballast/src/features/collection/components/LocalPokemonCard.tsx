import { useState } from 'react';
import { Link } from 'react-router';
import type { LocalPokemon } from '../../../api/types';
import { useAuth } from '../../../auth/useAuth';
import { TypeBadge } from '../../../shared/components/TypeBadge';
import { formatId, formatName } from '../../../shared/utils/format';
import { useDeletePokemon, useRefreshPokemon } from '../hooks/useCollection';
import styles from './Collection.module.css';

export function LocalPokemonCard({ pokemon }: { pokemon: LocalPokemon }) {
  const { hasRole } = useAuth();
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const refresh = useRefreshPokemon(pokemon.id);
  const remove = useDeletePokemon(pokemon.id);
  const error = refresh.error ?? remove.error;

  return (
    <article className={`card ${styles.card}`}>
      <div className={styles.cardHeader}>
        {pokemon.spriteUrl
          ? <img src={pokemon.spriteUrl} alt="" width={72} height={72} loading="lazy" />
          : <span className={styles.placeholder} aria-hidden="true" />}
        <div>
          <p className="muted">{formatId(pokemon.pokeApiId)} · v{pokemon.version}</p>
          <h2 className={styles.title}>{pokemon.localizedName ?? formatName(pokemon.name)}</h2>
          {pokemon.localizedName && <p className="muted">{formatName(pokemon.name)}</p>}
        </div>
      </div>
      <div className="badges">
        {pokemon.types.map((type) => <TypeBadge key={type} type={type} />)}
      </div>
      <dl className="facts">
        <div>
          <dt>Region</dt>
          <dd>{pokemon.region ?? '—'}</dd>
        </div>
        <div>
          <dt>Mass</dt>
          <dd>{pokemon.weightKg} kg</dd>
        </div>
      </dl>
      {pokemon.tags.length > 0 && (
        <ul className={styles.tags} aria-label="Tags">
          {pokemon.tags.map((tag) => (
            <li key={tag}><Link to={`/collection?tag=${encodeURIComponent(tag)}`}>#{tag}</Link></li>
          ))}
        </ul>
      )}

      {hasRole('TRAINER', 'ADMIN') && (
        <div className={styles.actions}>
          <Link to={`/collection/${pokemon.id}/edit`} className="button">Edit</Link>
          <button type="button" onClick={() => refresh.mutate()} disabled={refresh.isPending}>
            {refresh.isPending ? 'Syncing…' : 'Re-sync'}
          </button>
          {hasRole('ADMIN') && (confirmingDelete ? (
            <>
              <button type="button" className="danger" onClick={() => remove.mutate()} disabled={remove.isPending}>
                Confirm delete
              </button>
              <button type="button" onClick={() => setConfirmingDelete(false)}>Cancel</button>
            </>
          ) : (
            <button type="button" className="danger-outline" onClick={() => setConfirmingDelete(true)}>Delete</button>
          ))}
        </div>
      )}
      {refresh.isSuccess && <p className="success-text" role="status">Synced with PokeAPI.</p>}
      {error && <p className="error-text" role="alert">{error.message}</p>}
    </article>
  );
}
