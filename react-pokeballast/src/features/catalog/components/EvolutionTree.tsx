import { Link } from 'react-router';
import type { EvolutionNode } from '../../../api/types';
import { formatName } from '../../../shared/utils/format';
import styles from './Detail.module.css';

/** Recursive: lineages branch (Eevee has eight evolutions). */
export function EvolutionTree({ node, current }: { node: EvolutionNode; current: string }) {
  const isCurrent = node.name === current;
  return (
    <div className={styles.evolutionNode}>
      <Link to={`/pokemon/${node.name}`} className={`${styles.evolutionLink} ${isCurrent ? styles.current : ''}`}
        aria-current={isCurrent ? 'page' : undefined}>
        {node.imageUrl && <img src={node.imageUrl} alt="" width={72} height={72} loading="lazy" />}
        <span>{formatName(node.name)}</span>
      </Link>
      {node.evolvesTo.length > 0 && (
        <div className={styles.evolutionChildren}>
          {node.evolvesTo.map((child) => (
            <div key={child.id} className={styles.evolutionBranch}>
              <span className={styles.arrow} aria-hidden="true">→</span>
              <EvolutionTree node={child} current={current} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
