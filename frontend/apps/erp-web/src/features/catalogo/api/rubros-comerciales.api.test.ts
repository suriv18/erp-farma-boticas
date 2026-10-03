import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarRubroComercial,
  cambiarEstadoRubroComercial,
  crearRubroComercial,
  fetchRubrosComerciales,
  rubrosComercialesQuery
} from './rubros-comerciales.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleRubro = {
  id: 'rubro-1',
  tenantId: 'tenant-1',
  codigo: 'FARMA',
  nombre: 'Farmacéutico',
  descripcion: null,
  esFarmaceutico: true,
  orden: 1,
  estado: 'ACTIVO'
};

describe('rubros-comerciales.api', () => {
  it('fetchRubrosComerciales consulta /catalogo/rubros-comerciales con page y size por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/rubros-comerciales', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleRubro], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRubrosComerciales(client, {});

    expect(receivedUrl?.searchParams.has('tenantId')).toBe(false);
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleRubro]);
  });

  it('fetchRubrosComerciales envia q, esFarmaceutico, page y size cuando se especifican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/rubros-comerciales', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleRubro], page: 1, size: 10, totalElements: 15 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRubrosComerciales(client, {
      q: 'farma',
      esFarmaceutico: true,
      page: 1,
      size: 10
    });

    expect(receivedUrl?.searchParams.get('q')).toBe('farma');
    expect(receivedUrl?.searchParams.get('esFarmaceutico')).toBe('true');
    expect(receivedUrl?.searchParams.get('page')).toBe('1');
    expect(result.totalElements).toBe(15);
  });

  it('fetchRubrosComerciales envia estado cuando se especifica', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/catalogo/rubros-comerciales', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await fetchRubrosComerciales(client, { estado: 'INACTIVO' });

    expect(receivedUrl?.searchParams.get('estado')).toBe('INACTIVO');
  });

  it('rubrosComercialesQuery arma una queryKey con los defaults de page y size', () => {
    const options = rubrosComercialesQuery({});

    expect(options.queryKey).toEqual(['catalogo', 'rubros-comerciales', '', '', '', 0, 20]);
  });

  it('crearRubroComercial envia el payload y devuelve el rubro creado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/catalogo/rubros-comerciales', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleRubro, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearRubroComercial(client, {
      codigo: 'FARMA',
      nombre: 'Farmacéutico',
      esFarmaceutico: true,
      orden: 1
    });

    expect(receivedBody).toEqual({
      codigo: 'FARMA',
      nombre: 'Farmacéutico',
      esFarmaceutico: true,
      orden: 1
    });
    expect(result).toEqual(sampleRubro);
  });

  it('actualizarRubroComercial envia PUT con el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.put(
        'http://localhost/api/v1/catalogo/rubros-comerciales/rubro-1',
        async ({ request }) => {
          receivedBody = await request.json();
          return HttpResponse.json({ ...sampleRubro, nombre: 'Farmacéutico y afines' });
        }
      )
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await actualizarRubroComercial(client, 'rubro-1', {
      codigo: 'FARMA',
      nombre: 'Farmacéutico y afines',
      esFarmaceutico: true,
      orden: 1
    });

    expect(receivedBody).toEqual({
      codigo: 'FARMA',
      nombre: 'Farmacéutico y afines',
      esFarmaceutico: true,
      orden: 1
    });
    expect(result.nombre).toBe('Farmacéutico y afines');
  });

  it('cambiarEstadoRubroComercial envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/catalogo/rubros-comerciales/rubro-1/estado',
        async ({ request }) => {
          receivedBody = await request.json();
          return new HttpResponse(null, { status: 204 });
        }
      )
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoRubroComercial(client, 'rubro-1', 'INACTIVO');

    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });
});
