import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import { sampleSku } from '../../../test/inventario-fixtures';
import { SkuSelect } from './SkuSelect';

function renderSelect(props: Partial<Parameters<typeof SkuSelect>[0]> = {}) {
  const onChange = vi.fn();
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <SkuSelect
        id="sku"
        label="SKU"
        placeholder="Todos"
        search=""
        value=""
        onChange={onChange}
        {...props}
      />
    </QueryClientProvider>
  );
  return onChange;
}

describe('SkuSelect', () => {
  it('lista los SKU activos que coinciden con la búsqueda y notifica la selección', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get('*/api/v1/catalogo/skus', ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleSku]));
      })
    );
    const onChange = renderSelect({ search: 'para' });

    await userEvent.selectOptions(
      await screen.findByRole('combobox', { name: 'SKU' }),
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    );

    expect(received.get('q')).toBe('para');
    expect(received.get('estado')).toBe('ACTIVO');
    expect(received.get('size')).toBe('50');
    expect(onChange).toHaveBeenCalledWith('sku-0001-aaaa');
  });

  it('conserva el SKU seleccionado aunque no esté en los resultados', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([]))));
    renderSelect({ value: 'abcdef0123456789' });

    expect(await screen.findByRole('option', { name: 'abcdef01' })).toBeInTheDocument();
  });

  it('no duplica la opción cuando el SKU seleccionado está en los resultados', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku]))));
    renderSelect({ value: sampleSku.id });

    expect(
      await screen.findByRole('option', { name: 'MED-001 — Paracetamol 500 mg' })
    ).toBeInTheDocument();
    expect(screen.getAllByRole('option')).toHaveLength(2);
  });

  it('muestra el error del campo', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku]))));
    renderSelect({ error: 'Selecciona un SKU.' });

    expect(await screen.findByText('Selecciona un SKU.')).toBeInTheDocument();
  });
});
