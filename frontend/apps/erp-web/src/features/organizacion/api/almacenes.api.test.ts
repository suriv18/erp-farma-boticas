import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import { actualizarAlmacen, almacenesQuery, crearAlmacen, fetchAlmacenes } from './almacenes.api';
import type { Almacen } from './almacenes.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleAlmacen: Almacen = {
  id: 'alm-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'ALM001',
  nombre: 'Almacén Central',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: true,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: null,
  temperaturaMaxC: null,
  activo: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

const datos = {
  nombre: 'Almacén Central',
  tipo: 'GENERAL' as const,
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: true,
  permiteDespacho: true,
  controlTemperatura: false
};

describe('almacenes.api', () => {
  it('fetchAlmacenes filtra por establecimiento y usa paginación por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/almacenes', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleAlmacen], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchAlmacenes(client, {
      tenantId: 'tenant-1',
      establecimientoId: 'est-1'
    });

    expect(receivedUrl?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleAlmacen]);
  });

  it('fetchAlmacenes omite el establecimiento y envía search, page y size si se indican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/almacenes', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 3, size: 100, totalElements: 0 });
      })
    );

    await fetchAlmacenes(client, { tenantId: 'tenant-1', search: 'alm', page: 3, size: 100 });

    expect(receivedUrl?.searchParams.has('establecimientoId')).toBe(false);
    expect(receivedUrl?.searchParams.get('search')).toBe('alm');
    expect(receivedUrl?.searchParams.get('page')).toBe('3');
  });

  it('crearAlmacen envía el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/almacenes', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleAlmacen, { status: 201 });
      })
    );
    const payload = {
      ...datos,
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'ALM001'
    };

    await crearAlmacen(client, payload);

    expect(receivedBody).toEqual(payload);
  });

  it('actualizarAlmacen usa PUT con tenantId como query e incluye activo', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/organizacion/almacenes/alm-1', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleAlmacen, activo: false });
      })
    );

    const result = await actualizarAlmacen(client, 'alm-1', 'tenant-1', {
      ...datos,
      activo: false
    });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ ...datos, activo: false });
    expect(result.activo).toBe(false);
  });

  it('almacenesQuery define claves estables por parámetros', () => {
    expect(
      almacenesQuery({
        tenantId: 'tenant-1',
        establecimientoId: 'est-1',
        search: 'a',
        page: 2,
        size: 10
      }).queryKey
    ).toEqual(['organizacion', 'almacenes', 'lista', 'tenant-1', 'est-1', 'a', 2, 10]);
    expect(almacenesQuery({ tenantId: 'tenant-1' }).queryKey).toEqual([
      'organizacion',
      'almacenes',
      'lista',
      'tenant-1',
      '',
      '',
      0,
      20
    ]);
  });

  it('almacenesQuery ejecuta la consulta contra /organizacion/almacenes?tenantId=tenant-1&establecimientoId=est-1&page=0&size=20', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0 });

    await new QueryClient().fetchQuery(
      almacenesQuery({ tenantId: 'tenant-1', establecimientoId: 'est-1' })
    );

    expect(get).toHaveBeenCalledWith(
      '/organizacion/almacenes?tenantId=tenant-1&establecimientoId=est-1&page=0&size=20'
    );
  });

  it('crearAlmacen rechaza con ApiError 409 cuando el servidor informa un conflicto', async () => {
    server.use(
      http.post('http://localhost/api/v1/organizacion/almacenes', () =>
        HttpResponse.json({ title: 'Conflict', detail: 'Codigo duplicado' }, { status: 409 })
      )
    );

    const result = crearAlmacen(client, {
      ...datos,
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'ALM001'
    });

    await expect(result).rejects.toBeInstanceOf(ApiError);
    await expect(result).rejects.toMatchObject({ status: 409, message: 'Codigo duplicado' });
  });
});
