import { delay, http, HttpResponse } from 'msw';
import { screen, waitFor } from '@testing-library/react';
import { server } from '../../../test/mocks/server';
import { samplePosicion } from '../../../test/inventario-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { sampleSkuFraccionable, sampleSkuVenta } from '../../../test/ventas-fixtures';
import type { SkuResumen } from '../../catalogo';
import { BuscadorProductos } from './BuscadorProductos';

function renderBuscador(deshabilitado = false) {
  const onElegir = vi.fn();
  const Pantalla = () => (
    <BuscadorProductos almacenId="alm-1" deshabilitado={deshabilitado} onElegir={onElegir} />
  );
  return { onElegir, ...renderRoute('/pos', Pantalla, '/pos') };
}

function mockSkus(items: SkuResumen[]) {
  const consulta = vi.fn();
  server.use(
    http.get('*/api/v1/catalogo/skus', ({ request }) => {
      consulta(new URL(request.url).searchParams);
      return HttpResponse.json(pagina(items));
    }),
    http.get('*/api/v1/inventario/posiciones', () => HttpResponse.json(pagina([samplePosicion])))
  );
  return consulta;
}

async function buscar(user: ReturnType<typeof renderBuscador>['user'], termino: string) {
  await user.type(await screen.findByLabelText('Buscar producto'), termino);
  await user.click(screen.getByRole('button', { name: 'Buscar' }));
}

describe('BuscadorProductos', () => {
  it('busca al enviar y muestra código, precio y stock', async () => {
    const consulta = mockSkus([sampleSkuVenta]);
    const { user } = renderBuscador();

    await buscar(user, '  paracetamol ');

    expect(await screen.findByRole('button', { name: 'Agregar MED-001' })).toBeInTheDocument();
    expect(screen.getByText('MED-001 — Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByText('S/ 12.50')).toBeInTheDocument();
    expect(await screen.findByText('Stock: 90')).toBeInTheDocument();
    const params = consulta.mock.calls[0]?.[0] as URLSearchParams;
    expect(params.get('q')).toBe('paracetamol');
    expect(params.get('estado')).toBe('ACTIVO');
  });

  it('muestra Sin precio cuando el SKU no tiene precio de referencia', async () => {
    mockSkus([sampleSkuFraccionable]);
    const { user } = renderBuscador();

    await buscar(user, 'jarabe');

    expect(await screen.findByText('Sin precio')).toBeInTheDocument();
  });

  it('entrega el SKU al pulsar Agregar', async () => {
    mockSkus([sampleSkuVenta]);
    const { user, onElegir } = renderBuscador();

    await buscar(user, 'paracetamol');
    await user.click(await screen.findByRole('button', { name: 'Agregar MED-001' }));

    expect(onElegir).toHaveBeenCalledWith(sampleSkuVenta);
  });

  it('agrega automáticamente un único resultado por código de barras y limpia el campo', async () => {
    mockSkus([sampleSkuVenta]);
    const { user, onElegir } = renderBuscador();

    await buscar(user, '7750001234567');

    await waitFor(() => expect(onElegir).toHaveBeenCalledTimes(1));
    expect(onElegir).toHaveBeenCalledWith(sampleSkuVenta);
    expect(screen.getByLabelText('Buscar producto')).toHaveValue('');
    expect(screen.queryByRole('button', { name: 'Agregar MED-001' })).not.toBeInTheDocument();
  });

  it('no agrega automáticamente cuando hay varios resultados por código de barras', async () => {
    mockSkus([sampleSkuVenta, sampleSkuFraccionable]);
    const { user, onElegir } = renderBuscador();

    await buscar(user, '7750001234567');

    expect(await screen.findByRole('button', { name: 'Agregar JAR-002' })).toBeInTheDocument();
    expect(onElegir).not.toHaveBeenCalled();
  });

  it('no agrega automáticamente un único resultado de texto libre', async () => {
    mockSkus([sampleSkuVenta]);
    const { user, onElegir } = renderBuscador();

    await buscar(user, 'paracetamol');

    expect(await screen.findByRole('button', { name: 'Agregar MED-001' })).toBeInTheDocument();
    expect(onElegir).not.toHaveBeenCalled();
  });

  it('informa cuando no hay resultados', async () => {
    mockSkus([]);
    const { user } = renderBuscador();

    await buscar(user, 'inexistente');

    expect(await screen.findByText('No se encontraron productos.')).toBeInTheDocument();
  });

  it('no muestra el vacío mientras consulta', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus', async () => {
        await delay(50);
        return HttpResponse.json(pagina([]));
      })
    );
    const { user } = renderBuscador();

    await buscar(user, 'lento');

    expect(screen.queryByText('No se encontraron productos.')).not.toBeInTheDocument();
    expect(await screen.findByText('No se encontraron productos.')).toBeInTheDocument();
  });

  it('informa cuando la búsqueda falla', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json({}, { status: 500 })));
    const { user } = renderBuscador();

    await buscar(user, 'paracetamol');

    expect(await screen.findByText('No se pudo buscar productos.')).toBeInTheDocument();
  });

  it('informa el error cuando falla la búsqueda por código de barras', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json({}, { status: 500 })));
    const { user, onElegir } = renderBuscador();

    await buscar(user, '7750001234567');

    expect(await screen.findByText('No se pudo buscar productos.')).toBeInTheDocument();
    expect(onElegir).not.toHaveBeenCalled();
  });

  it('deshabilita Agregar cuando el buscador está deshabilitado', async () => {
    mockSkus([sampleSkuVenta]);
    const { user } = renderBuscador(true);

    await buscar(user, 'paracetamol');

    expect(await screen.findByRole('button', { name: 'Agregar MED-001' })).toBeDisabled();
  });

  it('no consulta nada antes de buscar ni con un término vacío', async () => {
    const consulta = mockSkus([sampleSkuVenta]);
    const { user } = renderBuscador();

    expect(consulta).not.toHaveBeenCalled();
    expect(screen.queryByText('No se encontraron productos.')).not.toBeInTheDocument();

    await user.click(await screen.findByRole('button', { name: 'Buscar' }));

    expect(consulta).not.toHaveBeenCalled();
  });
});
