export function TypeBadge({ type }: { type: string }) {
  return <span className={`badge type-${type}`}>{type}</span>;
}
