import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { localPikachu, trainerSession } from '../../../test/fixtures';
import { fail, ok } from '../../../test/msw/envelope';
import { server } from '../../../test/msw/server';
import { renderWithProviders } from '../../../test/utils';
import { EditPokemonPage } from './EditPokemonPage';

const renderEdit = () => renderWithProviders(<EditPokemonPage />,
  { route: '/collection/4/edit', path: '/collection/:id/edit', session: trainerSession });

describe('EditPokemonPage (US04)', () => {
  it('sends the proprietary fields with the version it read, then returns to the collection', async () => {
    let body: unknown;
    let auth: string | null = null;
    server.use(
      http.get('/api/v1/local-pokemon/4', () => ok(localPikachu)),
      http.put('/api/v1/local-pokemon/4', async ({ request }) => {
        body = await request.json();
        auth = request.headers.get('Authorization');
        return ok({ ...localPikachu, version: 3 });
      }),
    );
    renderEdit();

    const tags = await screen.findByLabelText(/tags/i);
    await userEvent.clear(tags);
    await userEvent.type(tags, 'Mascot, electric-team');
    await userEvent.clear(screen.getByLabelText('Region'));
    await userEvent.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByTestId('location')).toHaveTextContent('/collection');
    expect(body).toEqual({ localizedName: 'ピカチュウ', region: null, tags: ['mascot', 'electric-team'], version: 2 });
    expect(auth).toBe('Bearer trainer-token');
  });

  it('shows the replicated data as read-only', async () => {
    server.use(http.get('/api/v1/local-pokemon/4', () => ok(localPikachu)));

    renderEdit();

    expect(await screen.findByText(/re-sync refreshes it/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/^name$/i)).not.toBeInTheDocument();
  });

  it('validates on the client before calling the API', async () => {
    let called = false;
    server.use(
      http.get('/api/v1/local-pokemon/4', () => ok(localPikachu)),
      http.put('/api/v1/local-pokemon/4', () => {
        called = true;
        return ok(localPikachu);
      }),
    );
    renderEdit();

    await userEvent.type(await screen.findByLabelText(/tags/i), ', not valid!');
    await userEvent.click(screen.getByRole('button', { name: /save changes/i }));

    const message = await screen.findByText(/e\.g\. kanto-starter/i);
    const input = screen.getByLabelText(/tags/i);
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAttribute('aria-describedby', message.id);
    expect(called).toBe(false);
  });

  it('shows the field errors returned by the backend (1004)', async () => {
    server.use(
      http.get('/api/v1/local-pokemon/4', () => ok(localPikachu)),
      http.put('/api/v1/local-pokemon/4', () =>
        fail(400, '1004', 'Invalid proprietary fields', { region: 'must be at most 60 characters' })),
    );
    renderEdit();

    await userEvent.click(await screen.findByRole('button', { name: /save changes/i }));

    expect(await screen.findByText('must be at most 60 characters')).toBeInTheDocument();
    expect(screen.getByLabelText('Region')).toHaveAttribute('aria-invalid', 'true');
  });

  it('explains a version conflict and reloads the latest values (3002)', async () => {
    let reads = 0;
    server.use(
      http.get('/api/v1/local-pokemon/4', () => {
        reads += 1;
        return ok(reads === 1 ? localPikachu : { ...localPikachu, region: 'Johto', version: 3 });
      }),
      http.put('/api/v1/local-pokemon/4', () =>
        fail(409, '3002', 'The record was modified by someone else, reload and retry')),
    );
    renderEdit();

    await userEvent.click(await screen.findByRole('button', { name: /save changes/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('modified by someone else');
    expect(await screen.findByDisplayValue('Johto')).toBeInTheDocument();
  });

  it('shows not found for a missing record', async () => {
    server.use(http.get('/api/v1/local-pokemon/4', () => fail(404, '2002', 'Local pokemon not found')));

    renderEdit();

    expect(await screen.findByRole('alert')).toHaveTextContent('Local pokemon not found');
  });
});
