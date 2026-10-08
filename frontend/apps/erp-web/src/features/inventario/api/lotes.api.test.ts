import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleLote } from '../../../test/inventario-fixtures';
import { bloquearLote, desbloquearLote, fetchLote, loteQuery } from './lotes.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('lotes.api', () => {
  it('fetchLote consulta el lote por id', async () => {
    server.use(
      http.get('http://localhost/api/v1/inventario/lotes/lote-1', () =>
        HttpResponse.json(sampleLote)
      )
    );

    expect(await fetchLote(client, 'lote-1')).toEqual(sampleLote);
  });

  it('loteQuery usa una clave por lote y consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(sampleLote);
    const options = loteQuery('lote-1');

    const result = await new QueryClient().fetchQuery(options);

    expect(options.queryKey).toEqual(['inventario', 'lotes', 'detalle', 'lote-1']);
    expect(get).toHaveBeenCalledWith('/inventario/lotes/lote-1');
    expect(result).toEqual(sampleLote);
  });

  it('bloquearLote envía el motivo por POST', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/inventario/lotes/lote-1/bloqueos', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleLote, estado: 'BLOQUEADO' });
      })
    );

    const lote = await bloquearLote(client, 'lote-1', { motivo: 'Control de calidad' });

    expect(body).toEqual({ motivo: 'Control de calidad' });
    expect(lote.estado).toBe('BLOQUEADO');
  });

  it('desbloquearLote usa DELETE sobre los bloqueos del lote', async () => {
    server.use(
      http.delete('http://localhost/api/v1/inventario/lotes/lote-1/bloqueos', () =>
        HttpResponse.json(sampleLote)
      )
    );

    expect(await desbloquearLote(client, 'lote-1')).toEqual(sampleLote);
  });
});
