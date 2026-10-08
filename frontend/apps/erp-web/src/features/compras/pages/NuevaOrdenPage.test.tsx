import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleOrden, sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { formatoImporte } from '../lib/formato-compras';
import { NuevaOrdenPage } from './NuevaOrdenPage';

const ordenesUrl = '*/api/v1/compras/ordenes';

function mockEntorno(
  detalleSku: () => Response | Promise<Response> = () =>
    HttpResponse.json({ id: 'sku-0001-aaaa', afectoIgv: true })
) {
  server.use(
    http.get('*/api/v1/compras/proveedores', () => HttpResponse.json(pagina([sampleProveedor]))),
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSkuVenta]))),
    http.get('*/api/v1/catalogo/skus/sku-0001-aaaa', detalleSku)
  );
}

function renderPage() {
  return renderRoute('*', NuevaOrdenPage, '/compras/ordenes/nueva');
}

type Usuario = ReturnType<typeof renderPage>['user'];

async function elegirCabecera(user: Usuario) {
  await screen.findByRole('option', { name: 'Laboratorios Perú SAC' });
  await user.selectOptions(screen.getByLabelText('Proveedor'), 'prov-1');
  await user.selectOptions(screen.getByLabelText('Establecimiento de destino'), 'est-1');
}

async function agregarProducto(user: Usuario) {
  await user.type(screen.getByLabelText('Buscar producto'), 'paracetamol');
  await user.click(screen.getByRole('button', { name: 'Buscar' }));
  await user.click(await screen.findByRole('button', { name: 'Agregar MED-001' }));
}

describe('NuevaOrdenPage', () => {
  it('precarga las condiciones del proveedor al elegirlo', async () => {
    mockEntorno();
    const { user } = renderPage();

    await elegirCabecera(user);

    expect(screen.getByLabelText('Moneda')).toHaveValue('PEN');
    expect(screen.getByLabelText('Condición de pago')).toHaveValue('CREDITO 30');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('30');
  });

  it('crea la orden con IGV sugerido, totales en vivo y abre su detalle', async () => {
    mockEntorno();
    let body: unknown;
    server.use(
      http.post(ordenesUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user, router, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await elegirCabecera(user);
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await waitFor(() => expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('0.99'));
    expect(
      screen.getAllByText((texto) => texto === formatoImporte(6.49, 'PEN').replace(/\s/g, ' '))
        .length
    ).toBeGreaterThan(0);

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/ordenes/orden-1'));
    expect(body).toEqual({
      proveedorId: 'prov-1',
      establecimientoDestinoId: 'est-1',
      moneda: 'PEN',
      condicionPago: 'CREDITO 30',
      diasCredito: 30,
      lineas: [
        {
          skuId: 'sku-0001-aaaa',
          cantidad: 1,
          unidadMedidaCodigo: 'UND',
          precioUnitario: 5.5,
          descuento: 0,
          impuesto: 0.99,
          toleranciaExcesoPct: 0,
          toleranciaDefectoPct: 0
        }
      ]
    });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('un producto no afecto a IGV no sugiere impuesto', async () => {
    mockEntorno(() => HttpResponse.json({ id: 'sku-0001-aaaa', afectoIgv: false }));
    const { user } = renderPage();

    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('0');
  });

  it('al corregir el impuesto a mano ya no se recalcula', async () => {
    mockEntorno();
    const { user } = renderPage();
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await user.clear(screen.getByLabelText('Impuesto de MED-001'));
    await user.type(screen.getByLabelText('Impuesto de MED-001'), '2');
    await user.clear(screen.getByLabelText('Cantidad de MED-001'));
    await user.type(screen.getByLabelText('Cantidad de MED-001'), '3');

    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('2');

    await user.click(screen.getByRole('button', { name: 'Usar impuesto sugerido de MED-001' }));

    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('2.97');
    expect(
      screen.queryByRole('button', { name: 'Usar impuesto sugerido de MED-001' })
    ).not.toBeInTheDocument();
  });

  it('exige el tipo de cambio cuando la moneda no es PEN', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(ordenesUrl, () => {
        enviado();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user } = renderPage();
    await elegirCabecera(user);
    await user.selectOptions(screen.getByLabelText('Moneda'), 'USD');
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(
      await screen.findByText('El tipo de cambio es obligatorio cuando la moneda no es PEN.')
    ).toBeInTheDocument();
    expect(enviado).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText('Tipo de cambio'), '3.81');
    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    await waitFor(() => expect(enviado).toHaveBeenCalledTimes(1));
  });

  it('no duplica un producto ya agregado y permite quitarlo', async () => {
    mockEntorno();
    const { user } = renderPage();
    await agregarProducto(user);
    await screen.findByLabelText('Precio de MED-001');

    await user.click(screen.getByRole('button', { name: 'Agregar MED-001' }));

    await waitFor(() => expect(screen.getAllByLabelText('Cantidad de MED-001')).toHaveLength(1));
    await user.click(screen.getByRole('button', { name: 'Quitar MED-001' }));

    expect(screen.getByText('Aún no agregaste productos.')).toBeInTheDocument();
  });

  it('avisa cuando no se puede leer el detalle del producto', async () => {
    mockEntorno(() => HttpResponse.json({ title: 'Error' }, { status: 500 }));
    const { user } = renderPage();

    await agregarProducto(user);

    expect(
      await screen.findByText('No se pudo leer el producto. Inténtalo de nuevo.')
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('Cantidad de MED-001')).not.toBeInTheDocument();
  });

  it('no envía una orden incompleta y muestra qué falta', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(ordenesUrl, () => {
        enviado();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('option', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(await screen.findByText('Selecciona un proveedor.')).toBeInTheDocument();
    expect(screen.getByText('Selecciona el establecimiento de destino.')).toBeInTheDocument();
    expect(screen.getByText('Agrega al menos un producto.')).toBeInTheDocument();
    expect(enviado).not.toHaveBeenCalled();
  });

  it('no envía mientras una línea tenga errores', async () => {
    mockEntorno();
    const enviado = vi.fn();
    server.use(
      http.post(ordenesUrl, () => {
        enviado();
        return HttpResponse.json(sampleOrden, { status: 201 });
      })
    );
    const { user } = renderPage();
    await elegirCabecera(user);
    await agregarProducto(user);
    await screen.findByLabelText('Precio de MED-001');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El precio debe ser mayor o igual a cero con hasta 6 decimales.'
    );
    expect(enviado).not.toHaveBeenCalled();
  });

  it('muestra el error traducido del backend y no navega', async () => {
    mockEntorno();
    server.use(
      http.post(ordenesUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_PROVEEDOR_NO_OPERABLE', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user, router } = renderPage();
    await elegirCabecera(user);
    await agregarProducto(user);
    await user.type(await screen.findByLabelText('Precio de MED-001'), '5.5');

    await user.click(screen.getByRole('button', { name: 'Crear orden' }));

    expect(
      await screen.findByText('El proveedor debe estar activo para emitir órdenes de compra.')
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/compras/ordenes/nueva');
  });

  it('cancela y vuelve al listado de órdenes', async () => {
    mockEntorno();
    const { user, router } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/compras/ordenes');
  });
});
