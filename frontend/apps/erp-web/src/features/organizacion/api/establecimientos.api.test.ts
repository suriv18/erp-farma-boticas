import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import {
  actualizarEstablecimiento,
  cambiarEstadoEstablecimiento,
  crearEstablecimiento,
  establecimientoQuery,
  establecimientosQuery,
  fetchEstablecimiento,
  fetchEstablecimientos
} from './establecimientos.api';
import type { Establecimiento } from './establecimientos.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleEstablecimiento: Establecimiento = {
  id: 'est-1',
  tenantId: 'tenant-1',
  empresaId: 'empresa-1',
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: null,
  codigoAnexoSunat: '0001',
  codigoDigemid: null,
  direccion: null,
  ubigeo: null,
  referencia: null,
  latitud: null,
  longitud: null,
  telefono: null,
  email: null,
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima',
  estadoOperativo: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

const datos = {
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA' as const,
  codigoAnexoSunat: '0001',
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE' as const,
  zonaHoraria: 'America/Lima'
};

describe('establecimientos.api', () => {
  it('fetchEstablecimientos filtra por empresa y usa paginación por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/establecimientos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({
          items: [sampleEstablecimiento],
          page: 0,
          size: 20,
          totalElements: 1
        });
      })
    );

    const result = await fetchEstablecimientos(client, {
      tenantId: 'tenant-1',
      empresaId: 'empresa-1'
    });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('empresaId')).toBe('empresa-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleEstablecimiento]);
  });

  it('fetchEstablecimientos omite empresaId y envía search, page y size si se indican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/establecimientos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 1, size: 100, totalElements: 0 });
      })
    );

    await fetchEstablecimientos(client, {
      tenantId: 'tenant-1',
      search: 'cen',
      page: 1,
      size: 100
    });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.has('empresaId')).toBe(false);
    expect(receivedUrl?.searchParams.get('search')).toBe('cen');
    expect(receivedUrl?.searchParams.get('size')).toBe('100');
  });

  it('fetchEstablecimiento consulta el detalle con el tenantId como query', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/establecimientos/est-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json(sampleEstablecimiento);
      })
    );

    const result = await fetchEstablecimiento(client, 'tenant-1', 'est-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual(sampleEstablecimiento);
  });

  it('crearEstablecimiento envía el payload completo', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/establecimientos', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleEstablecimiento, { status: 201 });
      })
    );
    const payload = { ...datos, tenantId: 'tenant-1', empresaId: 'empresa-1', codigo: 'EST001' };

    await crearEstablecimiento(client, payload);

    expect(receivedBody).toEqual(payload);
  });

  it('actualizarEstablecimiento usa PUT con tenantId como query', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put(
        'http://localhost/api/v1/organizacion/establecimientos/est-1',
        async ({ request }) => {
          receivedUrl = new URL(request.url);
          receivedBody = await request.json();
          return HttpResponse.json(sampleEstablecimiento);
        }
      )
    );

    await actualizarEstablecimiento(client, 'est-1', 'tenant-1', datos);

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual(datos);
  });

  it('cambiarEstadoEstablecimiento usa PATCH /estado', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/organizacion/establecimientos/est-1/estado',
        async ({ request }) => {
          receivedUrl = new URL(request.url);
          receivedBody = await request.json();
          return HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'CLAUSURADO' });
        }
      )
    );

    const result = await cambiarEstadoEstablecimiento(client, 'est-1', 'tenant-1', 'CLAUSURADO');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ estado: 'CLAUSURADO' });
    expect(result.estadoOperativo).toBe('CLAUSURADO');
  });

  it('las consultas definen claves estables por parámetros', () => {
    expect(
      establecimientosQuery({
        tenantId: 'tenant-1',
        empresaId: 'empresa-1',
        search: 'c',
        page: 1,
        size: 5
      }).queryKey
    ).toEqual(['organizacion', 'establecimientos', 'lista', 'tenant-1', 'empresa-1', 'c', 1, 5]);
    expect(establecimientosQuery({ tenantId: 'tenant-1' }).queryKey).toEqual([
      'organizacion',
      'establecimientos',
      'lista',
      'tenant-1',
      '',
      '',
      0,
      20
    ]);
    expect(establecimientoQuery('tenant-1', 'est-1').queryKey).toEqual([
      'organizacion',
      'establecimientos',
      'detalle',
      'tenant-1',
      'est-1'
    ]);
  });

  it('establecimientosQuery ejecuta la consulta contra /organizacion/establecimientos?tenantId=tenant-1&empresaId=empresa-1&page=0&size=20', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0 });

    await new QueryClient().fetchQuery(
      establecimientosQuery({ tenantId: 'tenant-1', empresaId: 'empresa-1' })
    );

    expect(get).toHaveBeenCalledWith(
      '/organizacion/establecimientos?tenantId=tenant-1&empresaId=empresa-1&page=0&size=20'
    );
  });

  it('establecimientoQuery ejecuta la consulta contra /organizacion/establecimientos/est-1?tenantId=tenant-1', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(sampleEstablecimiento);

    await new QueryClient().fetchQuery(establecimientoQuery('tenant-1', 'est-1'));

    expect(get).toHaveBeenCalledWith('/organizacion/establecimientos/est-1?tenantId=tenant-1');
  });

  it('crearEstablecimiento rechaza con ApiError 409 cuando el servidor informa un conflicto', async () => {
    server.use(
      http.post('http://localhost/api/v1/organizacion/establecimientos', () =>
        HttpResponse.json({ title: 'Conflict', detail: 'Codigo duplicado' }, { status: 409 })
      )
    );

    const result = crearEstablecimiento(client, {
      ...datos,
      tenantId: 'tenant-1',
      empresaId: 'empresa-1',
      codigo: 'EST001'
    });

    await expect(result).rejects.toBeInstanceOf(ApiError);
    await expect(result).rejects.toMatchObject({ status: 409, message: 'Codigo duplicado' });
  });
});
