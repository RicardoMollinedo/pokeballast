import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { SESSION_STORAGE_KEY } from '../../../auth/session';
import { trainerSession } from '../../../test/fixtures';
import { fail, ok } from '../../../test/msw/envelope';
import { server } from '../../../test/msw/server';
import { renderWithProviders } from '../../../test/utils';
import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';

const fillCredentials = async (username: string, password: string) => {
  await userEvent.type(screen.getByLabelText('Username'), username);
  await userEvent.type(screen.getByLabelText('Password'), password);
};

describe('LoginPage (F05)', () => {
  it('stores the session and goes to the collection', async () => {
    server.use(http.post('/api/v1/auth/login', () => ok(trainerSession)));
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    await fillCredentials('ash', 'Pikachu123');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByTestId('location')).toHaveTextContent('/collection');
    expect(JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY)!)).toMatchObject({ token: 'trainer-token' });
  });

  it('shows the error for bad credentials (4002) and keeps the user signed out', async () => {
    server.use(http.post('/api/v1/auth/login', () => fail(401, '4002', 'Invalid username or password')));
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    await fillCredentials('ash', 'nope12345');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password');
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('documents the demo credentials', () => {
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    expect(screen.getByText('ash / Pikachu123')).toBeInTheDocument();
    expect(screen.getByText('admin / Admin1234')).toBeInTheDocument();
  });
});

describe('RegisterPage (F05)', () => {
  it('creates the account and sends the user to sign in', async () => {
    server.use(http.post('/api/v1/auth/register', () => ok({ username: 'misty', role: 'TRAINER' }, { status: 201 })));
    renderWithProviders(<RegisterPage />, { route: '/register', path: '/register' });

    await fillCredentials('Misty', 'Starmie123');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByTestId('location')).toHaveTextContent('/login');
  });

  it('shows the backend field errors (1007)', async () => {
    server.use(http.post('/api/v1/auth/register', () => fail(400, '1007', 'Invalid username or password format',
      { password: 'must contain at least one letter and one digit' })));
    renderWithProviders(<RegisterPage />, { route: '/register', path: '/register' });

    await fillCredentials('misty', 'onlyletters');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('must contain at least one letter and one digit')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toHaveAttribute('aria-invalid', 'true');
  });

  it('explains a taken username (3004)', async () => {
    server.use(http.post('/api/v1/auth/register', () => fail(409, '3004', 'Username is already taken')));
    renderWithProviders(<RegisterPage />, { route: '/register', path: '/register' });

    await fillCredentials('ash', 'Pikachu123');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Username is already taken');
  });
});
