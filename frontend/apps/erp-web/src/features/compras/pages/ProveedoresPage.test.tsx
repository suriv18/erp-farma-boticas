import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { ProveedoresPage } from './ProveedoresPage';

const proveedoresUrl = '*/api/v1/compras/proveedores';

function renderPage(entrada = '/compras/proveedores') {
  return renderRoute('/compras/proveedores', ProveedoresPage, entrada);
}

function capturar() {
  const captura = { params: new URLSearchParams() };
  server.use(
    http.get(proveedoresUrl, ({ request }) => {
      captura.params = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([sampleProveedor], { totalElements: 120 }));
    })
  );
  return captura;
}

describe('ProveedoresPage', () => {
  it('lista los proveedores con enlace al detalle y al alta', async () => {
    server.use(http.get(proveedoresUrl, () => HttpResponse.json(pagina([sampleProveedor]))));

    renderPage();

    expect(await screen.findByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Proveedores' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nuevo proveedor' })).toHaveAttribute(
      'href',
      '/compras/proveedores/nuevo'
    );
    expect(
      screen.getByRole('link', { name: 'Ver detalle de Laboratorios Perú SAC' })
    ).toHaveAttribute('href', '/compras/proveedores/prov-1');
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(proveedoresUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay proveedores con los filtros indicados.')
    ).toBeInTheDocument();
    primera.unmount();

    server.use(
      http.get(proveedoresUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 }))
    );
    renderPage();

    expect(
      await screen.findByText('No se pudo cargar el listado de proveedores.')
    ).toBeInTheDocument();
  });

  it('filtra por estado y texto y refleja los filtros en la URL', async () => {
    const captura = capturar();
    const { user, router } = renderPage();
    await screen.findByText('Laboratorios Perú SAC');

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await waitFor(() => expect(captura.params.get('estado')).toBe('SUSPENDIDO'));
    await user.type(screen.getByLabelText('Buscar proveedor'), 'lab');
    await waitFor(() => expect(captura.params.get('texto')).toBe('lab'));

    expect(router.state.location.search).toBe('?estado=SUSPENDIDO&texto=lab');
  });

  it('lee los filtros iniciales de la URL', async () => {
    const captura = capturar();

    renderPage('/compras/proveedores?estado=ACTIVO&texto=lab&page=2&size=50');

    await waitFor(() => expect(captura.params.get('estado')).toBe('ACTIVO'));
    expect(captura.params.get('texto')).toBe('lab');
    expect(captura.params.get('page')).toBe('2');
    expect(captura.params.get('size')).toBe('50');
    expect(screen.getByLabelText('Buscar proveedor')).toHaveValue('lab');
  });

  it('pagina y cambia el tamaño de página', async () => {
    const captura = capturar();
    const { user } = renderPage();
    await screen.findByText('Laboratorios Perú SAC');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(captura.params.get('page')).toBe('1'));
    await user.selectOptions(screen.getByLabelText('Filas por página'), '50');
    await waitFor(() => expect(captura.params.get('size')).toBe('50'));
    expect(captura.params.get('page')).toBe('0');
  });
});
