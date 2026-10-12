import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { localPikachu, page, pikachuDetail, trainerSession } from '../../../test/fixtures';
import { fail, ok } from '../../../test/msw/envelope';
import { server } from '../../../test/msw/server';
import { renderWithProviders } from '../../../test/utils';
import { PokemonDetailPage } from './PokemonDetailPage';

const renderDetail = (session?: typeof trainerSession) =>
  renderWithProviders(<PokemonDetailPage />, { route: '/pokemon/pikachu', path: '/pokemon/:idOrName', session });

describe('PokemonDetailPage (US02)', () => {
  it('shows image, stats, description and the evolution tree', async () => {
    server.use(http.get('/api/v1/pokemon/pikachu', () => ok(pikachuDetail)));

    renderDetail();

    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Pikachu' })).toHaveAttribute('src', pikachuDetail.imageUrl);
    expect(screen.getByText(pikachuDetail.description!)).toBeInTheDocument();
    expect(screen.getByRole('meter', { name: 'Speed' })).toHaveAttribute('aria-valuenow', '90');
    expect(screen.getByRole('link', { name: 'Pichu' })).toHaveAttribute('href', '/pokemon/pichu');
    expect(screen.getByRole('link', { name: 'Raichu' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Pikachu' })).toHaveAttribute('aria-current', 'page');
  });

  it('asks anonymous users to sign in before saving', async () => {
    server.use(http.get('/api/v1/pokemon/pikachu', () => ok(pikachuDetail)));

    renderDetail();

    expect(await screen.findByRole('link', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /save to collection/i })).not.toBeInTheDocument();
  });

  it('lets a trainer save it to the collection (US03)', async () => {
    let importedBody: unknown;
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => ok(pikachuDetail)),
      http.get('/api/v1/local-pokemon', () => ok(page([]))),
      http.post('/api/v1/local-pokemon', async ({ request }) => {
        importedBody = await request.json();
        return ok(localPikachu, { status: 201 });
      }),
    );
    renderDetail(trainerSession);

    await userEvent.click(await screen.findByRole('button', { name: /save to collection/i }));

    expect(await screen.findByText(/saved to the collection/i)).toBeInTheDocument();
    expect(importedBody).toEqual({ idOrName: '25' });
  });

  it('says it is already in the collection without trying to import it', async () => {
    let posted = false;
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => ok(pikachuDetail)),
      http.get('/api/v1/local-pokemon', () => ok(page([localPikachu]))),
      http.post('/api/v1/local-pokemon', () => {
        posted = true;
        return ok(localPikachu, { status: 201 });
      }),
    );
    renderDetail(trainerSession);

    expect(await screen.findByText(/already in the collection/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /save to collection/i })).not.toBeInTheDocument();
    expect(posted).toBe(false);
  });

  it('handles a concurrent import (3001) gracefully', async () => {
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => ok(pikachuDetail)),
      http.get('/api/v1/local-pokemon', () => ok(page([]))),
      http.post('/api/v1/local-pokemon', () => fail(409, '3001', 'Pokemon already stored locally')),
    );
    renderDetail(trainerSession);

    await userEvent.click(await screen.findByRole('button', { name: /save to collection/i }));

    expect(await screen.findByText(/already in the collection/i)).toBeInTheDocument();
  });

  it('shows a not-found state for unknown pokemon', async () => {
    server.use(http.get('/api/v1/pokemon/pikachu', () => fail(404, '2001', 'Pokemon not found')));

    renderDetail();

    expect(await screen.findByRole('alert')).toHaveTextContent('Pokemon not found');
    expect(screen.getByRole('link', { name: /back to the list/i })).toBeInTheDocument();
  });
});
