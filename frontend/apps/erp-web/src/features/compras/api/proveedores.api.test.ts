import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  actualizarProveedor,
  cambiarEstadoProveedor,
  crearProveedor,
  fetchProveedor,
  fetchProveedores,
  proveedorQuery,
  proveedoresQuery
} from './proveedores.api';
import type { ProveedorPayload } from './proveedores.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const payload: ProveedorPayload = {
  numeroDocumento: '20100070970',
  razonSocial: 'Laboratorios Perú SAC',
  diasCreditoDefault: 30,
  monedaDefault: 'PEN',
  esLaboratorio: true,
  esImportador: false,
  esDistribuidor: true
};

describe('proveedores.api', () => {
  it('fetchProveedores envía estado, texto, página y tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/proveedores', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleProveedor]));
      })
    );

    const result = await fetchProveedores(client, {
      estado: 'ACTIVO',
      texto: 'lab',
      page: 2,
      size: 50
    });

    expect(recibido?.searchParams.get('estado')).toBe('ACTIVO');
    expect(recibido?.searchParams.get('texto')).toBe('lab');
    expect(recibido?.searchParams.get('page')).toBe('2');
    expect(recibido?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([sampleProveedor]);
  });

  it('fetchProveedores omite los filtros ausentes y usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/proveedores', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchProveedores(client, {});

    expect(recibido?.search).toBe('?page=0&size=20');
  });

  it('fetchProveedor obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/compras/proveedores/prov-1', () =>
        HttpResponse.json(sampleProveedor)
      )
    );

    await expect(fetchProveedor(client, 'prov-1')).resolves.toEqual(sampleProveedor);
  });

  it('crearProveedor hace POST con el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/compras/proveedores', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor, { status: 201 });
      })
    );

    await expect(crearProveedor(client, payload)).resolves.toEqual(sampleProveedor);
    expect(body).toEqual(payload);
  });

  it('actualizarProveedor hace PUT con el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.put('http://localhost/api/v1/compras/proveedores/prov-1', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor);
      })
    );

    await expect(actualizarProveedor(client, 'prov-1', payload)).resolves.toEqual(sampleProveedor);
    expect(body).toEqual(payload);
  });

  it('cambiarEstadoProveedor hace PATCH con el estado', async () => {
    let body: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/compras/proveedores/prov-1/estado',
        async ({ request }) => {
          body = await request.json();
          return HttpResponse.json({ ...sampleProveedor, estado: 'SUSPENDIDO' });
        }
      )
    );

    const result = await cambiarEstadoProveedor(client, 'prov-1', 'SUSPENDIDO');

    expect(result.estado).toBe('SUSPENDIDO');
    expect(body).toEqual({ estado: 'SUSPENDIDO' });
  });

  it('proveedoresQuery arma la clave con valores por defecto y con filtros', () => {
    expect(proveedoresQuery({}).queryKey).toEqual([
      'compras',
      'proveedores',
      'lista',
      '',
      '',
      0,
      20
    ]);
    expect(
      proveedoresQuery({ estado: 'ACTIVO', texto: 'lab', page: 2, size: 10 }).queryKey
    ).toEqual(['compras', 'proveedores', 'lista', 'ACTIVO', 'lab', 2, 10]);
  });

  it('proveedoresQuery y proveedorQuery consultan con el cliente de la aplicación', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce(pagina([sampleProveedor]))
      .mockResolvedValueOnce(sampleProveedor);
    const queryClient = new QueryClient();

    await queryClient.fetchQuery(proveedoresQuery({ estado: 'ACTIVO' }));
    const detalle = proveedorQuery('prov-1');
    await queryClient.fetchQuery(detalle);

    expect(get).toHaveBeenNthCalledWith(1, '/compras/proveedores?estado=ACTIVO&page=0&size=20');
    expect(get).toHaveBeenNthCalledWith(2, '/compras/proveedores/prov-1');
    expect(detalle.queryKey).toEqual(['compras', 'proveedores', 'detalle', 'prov-1']);
  });
});
