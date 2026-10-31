import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { CategoriasPage } from './CategoriasPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/', Component: CategoriasPage }]);
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

const sampleCategoria = {
  id: 'categoria-1',
  tenantId: 'tenant-1',
  categoriaPadreId: null,
  codigo: 'ANALGESICOS',
  nombre: 'Analgésicos',
  descripcion: null,
  nivel: 1,
  orden: 1,
  estado: 'ACTIVO'
};

function paginaResponse(
  items: unknown[],
  overrides: Partial<{ page: number; size: number; totalElements: number }> = {}
) {
  return { items, page: 0, size: 20, totalElements: items.length, ...overrides };
}

describe('CategoriasPage', () => {
  it('lista las categorias con columna N°', async () => {
    server.use(
      http.get('*/api/v1/catalogo/categorias', () =>
        HttpResponse.json(paginaResponse([sampleCategoria]))
      )
    );

    renderPage();

    expect(await screen.findByText('Analgésicos')).toBeInTheDocument();
    const row = screen.getByText('Analgésicos').closest('tr');
    expect(row).not.toBeNull();
    expect(row).toHaveTextContent('1');
  });

  it('crea una categoria nueva y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/categorias', () =>
        HttpResponse.json(paginaResponse(created ? [sampleCategoria] : []))
      ),
      http.post('*/api/v1/catalogo/categorias', () => {
        created = true;
        return HttpResponse.json(sampleCategoria, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron categorías.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nueva categoría' }));
    await user.type(screen.getByLabelText('Código'), 'ANALGESICOS');
    await user.type(screen.getByLabelText('Nombre'), 'Analgésicos');
    await user.click(screen.getByRole('button', { name: 'Crear categoría' }));

    await waitFor(() => expect(screen.getByText('Analgésicos')).toBeInTheDocument());
  });

  it('busca por texto y envia el parametro q', async () => {
    let receivedQ: string | null = null;
    server.use(
      http.get('*/api/v1/catalogo/categorias', ({ request }) => {
        receivedQ = new URL(request.url).searchParams.get('q');
        return HttpResponse.json(paginaResponse(receivedQ === 'analg' ? [sampleCategoria] : []));
      })
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron categorías.');

    await user.type(screen.getByLabelText('Buscar categoría'), 'analg');

    await waitFor(() => expect(receivedQ).toBe('analg'));
    expect(await screen.findByText('Analgésicos')).toBeInTheDocument();
  });

  it('edita una categoria existente', async () => {
    let currentNombre = 'Analgésicos';
    server.use(
      http.get('*/api/v1/catalogo/categorias', () =>
        HttpResponse.json(paginaResponse([{ ...sampleCategoria, nombre: currentNombre }]))
      ),
      http.put('*/api/v1/catalogo/categorias/categoria-1', async ({ request }) => {
        const body = (await request.json()) as { nombre: string };
        currentNombre = body.nombre;
        return HttpResponse.json({ ...sampleCategoria, nombre: currentNombre });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Analgésicos');

    await user.click(screen.getByRole('button', { name: 'Editar Analgésicos' }));
    const nombreInput = screen.getByLabelText('Nombre');
    await user.clear(nombreInput);
    await user.type(nombreInput, 'Analgésicos y antipiréticos');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() =>
      expect(screen.getByText('Analgésicos y antipiréticos')).toBeInTheDocument()
    );
  });

  it('cambia el estado de una categoria activa a inactiva', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/categorias', () =>
        HttpResponse.json(paginaResponse([{ ...sampleCategoria, estado: currentEstado }]))
      ),
      http.patch('*/api/v1/catalogo/categorias/categoria-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Analgésicos');

    await user.click(screen.getByRole('button', { name: 'Desactivar Analgésicos' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
