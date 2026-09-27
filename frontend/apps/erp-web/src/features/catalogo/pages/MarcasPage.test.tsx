import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { MarcasPage } from './MarcasPage';

const authenticatedSession = {
  status: 'authenticated' as const,
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/', Component: MarcasPage }]);
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <RouterProvider router={router} />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

const sampleMarca = {
  id: 'marca-1',
  tenantId: 'tenant-1',
  codigo: 'BAYER',
  nombre: 'Bayer',
  descripcion: null,
  estado: 'ACTIVO'
};

function paginaResponse(
  items: unknown[],
  overrides: Partial<{ page: number; size: number; totalElements: number }> = {}
) {
  return { items, page: 0, size: 20, totalElements: items.length, ...overrides };
}

describe('MarcasPage', () => {
  it('pagina en servidor, cambia tamaño y reinicia la página al buscar', async () => {
    let received: URLSearchParams;
    server.use(
      http.get('*/api/v1/catalogo/marcas', ({ request }) => {
        received = new URL(request.url).searchParams;
        const page = Number(received.get('page'));
        const size = Number(received.get('size'));
        return HttpResponse.json(paginaResponse([sampleMarca], { page, size, totalElements: 120 }));
      })
    );
    const { user } = renderPage();
    await screen.findByText('Bayer');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await screen.findByText('Página 2 de 6');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => {
      expect(received.get('size')).toBe('50');
      expect(received.get('page')).toBe('0');
    });
    await screen.findByText('Página 1 de 3');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await screen.findByText('Página 2 de 3');
    await user.type(screen.getByLabelText('Buscar marca'), 'bay');
    await waitFor(() => {
      expect(received.get('q')).toBe('bay');
      expect(received.get('page')).toBe('0');
    });
  });
  it('lista las marcas del tenant activo con columna N°', async () => {
    server.use(
      http.get('*/api/v1/catalogo/marcas', () => HttpResponse.json(paginaResponse([sampleMarca])))
    );

    renderPage();

    expect(await screen.findByText('Bayer')).toBeInTheDocument();
    const row = screen.getByText('Bayer').closest('tr');
    expect(row).not.toBeNull();
    expect(row).toHaveTextContent('1');
  });

  it('crea una marca nueva y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/marcas', () =>
        HttpResponse.json(paginaResponse(created ? [sampleMarca] : []))
      ),
      http.post('*/api/v1/catalogo/marcas', () => {
        created = true;
        return HttpResponse.json(sampleMarca, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron marcas.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nueva marca' }));
    await user.type(screen.getByLabelText('Código'), 'BAYER');
    await user.type(screen.getByLabelText('Nombre'), 'Bayer');
    await user.click(screen.getByRole('button', { name: 'Crear marca' }));

    await waitFor(() => expect(screen.getByText('Bayer')).toBeInTheDocument());
  });

  it('busca por texto y envia el parametro q', async () => {
    let receivedQ: string | null = null;
    server.use(
      http.get('*/api/v1/catalogo/marcas', ({ request }) => {
        receivedQ = new URL(request.url).searchParams.get('q');
        return HttpResponse.json(paginaResponse(receivedQ === 'bay' ? [sampleMarca] : []));
      })
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron marcas.');

    await user.type(screen.getByLabelText('Buscar marca'), 'bay');

    await waitFor(() => expect(receivedQ).toBe('bay'));
    expect(await screen.findByText('Bayer')).toBeInTheDocument();
  });

  it('edita una marca existente', async () => {
    let currentNombre = 'Bayer';
    server.use(
      http.get('*/api/v1/catalogo/marcas', () =>
        HttpResponse.json(paginaResponse([{ ...sampleMarca, nombre: currentNombre }]))
      ),
      http.put('*/api/v1/catalogo/marcas/marca-1', async ({ request }) => {
        const body = (await request.json()) as { nombre: string };
        currentNombre = body.nombre;
        return HttpResponse.json({ ...sampleMarca, nombre: currentNombre });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Bayer');

    await user.click(screen.getByRole('button', { name: 'Editar Bayer' }));
    const nombreInput = screen.getByLabelText('Nombre');
    await user.clear(nombreInput);
    await user.type(nombreInput, 'Bayer S.A.');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByText('Bayer S.A.')).toBeInTheDocument());
  });

  it('cambia el estado de una marca activa a inactiva', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/marcas', () =>
        HttpResponse.json(paginaResponse([{ ...sampleMarca, estado: currentEstado }]))
      ),
      http.patch('*/api/v1/catalogo/marcas/marca-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Bayer');

    await user.click(screen.getByRole('button', { name: 'Desactivar Bayer' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
