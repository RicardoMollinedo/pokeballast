import { useState, type FormEvent } from 'react';

interface CredentialsFormProps {
  submitLabel: string;
  submitting: boolean;
  fieldErrors?: Record<string, string>;
  passwordHint?: string;
  newPassword?: boolean;
  onSubmit: (username: string, password: string) => void;
}

export function CredentialsForm({ submitLabel, submitting, fieldErrors = {}, passwordHint, newPassword = false,
  onSubmit }: CredentialsFormProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit(username.trim(), password);
  };

  return (
    <form className="card form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="username">Username</label>
        <input id="username" autoComplete="username" value={username} required
          onChange={(event) => setUsername(event.target.value)} aria-invalid={Boolean(fieldErrors.username)}
          aria-describedby={fieldErrors.username ? 'username-error' : undefined} />
        {fieldErrors.username && <p id="username-error" className="error-text">{fieldErrors.username}</p>}
      </div>
      <div className="field">
        <label htmlFor="password">Password</label>
        <input id="password" type="password" autoComplete={newPassword ? 'new-password' : 'current-password'}
          value={password} required onChange={(event) => setPassword(event.target.value)}
          aria-invalid={Boolean(fieldErrors.password)}
          aria-describedby={fieldErrors.password ? 'password-error' : passwordHint ? 'password-hint' : undefined} />
        {passwordHint && <p id="password-hint" className="hint">{passwordHint}</p>}
        {fieldErrors.password && <p id="password-error" className="error-text">{fieldErrors.password}</p>}
      </div>
      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? 'Please wait…' : submitLabel}
      </button>
    </form>
  );
}
