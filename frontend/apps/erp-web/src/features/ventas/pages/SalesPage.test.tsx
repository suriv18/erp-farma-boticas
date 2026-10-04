import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { sampleVentaResumen } from '../../../test/ventas-fixtures';
import { renderRoute } from '../../../test/render-route';
import { finDelDia, inicioDelDia } from '../lib/fechas';
import { SalesPage } from './SalesPage';

const ventasUrl = '*/api/v1/ventas/ventas';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura))
  );
});

function renderPage(entrada = '/ventas') {
  return renderRoute('/ventas', SalesPage, entrada);
}

function capturar() {
  const captura = { params: new URLSearchParams() };
  server.use(
    http.get(ventasUrl, ({ request }) => {
      captura.params = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([sampleVentaResumen], { totalElements: 120 }));
    })
  );
  return captura;
}

describe('SalesPage', () => {
  it('lista las ventas con número, total y enlace al detalle', async () => {
    server.use(http.get(ventasUrl, () => HttpResponse.json(pagina([sampleVentaResumen]))));

    renderPage();

    expect(await screen.findByText('EST001-T01-000001')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Ventas' })).toBeInTheDocument();
    expect(screen.getByText(/25[.,]00/)).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de la venta EST001-T01-000001' })
    ).toHaveAttribute('href', '/ventas/venta-1');
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(ventasUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByText('No hay ventas con los filtros indicados.')).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(ventasUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderPage();

    expect(
      await screen.findByText('No se pudo cargar el historial de ventas.')
    ).toBeInTheDocument();
  });

  it('filtra por establecimiento y fechas y refleja los filtros en la URL', async () => {
    const captura = capturar();
    const { user, router } = renderPage();
    await screen.findByText('EST001-T01-000001');

    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');
    await waitFor(() => expect(captura.params.get('establecimientoId')).toBe('est-1'));
    await user.type(screen.getByLabelText('Desde'), '2026-10-01');
    await waitFor(() => expect(captura.params.get('desde')).toBe(inicioDelDia('2026-10-01')));
    await user.type(screen.getByLabelText('Hasta'), '2026-10-03');
    await waitFor(() => expect(captura.params.get('hasta')).toBe(finDelDia('2026-10-03')));

    expect(router.state.location.search).toBe(
      '?establecimientoId=est-1&desde=2026-10-01&hasta=2026-10-03'
    );
  });

  it('no envía desde ni hasta cuando no hay fechas', async () => {
    const captura = capturar();

    renderPage();
    await screen.findByText('EST001-T01-000001');

    expect(captura.params.has('desde')).toBe(false);
    expect(captura.params.has('hasta')).toBe(false);
  });

  it('pagina enviando la página solicitada', async () => {
    const captura = capturar();
    const { user } = renderPage();
    await screen.findByText('EST001-T01-000001');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    await waitFor(() => expect(captura.params.get('page')).toBe('1'));
  });
});
