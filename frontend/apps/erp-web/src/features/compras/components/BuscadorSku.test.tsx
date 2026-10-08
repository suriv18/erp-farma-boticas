import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { BuscadorSku } from './BuscadorSku';

const skusUrl = '*/api/v1/catalogo/skus';

function renderBuscador() {
  const onElegir = vi.fn();
  const Pantalla = () => <BuscadorSku onElegir={onElegir} />;
  return { onElegir, ...renderRoute('/x', Pantalla, '/x') };
}

describe('BuscadorSku', () => {
  it('no consulta antes de buscar', () => {
    const consulta = vi.fn();
    server.use(
      http.get(skusUrl, () => {
        consulta();
        return HttpResponse.json(pagina([]));
      })
    );

    renderBuscador();

    expect(consulta).not.toHaveBeenCalled();
  });

  it('busca activos por el texto y entrega el SKU elegido', async () => {
    const parametros = vi.fn<(busqueda: URLSearchParams) => void>();
    server.use(
      http.get(skusUrl, ({ request }) => {
        parametros(new URL(request.url).searchParams);
        return HttpResponse.json(pagina([sampleSkuVenta]));
      })
    );
    const { onElegir, user } = renderBuscador();

    await user.type(screen.getByLabelText('Buscar producto'), '  paracetamol ');
    await user.click(screen.getByRole('button', { name: 'Buscar' }));
    await user.click(await screen.findByRole('button', { name: 'Agregar MED-001' }));

    expect(screen.getByText('MED-001 — Paracetamol 500 mg')).toBeInTheDocument();
    expect(parametros.mock.calls[0]?.[0].get('q')).toBe('paracetamol');
    expect(parametros.mock.calls[0]?.[0].get('estado')).toBe('ACTIVO');
    expect(onElegir).toHaveBeenCalledWith(sampleSkuVenta);
  });

  it('muestra el vacío y el error de búsqueda', async () => {
    server.use(http.get(skusUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderBuscador();

    await primera.user.type(screen.getByLabelText('Buscar producto'), 'zzz');
    await primera.user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByText('No se encontraron productos.')).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(skusUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    const segunda = renderBuscador();
    await segunda.user.type(screen.getByLabelText('Buscar producto'), 'zzz');
    await segunda.user.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByText('No se pudo buscar productos.')).toBeInTheDocument();
  });
});
