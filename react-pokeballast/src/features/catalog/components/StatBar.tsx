import type { Stat } from '../../../api/types';
import { formatName } from '../../../shared/utils/format';
import styles from './Detail.module.css';

const MAX_BASE_STAT = 255;

export function StatBar({ stat }: { stat: Stat }) {
  return (
    <div className={styles.stat}>
      <span>{formatName(stat.name)}</span>
      <span className={styles.statValue}>{stat.value}</span>
      <div className={styles.statTrack} role="meter" aria-label={formatName(stat.name)}
        aria-valuemin={0} aria-valuemax={MAX_BASE_STAT} aria-valuenow={stat.value}>
        <div className={styles.statFill} style={{ width: `${Math.round((stat.value / MAX_BASE_STAT) * 100)}%` }} />
      </div>
    </div>
  );
}
