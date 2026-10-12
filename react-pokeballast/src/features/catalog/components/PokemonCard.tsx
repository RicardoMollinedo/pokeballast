import { Link } from 'react-router';
import type { PokemonSummary } from '../../../api/types';
import { TypeBadge } from '../../../shared/components/TypeBadge';
import { formatId, formatName } from '../../../shared/utils/format';
import styles from './PokemonCard.module.css';

/** US01 card: sprite, category, mass and skills (abilities). */
export function PokemonCard({ pokemon }: { pokemon: PokemonSummary }) {
  return (
    <article className={`card ${styles.card}`}>
      <Link to={`/pokemon/${pokemon.name}`} className={styles.link} aria-label={`View ${formatName(pokemon.name)}`}>
        <div className={styles.sprite}>
          {pokemon.spriteUrl ? (
            <img src={pokemon.spriteUrl} alt={formatName(pokemon.name)} width={96} height={96} loading="lazy" />
          ) : (
            <span className={styles.noImage} aria-hidden="true">?</span>
          )}
        </div>
        <p className="muted">{formatId(pokemon.id)}</p>
        <h2 className={styles.title}>{formatName(pokemon.name)}</h2>
      </Link>
      <p className={styles.category}>{pokemon.category ?? 'Unknown category'}</p>
      <div className="badges">
        {pokemon.types.map((type) => <TypeBadge key={type} type={type} />)}
      </div>
      <dl className="facts">
        <div>
          <dt>Mass</dt>
          <dd>{pokemon.weightKg} kg</dd>
        </div>
        <div>
          <dt>Skills</dt>
          <dd>{pokemon.abilities.map(formatName).join(', ')}</dd>
        </div>
      </dl>
    </article>
  );
}
