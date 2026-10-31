import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarPrincipioActivo,
  cambiarEstadoPrincipioActivo,
  crearPrincipioActivo,
  fetchPrincipiosActivos,
  principiosActivosQuery
} from './principios-activos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sample = {
  id: 'pa-1',
  codigoFuente: 'PA-001',
  denominacion: 'Paracetamol',
  nombreNormalizado: 'paracetamol',
  fuente: 'DIGEMID',
  estado: 'ACTIVO'
};

describe('principios-activos.api', () => {
  it('fetchPrincipiosActivos consulta sin parametros por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/principios-activos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sample]);
      })
    );

    const result = await fetchPrincipiosActivos(client);

    expect(receivedUrl?.search).toBe('');
    expect(result).toEqual([sample]);
  });

  it('fetchPrincipiosActivos envia texto y estado', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/principios-activos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sample]);
      })
    );

    await fetchPrincipiosActivos(client, { texto: 'para', estado: 'ACTIVO' });

    expect(receivedUrl?.searchParams.get('texto')).toBe('para');
    expect(receivedUrl?.searchParams.get('estado')).toBe('ACTIVO');
  });

  it('principiosActivosQuery construye la clave con los filtros', () => {
    expect(principiosActivosQuery().queryKey).toEqual(['catalogo', 'principios-activos', '', '']);
    expect(principiosActivosQuery({ texto: 'a', estado: 'ACTIVO' }).queryKey).toEqual([
      'catalogo',
      'principios-activos',
      'a',
      'ACTIVO'
    ]);
  });

  it('crearPrincipioActivo envia el payload', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/principios-activos', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sample, { status: 201 });
      })
    );

    const result = await crearPrincipioActivo(client, { denominacion: 'Paracetamol' });

    expect(body).toEqual({ denominacion: 'Paracetamol' });
    expect(result).toEqual(sample);
  });

  it('actualizarPrincipioActivo envia PUT con el payload', async () => {
    let body: unknown;
    server.use(
      http.put('http://localhost/api/v1/catalogo/principios-activos/pa-1', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sample);
      })
    );

    await actualizarPrincipioActivo(client, 'pa-1', { denominacion: 'Paracetamol 500' });

    expect(body).toEqual({ denominacion: 'Paracetamol 500' });
  });

  it('cambiarEstadoPrincipioActivo envia PATCH con el estado', async () => {
    let body: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/catalogo/principios-activos/pa-1/estado',
        async ({ request }) => {
          body = await request.json();
          return new HttpResponse(null, { status: 204 });
        }
      )
    );

    await cambiarEstadoPrincipioActivo(client, 'pa-1', 'INACTIVO');

    expect(body).toEqual({ status: 'INACTIVO' });
  });
});
