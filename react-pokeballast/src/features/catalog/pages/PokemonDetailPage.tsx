import { Link, useParams } from 'react-router';
import { ErrorState, Loading } from '../../../shared/components/Status';
import { TypeBadge } from '../../../shared/components/TypeBadge';
import { formatId, formatName } from '../../../shared/utils/format';
import styles from '../components/Detail.module.css';
import { EvolutionTree } from '../components/EvolutionTree';
import { SaveToCollection } from '../components/SaveToCollection';
import { StatBar } from '../components/StatBar';
import { usePokemonDetail } from '../hooks/useCatalog';

/** US02: image, core statistics, description and evolution lineage. */
export function PokemonDetailPage() {
  const { idOrName = '' } = useParams();
  const { data: pokemon, isPending, isError, error } = usePokemonDetail(idOrName);

  if (isPending) return <Loading label="Loading Pokémon…" />;
  if (isError) return <ErrorState error={error} action={<Link to="/">Back to the list</Link>} />;

  return (
    <article className={styles.detail}>
      <Link to="/" className="back-link">← Back</Link>
      <div className={styles.top}>
        <div className={`card ${styles.hero}`}>
          {pokemon.imageUrl && <img src={pokemon.imageUrl} alt={formatName(pokemon.name)} width={280} height={280} />}
        </div>
        <div className={styles.info}>
          <p className="muted">{formatId(pokemon.id)}</p>
          <h1>{formatName(pokemon.name)}</h1>
          {pokemon.category && <p className={styles.category}>{pokemon.category}</p>}
          <div className="badges">
            {pokemon.types.map((type) => <TypeBadge key={type} type={type} />)}
          </div>
          {pokemon.description && <p className={styles.description}>{pokemon.description}</p>}
          <dl className={styles.inlineFacts}>
            <div>
              <dt>Height</dt>
              <dd>{pokemon.heightM} m</dd>
            </div>
            <div>
              <dt>Mass</dt>
              <dd>{pokemon.weightKg} kg</dd>
            </div>
            <div>
              <dt>Skills</dt>
              <dd>{pokemon.abilities.map(formatName).join(', ')}</dd>
            </div>
          </dl>
          <SaveToCollection pokeApiId={pokemon.id} name={pokemon.name} />
        </div>
      </div>

      <section className="card" aria-labelledby="stats-title">
        <h2 id="stats-title">Base stats</h2>
        {pokemon.stats.map((stat) => <StatBar key={stat.name} stat={stat} />)}
      </section>

      {pokemon.evolutionChain && (
        <section className="card" aria-labelledby="evolution-title">
          <h2 id="evolution-title">Evolution</h2>
          <div className={styles.evolutionScroll}>
            <EvolutionTree node={pokemon.evolutionChain} current={pokemon.name} />
          </div>
        </section>
      )}
    </article>
  );
}
