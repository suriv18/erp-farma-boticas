import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleTurno, sampleTurnoCerrado } from '../../../test/ventas-fixtures';
import { CashRegisterPage } from './CashRegisterPage';

const turnoActualUrl = '*/api/v1/ventas/turnos/actual';
const turnosUrl = '*/api/v1/ventas/turnos';

const sinTurno = () =>
  HttpResponse.json({ title: 'No encontrado', code: 'VEN_TURNO_NO_ENCONTRADO' }, { status: 404 });

function guardarPuesto(terminalId = 'term-1') {
  localStorage.setItem(
    'erp.puesto-trabajo',
    JSON.stringify({ establecimientoId: 'est-1', terminalId, almacenId: 'alm-1' })
  );
}

function renderPage() {
  return renderRoute('/caja', CashRegisterPage, '/caja');
}

beforeEach(() => {
  localStorage.clear();
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/organizacion/terminales-pos', () =>
      HttpResponse.json(
        pagina([
          { ...sampleTerminal, id: 'term-1', nombre: 'Caja 1', estado: 'ACTIVO' },
          { ...sampleTerminal, id: 'term-2', nombre: 'Caja 2', estado: 'ACTIVO' }
        ])
      )
    )
  );
});

describe('CashRegisterPage', () => {
  it('sin terminal elegida pide seleccionarla y muestra el selector', async () => {
    renderPage();

    expect(screen.getByText('Selecciona una terminal para ver su turno.')).toBeInTheDocument();
    expect(await screen.findByLabelText('Establecimiento')).toBeInTheDocument();
  });

  it('al elegir establecimiento y terminal consulta el turno y conserva el almacén del mismo establecimiento', async () => {
    guardarPuesto('');
    server.use(http.get(turnoActualUrl, () => HttpResponse.json(sampleTurno)));
    const { user } = renderPage();

    await screen.findByRole('option', { name: 'Caja 1' });
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-1');

    expect(await screen.findByRole('button', { name: 'Cerrar turno' })).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('erp.puesto-trabajo') ?? '{}')).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1',
      almacenId: 'alm-1'
    });
  });

  it('al cambiar de establecimiento descarta el almacén guardado', async () => {
    guardarPuesto();
    server.use(http.get(turnoActualUrl, sinTurno));
    const { user } = renderPage();

    await screen.findByText('No hay un turno abierto en esta terminal.');
    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-2');

    expect(JSON.parse(localStorage.getItem('erp.puesto-trabajo') ?? '{}')).toEqual({
      establecimientoId: 'est-2',
      terminalId: '',
      almacenId: ''
    });
    expect(screen.getByText('Selecciona una terminal para ver su turno.')).toBeInTheDocument();
  });

  it('sin turno abierto ofrece abrirlo y luego muestra el resumen', async () => {
    guardarPuesto();
    let abierto = false;
    let body: unknown;
    server.use(
      http.get(turnoActualUrl, () => (abierto ? HttpResponse.json(sampleTurno) : sinTurno())),
      http.post(turnosUrl, async ({ request }) => {
        body = await request.json();
        abierto = true;
        return HttpResponse.json(sampleTurno, { status: 201 });
      })
    );
    const { user } = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay un turno abierto en esta terminal.')
    ).toBeInTheDocument();
    await user.type(screen.getByLabelText('Fondo inicial'), '150.5');
    await user.click(screen.getByRole('button', { name: 'Abrir turno' }));

    expect(await screen.findByText('ABIERTO')).toBeInTheDocument();
    expect(body).toEqual({ terminalId: 'term-1', fondoInicial: 150.5 });
    expect(screen.getByRole('button', { name: 'Cerrar turno' })).toBeInTheDocument();
  });

  it('con turno abierto permite cerrarlo y muestra el resultado del cierre', async () => {
    guardarPuesto();
    let cerrado = false;
    let body: unknown;
    server.use(
      http.get(turnoActualUrl, () => (cerrado ? sinTurno() : HttpResponse.json(sampleTurno))),
      http.post('*/api/v1/ventas/turnos/turno-1/cierre', async ({ request }) => {
        body = await request.json();
        cerrado = true;
        return HttpResponse.json(sampleTurnoCerrado);
      })
    );
    const { user } = renderPage();

    await user.type(await screen.findByLabelText('Total declarado'), '348.5');
    await user.type(screen.getByLabelText('Observación'), 'Faltante');
    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }));

    expect(await screen.findByText('Turno cerrado')).toBeInTheDocument();
    expect(body).toEqual({ totalDeclarado: 348.5, observacion: 'Faltante' });
    expect(screen.getByText('-S/ 1.50')).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.queryByText('No hay un turno abierto en esta terminal.')
      ).not.toBeInTheDocument()
    );
  });

  it('estima la diferencia contra el total del sistema que incluye las ventas en efectivo', async () => {
    guardarPuesto();
    server.use(
      http.get(turnoActualUrl, () =>
        HttpResponse.json({ ...sampleTurno, totalVentasSistema: 25, totalSistema: 125 })
      )
    );
    const { user } = renderPage();

    await user.type(await screen.findByLabelText('Total declarado'), '120');

    expect(screen.getByText('Diferencia estimada: -S/ 5.00')).toBeInTheDocument();
    expect(screen.getByText('S/ 125.00')).toBeInTheDocument();
  });

  it('envía el cierre sin observación cuando está en blanco', async () => {
    guardarPuesto();
    let body: unknown;
    server.use(
      http.get(turnoActualUrl, () => HttpResponse.json(sampleTurno)),
      http.post('*/api/v1/ventas/turnos/turno-1/cierre', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleTurnoCerrado);
      })
    );
    const { user } = renderPage();

    await user.type(await screen.findByLabelText('Total declarado'), '100');
    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }));

    await screen.findByText('Turno cerrado');
    expect(body).toEqual({ totalDeclarado: 100 });
  });

  it('el resultado del cierre desaparece al cambiar de terminal', async () => {
    guardarPuesto();
    server.use(
      http.get(turnoActualUrl, () => HttpResponse.json(sampleTurno)),
      http.post('*/api/v1/ventas/turnos/turno-1/cierre', () =>
        HttpResponse.json(sampleTurnoCerrado)
      )
    );
    const { user } = renderPage();

    await user.type(await screen.findByLabelText('Total declarado'), '100');
    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }));
    await screen.findByText('Turno cerrado');
    await screen.findByRole('option', { name: 'Caja 2' });
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-2');

    await waitFor(() => expect(screen.queryByText('Turno cerrado')).not.toBeInTheDocument());
  });

  it('muestra el error del backend al abrir el turno', async () => {
    guardarPuesto();
    server.use(
      http.get(turnoActualUrl, sinTurno),
      http.post(turnosUrl, () =>
        HttpResponse.json(
          { title: 'Turno ya abierto', code: 'VEN_TURNO_YA_ABIERTO' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();

    await user.type(await screen.findByLabelText('Fondo inicial'), '100');
    await user.click(screen.getByRole('button', { name: 'Abrir turno' }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('muestra el error cuando no se puede cargar el turno', async () => {
    guardarPuesto();
    server.use(
      http.get(turnoActualUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 }))
    );

    renderPage();

    expect(await screen.findByText('No se pudo cargar el turno.')).toBeInTheDocument();
  });
});
