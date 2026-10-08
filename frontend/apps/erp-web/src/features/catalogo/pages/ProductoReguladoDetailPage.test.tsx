import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { PRODUCTO_REGULADO_VACIO } from '../lib/producto-regulado-form';
import { ProductoReguladoDetailPage } from './ProductoReguladoDetailPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter(
    [
      {
        path: '/catalogo/productos-regulados/:productoReguladoId',
        Component: ProductoReguladoDetailPage
      }
    ],
    { initialEntries: ['/catalogo/productos-regulados/pr-1'] }
  );
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

function productoApi(overrides: Record<string, unknown> = {}) {
  const vacio = Object.fromEntries(
    Object.keys(PRODUCTO_REGULADO_VACIO).map((clave) => [clave, null])
  );
  return {
    ...vacio,
    id: 'pr-1',
    tipoProducto: 'FARMACEUTICO',
    denominacion: 'Paracetamol 500 mg',
    fabricante: 'Farmalab',
    principiosActivos: [],
    estadoRegulatorio: 'VIGENTE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: null,
    ...overrides
  };
}

function usarCatalogosBase() {
  server.use(
    http.get('*/api/v1/catalogo/principios-activos', () => HttpResponse.json([])),
    http.get('*/api/v1/catalogo/productos-regulados/pr-1/principios-activos', () =>
      HttpResponse.json([])
    )
  );
}

describe('ProductoReguladoDetailPage', () => {
  it('muestra los datos del producto con marcador para los vacios', async () => {
    usarCatalogosBase();
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados/pr-1', () => HttpResponse.json(productoApi()))
    );

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Paracetamol 500 mg' })).toBeInTheDocument();
    expect(screen.getByText('Farmalab')).toBeInTheDocument();
    expect(screen.getByText('VIGENTE')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(10);
    expect(screen.getByRole('link', { name: 'Catálogo / Productos regulados' })).toHaveAttribute(
      'href',
      '/catalogo/productos-regulados'
    );
  });

  it('muestra el error cuando el producto no existe', async () => {
    usarCatalogosBase();
    server.use(
      http.get(
        '*/api/v1/catalogo/productos-regulados/pr-1',
        () => new HttpResponse(null, { status: 404 })
      )
    );

    renderPage();

    expect(
      await screen.findByText('El recurso no existe o no pertenece a tu organización.')
    ).toBeInTheDocument();
  });

  it('edita el producto y refresca los datos', async () => {
    usarCatalogosBase();
    let denominacion = 'Paracetamol 500 mg';
    let body: Record<string, unknown> = {};
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados/pr-1', () =>
        HttpResponse.json(productoApi({ denominacion }))
      ),
      http.put('*/api/v1/catalogo/productos-regulados/pr-1', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        denominacion = body.denominacion as string;
        return HttpResponse.json(productoApi({ denominacion }));
      })
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    const campo = screen.getByLabelText('Denominación');
    expect(campo).toHaveValue('Paracetamol 500 mg');
    await user.clear(campo);
    await user.type(campo, 'Paracetamol 1 g');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('heading', { name: 'Paracetamol 1 g' })).toBeInTheDocument();
    expect(body).toEqual({
      tipoProducto: 'FARMACEUTICO',
      denominacion: 'Paracetamol 1 g',
      fabricante: 'Farmalab'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al editar y permite cerrar el dialogo', async () => {
    usarCatalogosBase();
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados/pr-1', () =>
        HttpResponse.json(productoApi())
      ),
      http.put(
        '*/api/v1/catalogo/productos-regulados/pr-1',
        () => new HttpResponse(null, { status: 403 })
      )
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('cambia el estado regulatorio', async () => {
    usarCatalogosBase();
    let estado = 'VIGENTE';
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados/pr-1', () =>
        HttpResponse.json(productoApi({ estadoRegulatorio: estado }))
      ),
      http.patch('*/api/v1/catalogo/productos-regulados/pr-1/estado', async ({ request }) => {
        estado = ((await request.json()) as { status: string }).status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(screen.getByText('SUSPENDIDO')).toBeInTheDocument());
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del cambio de estado y permite cerrar el dialogo', async () => {
    usarCatalogosBase();
    server.use(
      http.get('*/api/v1/catalogo/productos-regulados/pr-1', () =>
        HttpResponse.json(productoApi())
      ),
      http.patch('*/api/v1/catalogo/productos-regulados/pr-1/estado', () =>
        HttpResponse.json({ title: 'Inválido', detail: 'Estado no permitido.' }, { status: 400 })
      )
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'CANCELADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));
    expect(await screen.findByText('Estado no permitido.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
