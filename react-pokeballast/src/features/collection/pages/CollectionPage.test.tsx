import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { adminSession, localPikachu, page, trainerSession } from '../../../test/fixtures';
import { fail, noContent, ok } from '../../../test/msw/envelope';
import { server } from '../../../test/msw/server';
import { renderWithProviders } from '../../../test/utils';
import { CollectionPage } from './CollectionPage';

const renderCollection = (options: Parameters<typeof renderWithProviders>[1] = {}) =>
  renderWithProviders(<CollectionPage />, { route: '/collection', path: '/collection', ...options });

describe('CollectionPage (US03 / US04)', () => {
  it('lists the stored pokemon with their proprietary fields, publicly', async () => {
    server.use(http.get('/api/v1/local-pokemon', () => ok(page([localPikachu]))));

    renderCollection();

    const card = (await screen.findByRole('heading', { name: 'ピカチュウ' })).closest('article')!;
    expect(within(card).getByText('Kanto')).toBeInTheDocument();
    expect(within(card).getByRole('link', { name: '#mascot' })).toHaveAttribute('href', '/collection?tag=mascot');
    expect(screen.queryByRole('link', { name: 'Edit' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/import from pokeapi/i)).not.toBeInTheDocument();
  });

  it('applies name and tag filters through the URL', async () => {
    const queries: string[] = [];
    server.use(http.get('/api/v1/local-pokemon', ({ request }) => {
      queries.push(new URL(request.url).search);
      return ok(page([localPikachu]));
    }));
    renderCollection();

    await userEvent.type(await screen.findByLabelText(/search by name/i), 'pika');
    await userEvent.type(screen.getByLabelText(/filter by tag/i), 'Mascot');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByTestId('location')).toHaveTextContent('/collection?name=pika&tag=mascot');
    expect(queries.at(-1)).toContain('name=pika');
    expect(queries.at(-1)).toContain('tag=mascot');
  });

  it('shows an empty state when nothing matches', async () => {
    server.use(http.get('/api/v1/local-pokemon', () => ok(page([]))));

    renderCollection({ route: '/collection?tag=nothing' });

    expect(await screen.findByText(/no pokémon match/i)).toBeInTheDocument();
  });

  it('lets a trainer import from PokeAPI (US03)', async () => {
    let body: unknown;
    server.use(
      http.get('/api/v1/local-pokemon', () => ok(page([]))),
      http.post('/api/v1/local-pokemon', async ({ request }) => {
        body = await request.json();
        return ok({ ...localPikachu, name: 'gengar' }, { status: 201 });
      }),
    );
    renderCollection({ session: trainerSession });

    await userEvent.type(await screen.findByLabelText(/import from pokeapi/i), ' Gengar ');
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    expect(await screen.findByText(/gengar was added/i)).toBeInTheDocument();
    expect(body).toEqual({ idOrName: 'gengar' });
  });

  it('shows the backend message when the import fails', async () => {
    server.use(
      http.get('/api/v1/local-pokemon', () => ok(page([]))),
      http.post('/api/v1/local-pokemon', () => fail(404, '2001', 'Pokemon not found')),
    );
    renderCollection({ session: trainerSession });

    await userEvent.type(await screen.findByLabelText(/import from pokeapi/i), 'missingno');
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Pokemon not found');
  });

  it('trainers can edit and re-sync but not delete', async () => {
    let refreshed = false;
    server.use(
      http.get('/api/v1/local-pokemon', () => ok(page([localPikachu]))),
      http.post('/api/v1/local-pokemon/4/refresh', () => {
        refreshed = true;
        return ok(localPikachu);
      }),
    );
    renderCollection({ session: trainerSession });

    expect(await screen.findByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/collection/4/edit');
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Re-sync' }));
    expect(await screen.findByText(/synced with pokeapi/i)).toBeInTheDocument();
    expect(refreshed).toBe(true);
  });

  it('admins delete after an inline confirmation (204)', async () => {
    let deleted = false;
    server.use(
      http.get('/api/v1/local-pokemon', () => ok(page(deleted ? [] : [localPikachu]))),
      http.delete('/api/v1/local-pokemon/4', () => {
        deleted = true;
        return noContent();
      }),
    );
    renderCollection({ session: adminSession });

    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }));
    expect(deleted).toBe(false);
    await userEvent.click(screen.getByRole('button', { name: 'Confirm delete' }));

    expect(await screen.findByText(/no pokémon match/i)).toBeInTheDocument();
    expect(deleted).toBe(true);
  });
});
