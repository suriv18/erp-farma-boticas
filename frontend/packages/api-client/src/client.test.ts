import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from './client';

const server = setupServer(
  http.get('http://localhost/api/v1/health', () => HttpResponse.json({ status: 'ok' })),
  http.post('http://localhost/api/v1/resources', async ({ request }) =>
    HttpResponse.json(await request.json(), { status: 201 })
  )
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('createApiClient', () => {
  it('returns parsed JSON for a successful response', async () => {
    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await expect(client.get<{ status: string }>('/health')).resolves.toEqual({ status: 'ok' });
  });

  it('sends JSON commands using the versioned base URL', async () => {
    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await expect(client.post('/resources', { name: 'lote' })).resolves.toEqual({ name: 'lote' });
  });
});
