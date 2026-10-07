import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura, samplePosicion } from '../../../test/inventario-fixtures';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleSkuVenta, sampleTurno, sampleVenta } from '../../../test/ventas-fixtures';
import type { SkuResumen } from '../../catalogo';
import { PosPage } from './PosPage';

const ventasUrl = '*/api/v1/ventas/ventas';

const sinTurno = () =>
  HttpResponse.json({ title: 'No encontrado', code: 'VEN_TURNO_NO_ENCONTRADO' }, { status: 404 });

function guardarPuesto(terminalId = 'term-1') {
  localStorage.setItem(
    'erp.puesto-trabajo',
    JSON.stringify({ establecimientoId: 'est-1', terminalId, almacenId: 'alm-1' })
  );
}

function mockEntorno({
  turno = true,
  skus = [sampleSkuVenta]
}: { turno?: boolean; skus?: SkuResumen[] } = {}) {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/organizacion/terminales-pos', () =>
      HttpResponse.json(
        pagina([
          { ...sampleTerminal, id: 'term-1', nombre: 'Caja 1', estado: 'ACTIVO' },
          { ...sampleTerminal, id: 'term-2', nombre: 'Caja 2', estado: 'ACTIVO' }
        ])
      )
    ),
    http.get('*/api/v1/ventas/turnos/actual', () =>
      turno ? HttpResponse.json(sampleTurno) : sinTurno()
    ),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina(skus))),
    http.get('*/api/v1/inventario/posiciones', () => HttpResponse.json(pagina([samplePosicion])))
  );
}

function renderPage() {
  return renderRoute('/pos', PosPage, '/pos');
}

async function agregarParacetamol(user: ReturnType<typeof renderPage>['user']) {
  await user.type(screen.getByLabelText('Buscar producto'), 'paracetamol');
  await user.click(screen.getByRole('button', { name: 'Buscar' }));
  await user.click(await screen.findByRole('button', { name: 'Agregar MED-001' }));
}

async function prepararCobro(user: ReturnType<typeof renderPage>['user']) {
  await screen.findByRole('option', { name: 'Caja 1' });
  await agregarParacetamol(user);
  await user.type(screen.getByLabelText('Monto recibido'), '30');
  await waitFor(() => expect(screen.getByRole('button', { name: 'Cobrar' })).toBeEnabled());
}

beforeEach(() => {
  localStorage.clear();
  guardarPuesto();
});

afterEach(() => {
  localStorage.clear();
});

describe('PosPage', () => {
  it('sin turno avisa, enlaza a Caja y bloquea el cobro', async () => {
    mockEntorno({ turno: false });
    const { user } = renderPage();

    expect(
      await screen.findByText('No hay un turno abierto en esta terminal.')
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir a Caja' })).toHaveAttribute('href', '/caja');
    await agregarParacetamol(user);
    await user.type(screen.getByLabelText('Monto recibido'), '30');

    expect(screen.getByRole('button', { name: 'Cobrar' })).toBeDisabled();
  });

  it('vende en efectivo, muestra el comprobante e invalida las consultas', async () => {
    mockEntorno();
    const cuerpo = vi.fn();
    const clave = vi.fn();
    server.use(
      http.post(ventasUrl, async ({ request }) => {
        cuerpo(await request.json());
        clave(request.headers.get('Idempotency-Key'));
        return HttpResponse.json(sampleVenta, { status: 201 });
      })
    );
    const { user, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    await screen.findByRole('option', { name: 'Caja 1' });

    await agregarParacetamol(user);
    expect(screen.getByLabelText('Cantidad de MED-001')).toHaveValue('1');
    expect(screen.getByLabelText('Precio de MED-001')).toHaveValue('12.5');
    await user.clear(screen.getByLabelText('Cantidad de MED-001'));
    await user.type(screen.getByLabelText('Cantidad de MED-001'), '2');
    expect(screen.getAllByText('S/ 25.00').length).toBeGreaterThan(0);
    await user.type(screen.getByLabelText('Monto recibido'), '30');
    expect(screen.getByText('S/ 5.00')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Cobrar' })).toBeEnabled());
    await user.click(screen.getByRole('button', { name: 'Cobrar' }));

    expect(await screen.findByText('Venta registrada')).toBeInTheDocument();
    expect(screen.getByText('EST001-T01-000001')).toBeInTheDocument();
    expect(cuerpo).toHaveBeenCalledWith({
      terminalId: 'term-1',
      almacenId: 'alm-1',
      lineas: [{ skuId: 'sku-0001-aaaa', cantidad: 2, precioUnitario: 12.5 }],
      pago: { montoRecibido: 30 }
    });
    expect(clave.mock.calls[0]?.[0]).toBeTruthy();
    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['ventas'] });
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['caja'] });
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
    });

    await user.click(screen.getByRole('button', { name: 'Nueva venta' }));

    expect(screen.queryByText('Venta registrada')).not.toBeInTheDocument();
    expect(screen.getByText('El carrito está vacío.')).toBeInTheDocument();
    expect(screen.getByLabelText('Monto recibido')).toHaveValue('');
  });

  it('reutiliza la clave al reintentar el mismo carrito', async () => {
    mockEntorno();
    const claves: (string | null)[] = [];
    server.use(
      http.post(ventasUrl, ({ request }) => {
        claves.push(request.headers.get('Idempotency-Key'));
        return claves.length === 1
          ? HttpResponse.json({ title: 'Error' }, { status: 500 })
          : HttpResponse.json(sampleVenta, { status: 201 });
      })
    );
    const { user } = renderPage();
    await prepararCobro(user);

    await user.click(screen.getByRole('button', { name: 'Cobrar' }));
    await screen.findByRole('alert');
    await user.click(screen.getByRole('button', { name: 'Cobrar' }));
    await screen.findByText('Venta registrada');

    expect(claves).toHaveLength(2);
    expect(claves[0]).toBe(claves[1]);
  });

  it('genera otra clave cuando cambia el carrito entre intentos', async () => {
    mockEntorno();
    const claves: (string | null)[] = [];
    server.use(
      http.post(ventasUrl, ({ request }) => {
        claves.push(request.headers.get('Idempotency-Key'));
        return HttpResponse.json({ title: 'Error' }, { status: 500 });
      })
    );
    const { user } = renderPage();
    await prepararCobro(user);
    await user.click(screen.getByRole('button', { name: 'Cobrar' }));
    await screen.findByRole('alert');

    await user.clear(screen.getByLabelText('Cantidad de MED-001'));
    await user.type(screen.getByLabelText('Cantidad de MED-001'), '2');
    await user.click(screen.getByRole('button', { name: 'Cobrar' }));
    await waitFor(() => expect(claves).toHaveLength(2));

    expect(claves[0]).not.toBe(claves[1]);
  });

  it('traduce el error de stock y conserva el carrito', async () => {
    mockEntorno();
    server.use(
      http.post(ventasUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'INV_STOCK_INSUFICIENTE', detail: 'sin stock' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await prepararCobro(user);

    await user.click(screen.getByRole('button', { name: 'Cobrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No hay stock suficiente para una de las líneas. Ajusta la cantidad.'
    );
    expect(screen.getByLabelText('Cantidad de MED-001')).toHaveValue('1');
  });

  it('un SKU sin precio de referencia entra con precio vacío y bloquea el cobro', async () => {
    mockEntorno({ skus: [{ ...sampleSkuVenta, precioVentaReferencia: null }] });
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Caja 1' });

    await agregarParacetamol(user);
    await user.type(screen.getByLabelText('Monto recibido'), '30');

    expect(screen.getByLabelText('Precio de MED-001')).toHaveValue('');
    expect(screen.getByRole('alert')).toHaveTextContent(
      'El precio debe ser mayor o igual a cero con hasta 4 decimales.'
    );
    expect(screen.getByRole('button', { name: 'Cobrar' })).toBeDisabled();

    await user.type(screen.getByLabelText('Precio de MED-001'), '10');

    await waitFor(() => expect(screen.getByRole('button', { name: 'Cobrar' })).toBeEnabled());
  });

  it('sin terminal guardada pide seleccionarla y deshabilita agregar productos', async () => {
    guardarPuesto('');
    mockEntorno();
    const { user } = renderPage();

    expect(screen.getByText('Selecciona una terminal para vender.')).toBeInTheDocument();
    await screen.findByRole('option', { name: 'Caja 1' });
    await user.type(screen.getByLabelText('Buscar producto'), 'paracetamol');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByRole('button', { name: 'Agregar MED-001' })).toBeDisabled();
  });

  it('al cambiar de terminal vacía el carrito', async () => {
    mockEntorno();
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Caja 2' });
    await agregarParacetamol(user);
    expect(screen.getByLabelText('Cantidad de MED-001')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-2');

    expect(screen.getByText('El carrito está vacío.')).toBeInTheDocument();
  });

  it('cierra el comprobante con la X y deja el carrito vacío', async () => {
    mockEntorno();
    server.use(http.post(ventasUrl, () => HttpResponse.json(sampleVenta, { status: 201 })));
    const { user } = renderPage();
    await prepararCobro(user);
    await user.click(screen.getByRole('button', { name: 'Cobrar' }));
    await screen.findByText('Venta registrada');

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.getByText('El carrito está vacío.')).toBeInTheDocument();
  });

  it('quita una línea del carrito', async () => {
    mockEntorno();
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Caja 1' });
    await agregarParacetamol(user);

    await user.click(screen.getByRole('button', { name: 'Quitar MED-001' }));

    expect(screen.getByText('El carrito está vacío.')).toBeInTheDocument();
  });

  it('muestra el comprobante sin nombre cuando el establecimiento de la venta no está en la estructura', async () => {
    mockEntorno();
    server.use(
      http.post(ventasUrl, () =>
        HttpResponse.json({ ...sampleVenta, establecimientoId: 'est-desconocido' }, { status: 201 })
      )
    );
    const { user } = renderPage();
    await prepararCobro(user);

    await user.click(screen.getByRole('button', { name: 'Cobrar' }));

    expect(await screen.findByText('Venta registrada')).toBeInTheDocument();
  });

  it('al cambiar solo de almacén conserva el carrito', async () => {
    mockEntorno();
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Caja 1' });
    await agregarParacetamol(user);

    await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-2');

    expect(screen.getByLabelText('Cantidad de MED-001')).toBeInTheDocument();
  });
});
