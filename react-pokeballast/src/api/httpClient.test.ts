import { http } from 'msw';
import { server } from '../test/msw/server';
import { fail, noContent, ok } from '../test/msw/envelope';
import { ApiError, request, setUnauthorizedListener } from './httpClient';

describe('httpClient', () => {
  it('unwraps the BallastResponse and sends the auth and audit headers', async () => {
    let headers: Headers | undefined;
    server.use(http.get('/api/v1/thing', ({ request: req }) => {
      headers = req.headers;
      return ok({ id: 1 });
    }));

    await expect(request('/thing', { session: { token: 'abc', username: 'ash' } })).resolves.toEqual({ id: 1 });
    expect(headers?.get('Authorization')).toBe('Bearer abc');
    expect(headers?.get('x-user')).toBe('ash');
    expect(headers?.get('x-channel')).toBe('WEB');
  });

  it('does not send auth headers without a session', async () => {
    let headers: Headers | undefined;
    server.use(http.get('/api/v1/thing', ({ request: req }) => {
      headers = req.headers;
      return ok(null);
    }));

    await request('/thing');
    expect(headers?.get('Authorization')).toBeNull();
    expect(headers?.get('x-user')).toBeNull();
  });

  it('turns an error envelope into an ApiError with code, message and field errors', async () => {
    server.use(http.put('/api/v1/thing', () =>
      fail(400, '1004', 'Invalid proprietary fields', { tags: 'at most 10 tags are allowed' })));

    const error = await request('/thing', { method: 'PUT', body: {} }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 400, code: '1004', message: 'Invalid proprietary fields' });
    expect((error as ApiError).fieldErrors).toEqual({ tags: 'at most 10 tags are allowed' });
  });

  it('resolves 204 responses to undefined', async () => {
    server.use(http.delete('/api/v1/thing', () => noContent()));

    await expect(request('/thing', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('reports network failures with a friendly message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await expect(request('/thing')).rejects.toMatchObject({ status: 0, code: 'NETWORK' });
    vi.unstubAllGlobals();
  });

  it('notifies the auth layer when an authenticated call gets 401', async () => {
    const listener = vi.fn();
    setUnauthorizedListener(listener);
    server.use(http.get('/api/v1/thing', () => fail(401, '4001', 'Authentication required')));

    await expect(request('/thing', { session: { token: 'old', username: 'ash' } })).rejects.toMatchObject({ code: '4001' });
    expect(listener).toHaveBeenCalledOnce();
    setUnauthorizedListener(null);
  });
});
