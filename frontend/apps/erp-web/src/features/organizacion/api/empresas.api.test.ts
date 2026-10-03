import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import {
  actualizarEmpresa,
  cambiarEstadoEmpresa,
  crearEmpresa,
  empresaQuery,
  empresasQuery,
  fetchEmpresa,
  fetchEmpresas
} from './empresas.api';
import type { Empresa } from './empresas.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleEmpresa: Empresa = {
  id: 'empresa-1',
  tenantId: 'tenant-1',
  ruc: '20123456786',
  razonSocial: 'Boticas SAC',
  nombreComercial: null,
  direccionFiscal: null,
  ubigeoFiscal: null,
  telefono: null,
  email: null,
  sitioWeb: null,
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

describe('empresas.api', () => {
  it('fetchEmpresas consulta con page y size por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/empresas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleEmpresa], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchEmpresas(client, {});

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(receivedUrl?.searchParams.has('search')).toBe(false);
    expect(result.items).toEqual([sampleEmpresa]);
  });

  it('fetchEmpresas envía search, page y size cuando se especifican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/empresas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 2, size: 50, totalElements: 0 });
      })
    );

    await fetchEmpresas(client, { search: 'bot', page: 2, size: 50 });

    expect(receivedUrl?.searchParams.get('search')).toBe('bot');
    expect(receivedUrl?.searchParams.get('page')).toBe('2');
    expect(receivedUrl?.searchParams.get('size')).toBe('50');
  });

  it('fetchEmpresa consulta el detalle sin tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/empresas/empresa-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json(sampleEmpresa);
      })
    );

    const result = await fetchEmpresa(client, 'empresa-1');

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(result).toEqual(sampleEmpresa);
  });

  it('crearEmpresa envía el payload y devuelve la empresa creada', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/empresas', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleEmpresa, { status: 201 });
      })
    );
    const payload = {
      ruc: '20123456786',
      razonSocial: 'Boticas SAC',
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: false
    };

    const result = await crearEmpresa(client, payload);

    expect(receivedBody).toEqual(payload);
    expect(result).toEqual(sampleEmpresa);
  });

  it('actualizarEmpresa usa PUT sin tenantId y el payload como body', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/organizacion/empresas/empresa-1', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return HttpResponse.json(sampleEmpresa);
      })
    );
    const payload = {
      razonSocial: 'Boticas del Perú SAC',
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: true
    };

    await actualizarEmpresa(client, 'empresa-1', payload);

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(receivedBody).toEqual(payload);
  });

  it('cambiarEstadoEmpresa usa PATCH /estado con el estado en el body', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/organizacion/empresas/empresa-1/estado',
        async ({ request }) => {
          receivedUrl = new URL(request.url);
          receivedBody = await request.json();
          return HttpResponse.json({ ...sampleEmpresa, estado: 'SUSPENDIDO' });
        }
      )
    );

    const result = await cambiarEstadoEmpresa(client, 'empresa-1', 'SUSPENDIDO');

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(receivedBody).toEqual({ estado: 'SUSPENDIDO' });
    expect(result.estado).toBe('SUSPENDIDO');
  });

  it('empresasQuery y empresaQuery definen claves estables por parámetros', () => {
    expect(empresasQuery({ search: 'bot', page: 1, size: 10 }).queryKey).toEqual([
      'organizacion',
      'empresas',
      'lista',
      'bot',
      1,
      10
    ]);
    expect(empresasQuery({}).queryKey).toEqual(['organizacion', 'empresas', 'lista', '', 0, 20]);
    expect(empresaQuery('empresa-1').queryKey).toEqual([
      'organizacion',
      'empresas',
      'detalle',
      'empresa-1'
    ]);
  });

  it('empresasQuery ejecuta la consulta contra /organizacion/empresas?page=0&size=20', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0 });

    await new QueryClient().fetchQuery(empresasQuery({}));

    expect(get).toHaveBeenCalledWith('/organizacion/empresas?page=0&size=20');
  });

  it('empresaQuery ejecuta la consulta contra /organizacion/empresas/empresa-1', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(sampleEmpresa);

    await new QueryClient().fetchQuery(empresaQuery('empresa-1'));

    expect(get).toHaveBeenCalledWith('/organizacion/empresas/empresa-1');
  });

  it('crearEmpresa rechaza con ApiError 409 cuando el servidor informa un conflicto', async () => {
    server.use(
      http.post('http://localhost/api/v1/organizacion/empresas', () =>
        HttpResponse.json({ title: 'Conflict', detail: 'Codigo duplicado' }, { status: 409 })
      )
    );

    const result = crearEmpresa(client, {
      ruc: '20123456786',
      razonSocial: 'Boticas SAC',
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: false
    });

    await expect(result).rejects.toBeInstanceOf(ApiError);
    await expect(result).rejects.toMatchObject({ status: 409, message: 'Codigo duplicado' });
  });
});
