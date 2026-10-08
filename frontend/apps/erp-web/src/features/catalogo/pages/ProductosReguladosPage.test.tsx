import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { ProductosReguladosPage } from './ProductosReguladosPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/', Component: ProductosReguladosPage }]);
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

const sample = {
  id: 'pr-1',
  denominacion: 'Paracetamol 500 mg',
  condicionVentaCodigo: 'VL',
  estadoRegulatorio: 'VIGENTE'
};

function pagina(items: unknown[], extra: Record<string, number> = {}) {
  return { items, page: 0, size: 20, totalElements: items.length, ...extra };
}

describe('ProductosReguladosPage', () => {
  it('lista los productos regulados con enlace al detalle', async () => {
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados', () =>
        HttpResponse.json(
          pagina([
            sample,
            { ...sample, id: 'pr-2', denominacion: 'Sin condición', condicionVentaCodigo: null }
          ])
        )
      )
    );

    renderPage();

    expect(await screen.findByText('Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle de Paracetamol 500 mg' })).toHaveAttribute(
      'href',
      '/catalogo/productos-regulados/pr-1'
    );
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('informa cuando el listado falla', async () => {
    server.use(
      http.get(
        '*/api/v1/catalogo/productos-regulados',
        () => new HttpResponse(null, { status: 500 })
      )
    );

    renderPage();

    expect(
      await screen.findByText('No se pudo cargar el listado de productos regulados.')
    ).toBeInTheDocument();
  });

  it('filtra por texto, condicion de venta y estado regulatorio, y pagina en el servidor', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados', ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sample], { totalElements: 120 }));
      })
    );

    const { user } = renderPage();
    await screen.findByText('Paracetamol 500 mg');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));

    await user.type(screen.getByLabelText('Buscar producto regulado'), 'para');
    await waitFor(() => {
      expect(received.get('q')).toBe('para');
      expect(received.get('page')).toBe('0');
    });

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await screen.findByRole('option', { name: 'RM — Con receta médica' });
    await user.selectOptions(screen.getByLabelText('Condición de venta'), 'RM');
    await waitFor(() => {
      expect(received.get('condicionVentaCodigo')).toBe('RM');
      expect(received.get('page')).toBe('0');
    });

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await user.selectOptions(screen.getByLabelText('Estado regulatorio'), 'SUSPENDIDO');
    await waitFor(() => {
      expect(received.get('estadoRegulatorio')).toBe('SUSPENDIDO');
      expect(received.get('page')).toBe('0');
    });

    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => expect(received.get('size')).toBe('50'));
  });

  it('crea un producto regulado y refresca el listado', async () => {
    let created = false;
    let body: Record<string, unknown> = {};
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados', () =>
        HttpResponse.json(pagina(created ? [sample] : []))
      ),
      http.post('*/api/v1/catalogo/productos-regulados', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ id: 'pr-1' }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron productos regulados.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo producto regulado' }));
    await user.type(screen.getByLabelText('Tipo de producto'), 'FARMACEUTICO');
    await user.type(screen.getByLabelText('Denominación'), 'Paracetamol 500 mg');
    await user.click(screen.getByRole('button', { name: 'Crear producto regulado' }));

    expect(await screen.findByText('Paracetamol 500 mg')).toBeInTheDocument();
    expect(body).toEqual({ tipoProducto: 'FARMACEUTICO', denominacion: 'Paracetamol 500 mg' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y permite cerrar el dialogo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados', () => HttpResponse.json(pagina([]))),
      http.post('*/api/v1/catalogo/productos-regulados', () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'El número de registro ya existe.' },
          { status: 409 }
        )
      )
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron productos regulados.');

    await user.click(screen.getByRole('button', { name: 'Nuevo producto regulado' }));
    await user.type(screen.getByLabelText('Tipo de producto'), 'FARMACEUTICO');
    await user.type(screen.getByLabelText('Denominación'), 'Paracetamol 500 mg');
    await user.click(screen.getByRole('button', { name: 'Crear producto regulado' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('El número de registro ya existe.');

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
