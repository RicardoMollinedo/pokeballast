import type { AuthResponse, LocalPokemon, Page, PokemonDetail, PokemonSummary } from '../api/types';

export const inOneHour = () => new Date(Date.now() + 3_600_000).toISOString();

export const trainerSession: AuthResponse = {
  accessToken: 'trainer-token',
  tokenType: 'Bearer',
  expiresAt: inOneHour(),
  user: { username: 'ash', role: 'TRAINER' },
};

export const adminSession: AuthResponse = {
  ...trainerSession,
  accessToken: 'admin-token',
  user: { username: 'admin', role: 'ADMIN' },
};

export const page = <T,>(content: T[], overrides: Partial<Page<T>> = {}): Page<T> => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: content.length === 0 ? 0 : 1,
  ...overrides,
});

export const bulbasaur: PokemonSummary = {
  id: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://img.test/1.png',
  category: 'Seed Pokémon',
  types: ['grass', 'poison'],
  abilities: ['overgrow', 'chlorophyll'],
  heightM: 0.7,
  weightKg: 6.9,
};

export const pikachuDetail: PokemonDetail = {
  id: 25,
  name: 'pikachu',
  imageUrl: 'https://img.test/25-art.png',
  spriteUrl: 'https://img.test/25.png',
  category: 'Mouse Pokémon',
  description: 'It keeps its tail raised to monitor its surroundings.',
  types: ['electric'],
  abilities: ['static', 'lightning-rod'],
  heightM: 0.4,
  weightKg: 6,
  stats: [
    { name: 'hp', value: 35 },
    { name: 'speed', value: 90 },
  ],
  evolutionChain: {
    id: 172,
    name: 'pichu',
    imageUrl: null,
    evolvesTo: [{ id: 25, name: 'pikachu', imageUrl: null, evolvesTo: [{ id: 26, name: 'raichu', imageUrl: null, evolvesTo: [] }] }],
  },
};

export const localPikachu: LocalPokemon = {
  id: 4,
  pokeApiId: 25,
  name: 'pikachu',
  spriteUrl: null,
  category: 'Mouse Pokémon',
  types: ['electric'],
  abilities: ['static'],
  heightM: 0.4,
  weightKg: 6,
  localizedName: 'ピカチュウ',
  region: 'Kanto',
  tags: ['mascot'],
  version: 2,
  syncedAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};
