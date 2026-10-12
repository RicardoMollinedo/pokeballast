import { ErrorCode } from './errorCodes';
import type { BallastResponse } from './types';

const BASE_URL = '/api/v1';

/** Error carrying the backend's response code, message and optional data (field errors for 1000/1004/1007). */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly data: unknown;

  constructor(status: number, code: string, message: string, data: unknown = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.data = data;
  }

  get fieldErrors(): Record<string, string> {
    return this.data !== null && typeof this.data === 'object' ? (this.data as Record<string, string>) : {};
  }
}

export interface Session {
  token: string;
  username: string;
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  session?: Session | null;
  signal?: AbortSignal;
}

let onUnauthorized: (() => void) | null = null;

/** Lets the auth layer react to an expired or invalid token without api/ depending on React. */
export function setUnauthorizedListener(listener: (() => void) | null): void {
  onUnauthorized = listener;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json', 'x-channel': 'WEB' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (options.session) {
    headers.Authorization = `Bearer ${options.session.token}`;
    headers['x-user'] = options.session.username; // audit only: the backend authorizes from the token
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError(0, ErrorCode.NETWORK, 'Cannot reach the server. Check your connection and try again.');
  }

  if (response.status === 401 && options.session) onUnauthorized?.();
  if (response.status === 204) return undefined as T;

  let envelope: BallastResponse<T>;
  try {
    envelope = (await response.json()) as BallastResponse<T>;
  } catch {
    throw new ApiError(response.status, ErrorCode.UNEXPECTED, 'Unexpected response from the server.');
  }
  if (response.ok && envelope.responseCode === ErrorCode.SUCCESS) {
    return envelope.response as T;
  }
  throw new ApiError(response.status, envelope.responseCode, envelope.errorDetail ?? 'Unexpected error.',
    envelope.response);
}
