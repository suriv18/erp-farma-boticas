import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import {
  actualizarTerminal,
  crearTerminal,
  fetchTerminales,
  terminalesQuery
} from './terminales.api';
import type { Terminal } from './terminales.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleTerminal: Terminal = {
  id: 'term-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'POS001',
  nombre: 'Caja 1',
  serieBoletaDefecto: null,
  serieFacturaDefecto: null,
  numeroSerieEquipo: null,
  hostname: null,
  ipEquipo: null,
  impresoraCodigo: null,
  storeEdgeHabilitado: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

describe('terminales.api', () => {
  it('fetchTerminales filtra por establecimiento y usa paginación por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/terminales-pos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleTerminal], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchTerminales(client, {
      establecimientoId: 'est-1'
    });

    expect(receivedUrl?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleTerminal]);
  });

  it('fetchTerminales omite el establecimiento y envía search, page y size si se indican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/terminales-pos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 1, size: 100, totalElements: 0 });
      })
    );

    await fetchTerminales(client, { search: 'caj', page: 1, size: 100 });

    expect(receivedUrl?.searchParams.has('establecimientoId')).toBe(false);
    expect(receivedUrl?.searchParams.get('search')).toBe('caj');
  });

  it('crearTerminal envía el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/terminales-pos', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleTerminal, { status: 201 });
      })
    );
    const payload = {
      establecimientoId: 'est-1',
      codigo: 'POS001',
      nombre: 'Caja 1',
      storeEdgeHabilitado: false
    };

    await crearTerminal(client, payload);

    expect(receivedBody).toEqual(payload);
  });

  it('actualizarTerminal usa PUT sin tenantId e incluye estado', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put(
        'http://localhost/api/v1/organizacion/terminales-pos/term-1',
        async ({ request }) => {
          receivedUrl = new URL(request.url);
          receivedBody = await request.json();
          return HttpResponse.json({ ...sampleTerminal, estado: 'BLOQUEADO' });
        }
      )
    );
    const payload = { nombre: 'Caja 1', storeEdgeHabilitado: false, estado: 'BLOQUEADO' as const };

    const result = await actualizarTerminal(client, 'term-1', payload);

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(receivedBody).toEqual(payload);
    expect(result.estado).toBe('BLOQUEADO');
  });

  it('terminalesQuery define claves estables por parámetros', () => {
    expect(
      terminalesQuery({
        establecimientoId: 'est-1',
        search: 'c',
        page: 2,
        size: 10
      }).queryKey
    ).toEqual(['organizacion', 'terminales', 'lista', 'est-1', 'c', 2, 10]);
    expect(terminalesQuery({}).queryKey).toEqual([
      'organizacion',
      'terminales',
      'lista',
      '',
      '',
      0,
      20
    ]);
  });

  it('terminalesQuery ejecuta la consulta contra /organizacion/terminales-pos?establecimientoId=est-1&page=0&size=20', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0 });

    await new QueryClient().fetchQuery(terminalesQuery({ establecimientoId: 'est-1' }));

    expect(get).toHaveBeenCalledWith(
      '/organizacion/terminales-pos?establecimientoId=est-1&page=0&size=20'
    );
  });

  it('crearTerminal rechaza con ApiError 409 cuando el servidor informa un conflicto', async () => {
    server.use(
      http.post('http://localhost/api/v1/organizacion/terminales-pos', () =>
        HttpResponse.json({ title: 'Conflict', detail: 'Codigo duplicado' }, { status: 409 })
      )
    );

    const result = crearTerminal(client, {
      establecimientoId: 'est-1',
      codigo: 'POS001',
      nombre: 'Caja 1',
      storeEdgeHabilitado: false
    });

    await expect(result).rejects.toBeInstanceOf(ApiError);
    await expect(result).rejects.toMatchObject({ status: 409, message: 'Codigo duplicado' });
  });
});
