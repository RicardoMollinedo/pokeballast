import { useMutation } from '@tanstack/react-query';
import { Link, useLocation, useNavigate } from 'react-router';
import { authApi } from '../../../api/authApi';
import { useAuth } from '../../../auth/useAuth';
import { CredentialsForm } from '../components/CredentialsForm';

interface LoginState {
  from?: string;
  registered?: boolean;
}

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const state = (useLocation().state as LoginState | null) ?? {};
  const login = useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) => authApi.login(username, password),
    onSuccess: (response) => {
      signIn(response);
      navigate(state.from ?? '/collection', { replace: true });
    },
  });

  return (
    <section className="narrow">
      <h1>Sign in</h1>
      {state.registered && <p className="success-text" role="status">Account created. You can sign in now.</p>}
      <CredentialsForm submitLabel="Sign in" submitting={login.isPending}
        onSubmit={(username, password) => login.mutate({ username, password })} />
      {login.isError && <p className="error-text" role="alert">{login.error.message}</p>}
      <p className="muted">
        No account? <Link to="/register">Create one</Link>. Demo users: <code>ash / Pikachu123</code> (trainer),{' '}
        <code>admin / Admin1234</code> (admin).
      </p>
    </section>
  );
}
