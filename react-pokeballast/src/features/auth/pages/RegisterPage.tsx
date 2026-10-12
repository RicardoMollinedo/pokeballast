import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { authApi } from '../../../api/authApi';
import { ApiError } from '../../../api/httpClient';
import { CredentialsForm } from '../components/CredentialsForm';

export function RegisterPage() {
  const navigate = useNavigate();
  const register = useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) =>
      authApi.register(username, password),
    onSuccess: () => navigate('/login', { state: { registered: true } }),
  });
  const fieldErrors = register.error instanceof ApiError ? register.error.fieldErrors : {};

  return (
    <section className="narrow">
      <h1>Create account</h1>
      <CredentialsForm submitLabel="Create account" submitting={register.isPending} fieldErrors={fieldErrors} newPassword
        passwordHint="8-72 characters, with at least one letter and one digit."
        onSubmit={(username, password) => register.mutate({ username, password })} />
      {register.isError && Object.keys(fieldErrors).length === 0 && (
        <p className="error-text" role="alert">{register.error.message}</p>
      )}
      <p className="muted">Already registered? <Link to="/login">Sign in</Link></p>
    </section>
  );
}
