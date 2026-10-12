interface PaginationProps {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({ page, totalPages, onChange, disabled = false }: PaginationProps) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" onClick={() => onChange(page - 1)} disabled={disabled || page <= 0}>
        ← Previous
      </button>
      <span aria-current="page">
        Page {page + 1} of {totalPages}
      </span>
      <button type="button" onClick={() => onChange(page + 1)} disabled={disabled || page >= totalPages - 1}>
        Next →
      </button>
    </nav>
  );
}
