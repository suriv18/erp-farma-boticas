import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { delay, http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { samplePosicion } from '../../../test/inventario-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { StockDisponible } from './StockDisponible';

function renderStock(almacenId = 'alm-1') {
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <StockDisponible almacenId={almacenId} skuId="sku-0001-aaaa" />
    </QueryClientProvider>
  );
}

describe('StockDisponible', () => {
  it('suma solo las posiciones vendibles y filtra por almacén y SKU', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get('*/api/v1/inventario/posiciones', ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([
            samplePosicion,
            { ...samplePosicion, id: 'pos-2', cantidadDisponible: 5, vendible: false }
          ])
        );
      })
    );
    renderStock();

    expect(await screen.findByText('Stock: 90')).toBeInTheDocument();
    expect(received.get('almacenId')).toBe('alm-1');
    expect(received.get('skuId')).toBe('sku-0001-aaaa');
  });

  it('muestra cero sin posiciones', async () => {
    server.use(http.get('*/api/v1/inventario/posiciones', () => HttpResponse.json(pagina([]))));
    renderStock();

    expect(await screen.findByText('Stock: 0')).toBeInTheDocument();
  });

  it('indica la carga mientras consulta', async () => {
    server.use(
      http.get('*/api/v1/inventario/posiciones', async () => {
        await delay(50);
        return HttpResponse.json(pagina([]));
      })
    );
    renderStock();

    expect(screen.getByText('Stock: …')).toBeInTheDocument();
    expect(await screen.findByText('Stock: 0')).toBeInTheDocument();
  });

  it('muestra guion ante un error', async () => {
    server.use(
      http.get('*/api/v1/inventario/posiciones', () => HttpResponse.json({}, { status: 500 }))
    );
    renderStock();

    expect(await screen.findByText('Stock: —')).toBeInTheDocument();
  });

  it('pide un almacén y no consulta sin él', () => {
    const consulta = vi.fn();
    server.use(
      http.get('*/api/v1/inventario/posiciones', () => {
        consulta();
        return HttpResponse.json(pagina([]));
      })
    );
    renderStock('');

    expect(screen.getByText('Selecciona un almacén para ver el stock.')).toBeInTheDocument();
    expect(consulta).not.toHaveBeenCalled();
  });
});
