import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleOrden, sampleRecepcion } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import type { EstadoOrden } from '../api/ordenes.types';
import { RecepcionPage } from './RecepcionPage';

vi.mock('../lib/fecha-utc', () => ({ fechaUtcISO: () => '2026-10-09' }));

const recepcionesUrl = '*/api/v1/compras/recepciones';
const ordenUrl = '*/api/v1/compras/ordenes/orden-1';

function mockEntorno(estado: EstadoOrden = 'EMITIDA') {
  server.use(
    http.get(ordenUrl, () => HttpResponse.json({ ...sampleOrden, estado })),
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
  );
}

function renderPage() {
  return renderRoute(
    '/compras/ordenes/:ordenId/*',
    RecepcionPage,
    '/compras/ordenes/orden-1/recepcion'
  );
}

type Usuario = ReturnType<typeof renderPage>['user'];

const linea1 = () => within(screen.getByRole('group', { name: 'Línea 1 — Paracetamol 500 mg' }));

const registrar = () => screen.getByRole('button', { name: 'Registrar recepción' });

async function completarRecepcion(user: Usuario) {
  await screen.findByRole('option', { name: 'Almacén Central' });
  await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-1');
  await user.type(screen.getByLabelText('Serie del documento'), 'F001');
  await user.type(screen.getByLabelText('Número del documento'), '123');
  await user.type(linea1().getByLabelText('Número de lote'), 'L2026-01');
  await user.type(linea1().getByLabelText('Fecha de vencimiento'), '2099-12-31');
  await user.type(linea1().getByLabelText('Cantidad recibida'), '4');
}

describe('RecepcionPage', () => {
  it('muestra las líneas pendientes con el costo de la orden y solo los almacenes del destino', async () => {
    mockEntorno();

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Recepción de la orden OC-2026-000001' })
    ).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Almacén Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Frío' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Almacén Norte' })).not.toBeInTheDocument();
    expect(linea1().getByText('Pendiente: 10 UND')).toBeInTheDocument();
    expect(linea1().getByLabelText('Costo unitario')).toHaveValue('5.5');
  });

  it('registra una recepción parcial con Idempotency-Key, invalida compras e inventario y vuelve al detalle', async () => {
    mockEntorno();
    let body: unknown;
    let clave: string | null = null;
    server.use(
      http.post(recepcionesUrl, async ({ request }) => {
        clave = request.headers.get('Idempotency-Key');
        body = await request.json();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user, router, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await completarRecepcion(user);
    await user.click(registrar());

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    expect(body).toEqual({
      ordenCompraId: 'orden-1',
      almacenId: 'alm-1',
      documentoProveedorTipo: '01',
      documentoProveedorSerie: 'F001',
      documentoProveedorNumero: '123',
      items: [
        {
          numeroLineaOrden: 1,
          numeroLote: 'L2026-01',
          fechaVencimiento: '2099-12-31',
          cantidadRecibida: 4,
          cantidadRechazada: 0,
          costoUnitario: 5.5
        }
      ]
    });
    expect(clave).toEqual(expect.any(String));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('exige el motivo cuando hay cantidad rechazada y luego lo envía', async () => {
    mockEntorno();
    const enviado = vi.fn();
    let body: unknown;
    server.use(
      http.post(recepcionesUrl, async ({ request }) => {
        enviado();
        body = await request.json();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user, router } = renderPage();
    await completarRecepcion(user);
    await user.clear(linea1().getByLabelText('Cantidad rechazada'));
    await user.type(linea1().getByLabelText('Cantidad rechazada'), '1');

    await user.click(registrar());

    expect(await linea1().findByRole('alert')).toHaveTextContent('Indica el motivo del rechazo.');
    expect(enviado).not.toHaveBeenCalled();

    await user.type(linea1().getByLabelText('Motivo del rechazo'), 'Empaque dañado');
    await user.click(registrar());

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    expect(body).toMatchObject({
      items: [{ cantidadRecibida: 4, cantidadRechazada: 1, motivoRechazo: 'Empaque dañado' }]
    });
  });

  it('no envía sin almacén ni cantidades y muestra qué falta', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(recepcionesUrl, () => {
        enviado();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Almacén Central' });

    await user.click(registrar());

    expect(await screen.findByText('Selecciona el almacén de recepción.')).toBeInTheDocument();
    expect(
      screen.getByText('Ingresa la cantidad recibida de al menos un producto.')
    ).toBeInTheDocument();
    expect(enviado).not.toHaveBeenCalled();
  });

  it('valida el vencimiento contra la fecha de hoy en UTC como inventario', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(recepcionesUrl, () => {
        enviado();
        return HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user } = renderPage();
    await completarRecepcion(user);
    const vencimiento = linea1().getByLabelText('Fecha de vencimiento');
    await user.clear(vencimiento);
    await user.type(vencimiento, '2026-10-08');

    await user.click(registrar());

    expect(
      await screen.findByText(
        'No se puede ingresar un lote vencido; recházalo por completo o corrige la fecha.'
      )
    ).toBeInTheDocument();
    expect(enviado).not.toHaveBeenCalled();
  });

  it('reutiliza la clave al reintentar y usa otra después de registrar con éxito', async () => {
    mockEntorno();
    const claves: (string | null)[] = [];
    server.use(
      http.post(recepcionesUrl, ({ request }) => {
        claves.push(request.headers.get('Idempotency-Key'));
        return claves.length === 1
          ? HttpResponse.json({ title: 'Error' }, { status: 500 })
          : HttpResponse.json(sampleRecepcion, { status: 201 });
      })
    );
    const { user, router } = renderPage();
    await completarRecepcion(user);

    await user.click(registrar());
    await screen.findByRole('alert');
    await user.click(registrar());
    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    await user.click(registrar());
    await waitFor(() => expect(claves).toHaveLength(3));

    expect(claves[0]).toBe(claves[1]);
    expect(claves[2]).not.toBe(claves[1]);
  });

  it('muestra el error traducido del backend y no navega', async () => {
    mockEntorno();
    server.use(
      http.post(recepcionesUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_RECEPCION_EXCEDE_PENDIENTE', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user, router } = renderPage();
    await completarRecepcion(user);

    await user.click(registrar());

    expect(
      await screen.findByText(
        'La cantidad recibida excede lo pendiente de la orden más su tolerancia.'
      )
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1/recepcion');
  });

  it('avisa cuando la orden no admite recepciones', async () => {
    mockEntorno('BORRADOR');

    renderPage();

    expect(
      await screen.findByText('La orden no admite recepciones en su estado actual.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar recepción' })).not.toBeInTheDocument();
  });

  it('muestra la carga y el error de la orden', async () => {
    server.use(
      http.get(ordenUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 }))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('cancela y vuelve al detalle de la orden', async () => {
    mockEntorno();
    const { user, router } = renderPage();
    await screen.findByRole('heading', { name: 'Recepción de la orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1');
  });
});
