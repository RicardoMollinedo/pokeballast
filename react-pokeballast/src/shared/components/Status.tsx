import type { ReactNode } from 'react';

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="status" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      {label}
    </div>
  );
}

interface ErrorStateProps {
  error?: unknown;
  message?: string;
  onRetry?: () => void;
  action?: ReactNode;
}

export function ErrorState({ error, message, onRetry, action }: ErrorStateProps) {
  const text = message ?? (error instanceof Error ? error.message : 'Something went wrong.');
  return (
    <div className="status status-error" role="alert">
      <p>{text}</p>
      {onRetry && (
        <button type="button" onClick={onRetry}>
          Try again
        </button>
      )}
      {action}
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="status">{children}</div>;
}
