// Mirrors pokeballast/src/main/java/.../dto. Nullability follows the @Schema documentation.

export interface BallastResponse<T> {
  responseCode: string;
  response: T | null;
  errorDetail: string | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PokemonSummary {
  id: number;
  name: string;
  spriteUrl: string | null;
  category: string | null;
  types: string[];
  abilities: string[];
  heightM: number;
  weightKg: number;
}

export interface Stat {
  name: string;
  value: number;
}

export interface EvolutionNode {
  id: number;
  name: string;
  imageUrl: string | null;
  evolvesTo: EvolutionNode[];
}

export interface PokemonDetail {
  id: number;
  name: string;
  imageUrl: string | null;
  spriteUrl: string | null;
  category: string | null;
  description: string | null;
  types: string[];
  abilities: string[];
  heightM: number;
  weightKg: number;
  stats: Stat[];
  evolutionChain: EvolutionNode | null;
}

export interface LocalPokemon {
  id: number;
  pokeApiId: number;
  name: string;
  spriteUrl: string | null;
  category: string | null;
  types: string[];
  abilities: string[];
  heightM: number;
  weightKg: number;
  localizedName: string | null;
  region: string | null;
  tags: string[];
  version: number;
  syncedAt: string;
  updatedAt: string;
}

export interface UpdateLocalPokemon {
  localizedName: string | null;
  region: string | null;
  tags: string[];
  version: number;
}

export type Role = 'TRAINER' | 'ADMIN';

export interface User {
  username: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}
