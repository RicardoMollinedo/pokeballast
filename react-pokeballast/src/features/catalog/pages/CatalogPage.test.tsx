import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
import { bulbasaur, page } from '../../../test/fixtures';
import { fail, ok } from '../../../test/msw/envelope';
import { server } from '../../../test/msw/server';
import { renderWithProviders } from '../../../test/utils';
import { CatalogPage } from './CatalogPage';

describe('CatalogPage (US01)', () => {
  beforeEach(() => {
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
  });

  it('shows each pokemon with sprite, category, mass and skills', async () => {
    server.use(http.get('/api/v1/pokemon', () => ok(page([bulbasaur], { totalElements: 1351, totalPages: 68 }))));

    renderWithProviders(<CatalogPage />);

    const card = (await screen.findByRole('heading', { name: 'Bulbasaur' })).closest('article')!;
    expect(within(card).getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute('src', bulbasaur.spriteUrl);
    expect(within(card).getByText('Seed Pokémon')).toBeInTheDocument();
    expect(within(card).getByText('6.9 kg')).toBeInTheDocument();
    expect(within(card).getByText('Overgrow, Chlorophyll')).toBeInTheDocument();
    expect(within(card).getByText('#001')).toBeInTheDocument();
    expect(screen.getByText('1,351 Pokémon from PokeAPI')).toBeInTheDocument();
  });

  it('requests the next page and keeps it in the URL', async () => {
    const requestedPages: (string | null)[] = [];
    server.use(http.get('/api/v1/pokemon', ({ request }) => {
      const current = new URL(request.url).searchParams.get('page');
      requestedPages.push(current);
      return ok(page([bulbasaur], { page: Number(current), totalElements: 1351, totalPages: 68 }));
    }));
    renderWithProviders(<CatalogPage />);

    await userEvent.click(await screen.findByRole('button', { name: /next/i }));

    expect(await screen.findByText('Page 2 of 68')).toBeInTheDocument();
    expect(requestedPages).toContain('1');
    expect(screen.getByTestId('location')).toHaveTextContent('/?page=1');
  });

  it('reads the page from the URL', async () => {
    server.use(http.get('/api/v1/pokemon', ({ request }) =>
      ok(page([bulbasaur], { page: Number(new URL(request.url).searchParams.get('page')), totalPages: 68 }))));

    renderWithProviders(<CatalogPage />, { route: '/?page=4' });

    expect(await screen.findByText('Page 5 of 68')).toBeInTheDocument();
  });

  it('explains when PokeAPI is down and lets the user retry', async () => {
    server.use(http.get('/api/v1/pokemon', () => fail(503, '5001', 'PokeAPI is temporarily unavailable, please try again later')));

    renderWithProviders(<CatalogPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('PokeAPI is temporarily unavailable');
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });
});
