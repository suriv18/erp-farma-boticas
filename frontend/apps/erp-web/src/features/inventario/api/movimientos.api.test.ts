import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { sampleMovimiento } from '../../../test/inventario-fixtures';
import { registrarMovimiento } from './movimientos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('movimientos.api', () => {
  it('registrarMovimiento envía el payload y la cabecera Idempotency-Key', async () => {
    let body: unknown;
    let key: string | null = null;
    server.use(
      http.post('http://localhost/api/v1/inventario/movimientos', async ({ request }) => {
        body = await request.json();
        key = request.headers.get('Idempotency-Key');
        return HttpResponse.json(sampleMovimiento, { status: 201 });
      })
    );
    const payload = {
      almacenId: 'alm-1',
      skuId: 'sku-1',
      tipo: 'AJUSTE_INGRESO' as const,
      cantidad: 5,
      motivo: 'Conteo',
      loteId: 'lote-1'
    };

    const result = await registrarMovimiento(client, payload, 'clave-1');

    expect(body).toEqual(payload);
    expect(key).toBe('clave-1');
    expect(result).toEqual(sampleMovimiento);
  });
});
