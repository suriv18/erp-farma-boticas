import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleTurno, sampleTurnoCerrado } from '../../../test/ventas-fixtures';
import { abrirTurno, cerrarTurno, fetchTurnoActual, turnoActualQuery } from './turnos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('turnos.api', () => {
  it('fetchTurnoActual consulta por terminal y devuelve el turno', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/ventas/turnos/actual', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(sampleTurno);
      })
    );

    await expect(fetchTurnoActual(client, 'term-1')).resolves.toEqual(sampleTurno);
    expect(recibido?.searchParams.get('terminalId')).toBe('term-1');
  });

  it('fetchTurnoActual devuelve null cuando la terminal no tiene turno abierto', async () => {
    server.use(
      http.get('http://localhost/api/v1/ventas/turnos/actual', () =>
        HttpResponse.json(
          { title: 'No encontrado', code: 'VEN_TURNO_NO_ENCONTRADO' },
          { status: 404 }
        )
      )
    );

    await expect(fetchTurnoActual(client, 'term-1')).resolves.toBeNull();
  });

  it('fetchTurnoActual propaga los demás errores', async () => {
    server.use(
      http.get('http://localhost/api/v1/ventas/turnos/actual', () =>
        HttpResponse.json({ title: 'Error' }, { status: 500 })
      )
    );

    await expect(fetchTurnoActual(client, 'term-1')).rejects.toBeInstanceOf(ApiError);
  });

  it('fetchTurnoActual propaga errores que no son de la API', async () => {
    const fallo = new Error('red caída');
    const falso = { get: vi.fn().mockRejectedValue(fallo) } as unknown as typeof client;

    await expect(fetchTurnoActual(falso, 'term-1')).rejects.toBe(fallo);
  });

  it('abrirTurno envía terminal y fondo inicial', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/ventas/turnos', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleTurno, { status: 201 });
      })
    );

    await expect(abrirTurno(client, { terminalId: 'term-1', fondoInicial: 100 })).resolves.toEqual(
      sampleTurno
    );
    expect(body).toEqual({ terminalId: 'term-1', fondoInicial: 100 });
  });

  it('cerrarTurno envía el total declarado y la observación', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/ventas/turnos/turno-1/cierre', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleTurnoCerrado);
      })
    );

    await expect(
      cerrarTurno(client, 'turno-1', { totalDeclarado: 348.5, observacion: 'Faltante de monedas' })
    ).resolves.toEqual(sampleTurnoCerrado);
    expect(body).toEqual({ totalDeclarado: 348.5, observacion: 'Faltante de monedas' });
  });

  it('turnoActualQuery arma la clave y consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(sampleTurno);

    const query = turnoActualQuery('term-1');
    const result = await new QueryClient().fetchQuery(query);

    expect(query.queryKey).toEqual(['caja', 'turno-actual', 'term-1']);
    expect(get).toHaveBeenCalledWith('/ventas/turnos/actual?terminalId=term-1');
    expect(result).toEqual(sampleTurno);
  });
});
