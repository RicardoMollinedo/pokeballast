import { HttpResponse } from 'msw';

/** Real BallastResponse payloads, exactly as the API sends them. */
export const ok = <T,>(response: T, init?: ResponseInit) =>
  HttpResponse.json({ responseCode: '0000', response, errorDetail: null }, init);

export const fail = (status: number, responseCode: string, errorDetail: string, response: unknown = null) =>
  HttpResponse.json({ responseCode, response, errorDetail }, { status });

export const noContent = () => new HttpResponse(null, { status: 204 });
