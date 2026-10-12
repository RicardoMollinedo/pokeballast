import { screen } from '@testing-library/react';
import { adminSession, trainerSession } from '../test/fixtures';
import { renderWithProviders } from '../test/utils';
import { RequireAuth } from './RequireAuth';

const route = { route: '/collection/1/edit', path: '/collection/:id/edit' };

describe('RequireAuth', () => {
  it('sends anonymous users to the login page', () => {
    renderWithProviders(<RequireAuth><p>secret</p></RequireAuth>, route);

    expect(screen.queryByText('secret')).not.toBeInTheDocument();
    expect(screen.getByTestId('location')).toHaveTextContent('/login');
  });

  it('renders the page for signed-in users', () => {
    renderWithProviders(<RequireAuth><p>secret</p></RequireAuth>, { ...route, session: trainerSession });

    expect(screen.getByText('secret')).toBeInTheDocument();
  });

  it('ignores an expired stored session', () => {
    renderWithProviders(<RequireAuth><p>secret</p></RequireAuth>,
      { ...route, session: { ...trainerSession, expiresAt: '2000-01-01T00:00:00Z' } });

    expect(screen.getByTestId('location')).toHaveTextContent('/login');
  });

  it('enforces roles when requested', () => {
    renderWithProviders(<RequireAuth roles={['ADMIN']}><p>admin area</p></RequireAuth>, { ...route, session: trainerSession });
    expect(screen.queryByText('admin area')).not.toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/permission/i);
  });

  it('lets the right role in', () => {
    renderWithProviders(<RequireAuth roles={['ADMIN']}><p>admin area</p></RequireAuth>, { ...route, session: adminSession });
    expect(screen.getByText('admin area')).toBeInTheDocument();
  });
});
