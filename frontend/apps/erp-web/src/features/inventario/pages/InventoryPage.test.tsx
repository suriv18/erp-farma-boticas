import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  sampleEstructura,
  sampleMovimiento,
  samplePosicion,
  sampleSku
} from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { InventoryPage } from './InventoryPage';

const posicionesUrl = '*/api/v1/inventario/posiciones';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku])))
  );
});

function renderPage(entrada = '/inventario') {
  return renderRoute('/inventario', InventoryPage, entrada);
}

describe('InventoryPage', () => {
  it('lista las posiciones con cantidades, estado y acciones', async () => {
    server.use(http.get(posicionesUrl, () => HttpResponse.json(pagina([samplePosicion]))));

    renderPage();

    expect(await screen.findByText('L001')).toBeInTheDocument();
    expect(screen.getByText('HABILITADO')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle del lote L001' })).toHaveAttribute(
      'href',
      '/inventario/lotes/lote-1'
    );
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(posicionesUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay stock registrado con los filtros indicados.')
    ).toBeInTheDocument();
    primera.unmount();

    server.use(
      http.get(posicionesUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 }))
    );
    renderPage();

    expect(await screen.findByText('No se pudo cargar el inventario.')).toBeInTheDocument();
  });

  it('filtra por establecimiento, almacén y SKU y refleja los filtros en la URL', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(posicionesUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([samplePosicion]));
      })
    );
    const { user, router } = renderPage();
    await screen.findByText('L001');

    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');
    await waitFor(() => expect(received.get('establecimientoId')).toBe('est-1'));
    const almacen = screen.getByLabelText('Almacén');
    expect(
      within(almacen).queryByRole('option', { name: 'Almacén Norte' })
    ).not.toBeInTheDocument();
    await user.selectOptions(almacen, 'alm-2');
    await waitFor(() => expect(received.get('almacenId')).toBe('alm-2'));
    await user.selectOptions(
      screen.getByLabelText('SKU'),
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    );
    await waitFor(() => expect(received.get('skuId')).toBe('sku-0001-aaaa'));

    expect(router.state.location.search).toBe(
      '?establecimientoId=est-1&almacenId=alm-2&skuId=sku-0001-aaaa'
    );
  });

  it('pagina y cambia el tamaño de página', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(posicionesUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([samplePosicion], { totalElements: 120 }));
      })
    );
    const { user } = renderPage();
    await screen.findByText('L001');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => expect(received.get('size')).toBe('50'));
    expect(received.get('page')).toBe('0');
  });

  it('abre el ajuste de una fila y lo registra', async () => {
    server.use(
      http.get(posicionesUrl, () => HttpResponse.json(pagina([samplePosicion]))),
      http.post('*/api/v1/inventario/movimientos', () =>
        HttpResponse.json(sampleMovimiento, { status: 201 })
      )
    );
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Ajustar stock del lote L001' }));
    const dialogo = screen.getByRole('dialog');
    await user.type(within(dialogo).getByLabelText('Cantidad'), '5');
    await user.type(within(dialogo).getByLabelText('Motivo'), 'Conteo');
    await user.click(within(dialogo).getByRole('button', { name: 'Registrar ajuste' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('abre y cierra el diálogo de ingreso con lote nuevo desde la cabecera', async () => {
    server.use(http.get(posicionesUrl, () => HttpResponse.json(pagina([samplePosicion]))));
    const { user } = renderPage();
    await screen.findByText('L001');

    await user.click(screen.getByRole('button', { name: 'Registrar ingreso' }));
    expect(
      screen.getByRole('heading', { name: 'Registrar ingreso con lote nuevo' })
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
