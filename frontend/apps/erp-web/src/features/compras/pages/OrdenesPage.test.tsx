import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleOrdenResumen, sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { OrdenesPage } from './OrdenesPage';

const ordenesUrl = '*/api/v1/compras/ordenes';
const proveedoresUrl = '*/api/v1/compras/proveedores';

beforeEach(() => {
  server.use(http.get(proveedoresUrl, () => HttpResponse.json(pagina([sampleProveedor]))));
});

function renderPage(entrada = '/compras/ordenes') {
  return renderRoute('/compras/ordenes', OrdenesPage, entrada);
}

function capturar() {
  const captura = { params: new URLSearchParams() };
  server.use(
    http.get(ordenesUrl, ({ request }) => {
      captura.params = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([sampleOrdenResumen], { totalElements: 120 }));
    })
  );
  return captura;
}

describe('OrdenesPage', () => {
  it('ofrece crear una orden nueva', async () => {
    server.use(http.get(ordenesUrl, () => HttpResponse.json(pagina([]))));

    renderPage();

    expect(await screen.findByRole('link', { name: 'Nueva orden' })).toHaveAttribute(
      'href',
      '/compras/ordenes/nueva'
    );
  });

  it('lista las órdenes con enlace al detalle', async () => {
    server.use(http.get(ordenesUrl, () => HttpResponse.json(pagina([sampleOrdenResumen]))));

    renderPage();

    expect(await screen.findByText('OC-2026-000001')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Órdenes de compra' })).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de la orden OC-2026-000001' })
    ).toHaveAttribute('href', '/compras/ordenes/orden-1');
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(ordenesUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay órdenes de compra con los filtros indicados.')
    ).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(ordenesUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderPage();

    expect(await screen.findByText('No se pudo cargar el listado de órdenes.')).toBeInTheDocument();
  });

  it('filtra por proveedor y estado y refleja los filtros en la URL', async () => {
    const captura = capturar();
    const { user, router } = renderPage();
    await screen.findByText('OC-2026-000001');

    await user.selectOptions(await screen.findByLabelText('Proveedor'), 'prov-1');
    await waitFor(() => expect(captura.params.get('proveedorId')).toBe('prov-1'));
    await user.selectOptions(screen.getByLabelText('Estado'), 'EMITIDA');
    await waitFor(() => expect(captura.params.get('estado')).toBe('EMITIDA'));

    expect(router.state.location.search).toBe('?proveedorId=prov-1&estado=EMITIDA');
  });

  it('lee los filtros iniciales de la URL y no envía los vacíos', async () => {
    const captura = capturar();

    renderPage('/compras/ordenes?estado=APROBADA&page=2&size=50');

    await waitFor(() => expect(captura.params.get('estado')).toBe('APROBADA'));
    expect(captura.params.get('page')).toBe('2');
    expect(captura.params.get('size')).toBe('50');
    expect(captura.params.has('proveedorId')).toBe(false);
  });

  it('pagina y cambia el tamaño de página', async () => {
    const captura = capturar();
    const { user } = renderPage();
    await screen.findByText('OC-2026-000001');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(captura.params.get('page')).toBe('1'));
    await user.selectOptions(screen.getByLabelText('Filas por página'), '50');
    await waitFor(() => expect(captura.params.get('size')).toBe('50'));
    expect(captura.params.get('page')).toBe('0');
  });
});
