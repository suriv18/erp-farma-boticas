import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleVenta, sampleVentaAnulada } from '../../../test/ventas-fixtures';
import { renderRoute } from '../../../test/render-route';
import { SaleDetailPage } from './SaleDetailPage';

const ventaUrl = '*/api/v1/ventas/ventas/venta-1';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
  );
});

function renderPage() {
  return renderRoute('/ventas/:ventaId', SaleDetailPage, '/ventas/venta-1');
}

function seccion(nombre: string) {
  return screen.getByRole('heading', { name: nombre }).parentElement as HTMLElement;
}

describe('SaleDetailPage', () => {
  it('muestra resumen, líneas con lotes y pago', async () => {
    server.use(http.get(ventaUrl, () => HttpResponse.json(sampleVenta)));

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Venta EST001-T01-000001' })
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ventas' })).toHaveAttribute('href', '/ventas');
    const resumen = seccion('Resumen');
    expect(within(resumen).getByText('CONFIRMADA')).toBeInTheDocument();
    expect(await within(resumen).findByText('Botica Central')).toBeInTheDocument();
    expect(within(seccion('Datos de la operación')).getByText('user-1')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Productos vendidos' })).toBeInTheDocument();
    expect(screen.getAllByText('Paracetamol 500 mg').length).toBeGreaterThan(0);
    expect(screen.getByText('Lote lote-1 × 2')).toBeInTheDocument();
    const pago = seccion('Pago');
    expect(within(pago).getByText('Recibido')).toBeInTheDocument();
    expect(within(pago).getByText('Vuelto')).toBeInTheDocument();
    expect(within(pago).getByText('EFECTIVO')).toBeInTheDocument();
  });

  it('usa el identificador cuando el establecimiento no está en la estructura', async () => {
    server.use(
      http.get(ventaUrl, () => HttpResponse.json({ ...sampleVenta, establecimientoId: 'est-x' }))
    );

    renderPage();

    expect((await screen.findAllByText('est-x')).length).toBeGreaterThan(0);
  });

  it('muestra la anulación solo en una venta anulada', async () => {
    server.use(http.get(ventaUrl, () => HttpResponse.json(sampleVentaAnulada)));
    const anulada = renderPage();

    expect(await screen.findByRole('heading', { name: 'Anulación' })).toBeInTheDocument();
    expect(within(seccion('Anulación')).getByText('Error de digitación')).toBeInTheDocument();
    expect(within(seccion('Anulación')).getByText('user-2')).toBeInTheDocument();
    anulada.unmount();

    server.use(http.get(ventaUrl, () => HttpResponse.json(sampleVenta)));
    renderPage();

    await screen.findByRole('heading', { name: 'Resumen' });
    expect(screen.queryByRole('heading', { name: 'Anulación' })).not.toBeInTheDocument();
  });

  it('muestra el comprobante con su botón de impresión', async () => {
    server.use(http.get(ventaUrl, () => HttpResponse.json(sampleVenta)));

    renderPage();

    expect(await screen.findByRole('button', { name: 'Imprimir comprobante' })).toBeInTheDocument();
    expect(screen.getByText('Comprobante interno — no válido como comprobante de pago')).toBeInTheDocument();
  });

  it('permite abrir y cerrar la vista previa e imprimir desde el encabezado', async () => {
    server.use(http.get(ventaUrl, () => HttpResponse.json(sampleVenta)));
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    const { user } = renderPage();
    const toggle = await screen.findByRole('button', { name: 'Ver comprobante' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await user.click(toggle);
    expect(screen.getByRole('button', { name: 'Ocultar comprobante' })).toHaveAttribute(
      'aria-expanded',
      'true'
    );
    await user.click(screen.getByRole('button', { name: 'Imprimir comprobante' }));
    expect(print).toHaveBeenCalledOnce();
    await user.click(screen.getByRole('button', { name: 'Ocultar comprobante' }));
    expect(screen.getByRole('button', { name: 'Ver comprobante' })).toHaveAttribute(
      'aria-expanded',
      'false'
    );
    print.mockRestore();
  });

  it('recupera el detalle al reintentar una carga fallida', async () => {
    let failed = true;
    server.use(
      http.get(ventaUrl, () =>
        failed ? new HttpResponse(null, { status: 500 }) : HttpResponse.json(sampleVenta)
      )
    );
    const { user } = renderPage();
    await screen.findByRole('alert');
    failed = false;
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(
      await screen.findByRole('heading', { name: 'Venta EST001-T01-000001' })
    ).toBeInTheDocument();
  });

  it('muestra el error de la API cuando la venta no existe', async () => {
    server.use(
      http.get(ventaUrl, () =>
        HttpResponse.json({ title: 'Venta no encontrada', status: 404 }, { status: 404 })
      )
    );

    renderPage();

    expect(
      await screen.findByText('El recurso no existe o no pertenece a tu organización.')
    ).toBeInTheDocument();
  });

  it('muestra Cargando… mientras llega la venta', async () => {
    server.use(http.get(ventaUrl, () => HttpResponse.json(sampleVenta)));

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    await screen.findByRole('heading', { name: 'Resumen' });
  });
});
