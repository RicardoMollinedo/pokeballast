import { setupServer } from 'msw/node';

/** No default handlers: every test declares the API calls it expects (unhandled requests fail the test). */
export const server = setupServer();
