import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { fetchPerfil, perfilQuery } from './profile.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const perfil = {
  userId: 'user-1',
  tenantId: 'tenant-1',
  displayName: 'Luis Paredes Soto',
  username: 'crit001.manana1',
  email: 'crit001.manana1@boticas.pe',
  assignments: []
};

describe('profile api', () => {
  it('consulta el perfil del usuario autenticado', async () => {
    server.use(http.get('http://api.test/auth/me', () => HttpResponse.json(perfil)));

    const result = await fetchPerfil(createApiClient({ baseUrl: 'http://api.test' }));

    expect(result).toEqual(perfil);
  });

  it('identifica la consulta por usuario', () => {
    expect(perfilQuery('user-1').queryKey).toEqual(['auth', 'perfil', 'user-1']);
  });
});
