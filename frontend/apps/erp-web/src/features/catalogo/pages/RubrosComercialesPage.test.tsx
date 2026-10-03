import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { RubrosComercialesPage } from './RubrosComercialesPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/', Component: RubrosComercialesPage }]);
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

const sampleRubro = {
  id: 'rubro-1',
  tenantId: 'tenant-1',
  codigo: 'FARMA',
  nombre: 'Productos Farmacéuticos',
  descripcion: null,
  esFarmaceutico: true,
  orden: 1,
  estado: 'ACTIVO'
};

function paginaResponse(
  items: unknown[],
  overrides: Partial<{ page: number; size: number; totalElements: number }> = {}
) {
  return { items, page: 0, size: 20, totalElements: items.length, ...overrides };
}

describe('RubrosComercialesPage', () => {
  it('crea un rubro comercial sin enviar tenantId', async () => {
    let receivedCreateBody: Record<string, unknown> | undefined;
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () => HttpResponse.json(paginaResponse([]))),
      http.post('*/api/v1/catalogo/rubros-comerciales', async ({ request }) => {
        receivedCreateBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(sampleRubro, { status: 201 });
      })
    );

    const { user } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Nuevo rubro comercial' }));
    await user.type(screen.getByLabelText('Código'), 'FARMA');
    await user.type(screen.getByLabelText('Nombre'), 'Otro rubro');
    await user.click(screen.getByRole('button', { name: 'Crear rubro comercial' }));

    await waitFor(() => expect(receivedCreateBody).toMatchObject({ codigo: 'FARMA' }));
    expect(receivedCreateBody).not.toHaveProperty('tenantId');
  });

  it('lista los rubros comerciales', async () => {
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([sampleRubro]))
      )
    );

    renderPage();

    expect(await screen.findByText('Productos Farmacéuticos')).toBeInTheDocument();
  });

  it('crea un rubro comercial nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse(created ? [sampleRubro] : []))
      ),
      http.post('*/api/v1/catalogo/rubros-comerciales', () => {
        created = true;
        return HttpResponse.json(sampleRubro, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron rubros comerciales.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo rubro comercial' }));
    await user.type(screen.getByLabelText('Código'), 'FARMA');
    await user.type(screen.getByLabelText('Nombre'), 'Productos Farmacéuticos');
    await user.type(screen.getByLabelText('Descripción'), 'Rubro exclusivo de farmacia.');
    await user.click(screen.getByRole('button', { name: 'Crear rubro comercial' }));

    await waitFor(() => expect(screen.getByText('Productos Farmacéuticos')).toBeInTheDocument());
  });

  it('crea un rubro comercial no farmaceutico', async () => {
    const noFarmaceutico = { ...sampleRubro, id: 'rubro-2', esFarmaceutico: false };
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse(created ? [noFarmaceutico] : []))
      ),
      http.post('*/api/v1/catalogo/rubros-comerciales', async ({ request }) => {
        created = true;
        const body = (await request.json()) as { esFarmaceutico: boolean };
        expect(body.esFarmaceutico).toBe(false);
        return HttpResponse.json(noFarmaceutico, { status: 201 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron rubros comerciales.');

    await user.click(screen.getByRole('button', { name: 'Nuevo rubro comercial' }));
    await user.type(screen.getByLabelText('Código'), 'FARMA');
    await user.type(screen.getByLabelText('Nombre'), 'Productos Farmacéuticos');
    await user.click(screen.getByRole('button', { name: 'Crear rubro comercial' }));

    await waitFor(() => expect(screen.getAllByText('No').length).toBeGreaterThan(0));
  });

  it('edita un rubro comercial existente', async () => {
    let currentNombre = 'Productos Farmacéuticos';
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([{ ...sampleRubro, nombre: currentNombre }]))
      ),
      http.put('*/api/v1/catalogo/rubros-comerciales/rubro-1', async ({ request }) => {
        const body = (await request.json()) as { nombre: string };
        currentNombre = body.nombre;
        return HttpResponse.json({ ...sampleRubro, nombre: currentNombre });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Productos Farmacéuticos');

    await user.click(screen.getByRole('button', { name: 'Editar Productos Farmacéuticos' }));
    const nombreInput = screen.getByLabelText('Nombre');
    await user.clear(nombreInput);
    await user.type(nombreInput, 'Productos Farmacéuticos y afines');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() =>
      expect(screen.getByText('Productos Farmacéuticos y afines')).toBeInTheDocument()
    );
  });

  it('busca por texto y envia el parametro q', async () => {
    let receivedQ: string | null = null;
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', ({ request }) => {
        receivedQ = new URL(request.url).searchParams.get('q');
        return HttpResponse.json(paginaResponse(receivedQ === 'farma' ? [sampleRubro] : []));
      })
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron rubros comerciales.');

    await user.type(screen.getByLabelText('Buscar rubro comercial'), 'farma');

    await waitFor(() => expect(receivedQ).toBe('farma'));
    expect(await screen.findByText('Productos Farmacéuticos')).toBeInTheDocument();
  });

  it('cierra los modales de crear y editar sin guardar cambios', async () => {
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([sampleRubro]))
      )
    );

    const { user } = renderPage();
    await screen.findByText('Productos Farmacéuticos');

    await user.click(screen.getByRole('button', { name: 'Nuevo rubro comercial' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('button', { name: 'Crear rubro comercial' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Editar Productos Farmacéuticos' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument();
  });

  it('cambia el estado de un rubro comercial activo a inactivo', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([{ ...sampleRubro, estado: currentEstado }]))
      ),
      http.patch('*/api/v1/catalogo/rubros-comerciales/rubro-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Productos Farmacéuticos');

    await user.click(screen.getByRole('button', { name: 'Desactivar Productos Farmacéuticos' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });

  it('cambia el estado de un rubro comercial inactivo a activo', async () => {
    let currentEstado = 'INACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/rubros-comerciales', () =>
        HttpResponse.json(paginaResponse([{ ...sampleRubro, estado: currentEstado }]))
      ),
      http.patch('*/api/v1/catalogo/rubros-comerciales/rubro-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Productos Farmacéuticos');

    await user.click(screen.getByRole('button', { name: 'Activar Productos Farmacéuticos' }));

    await waitFor(() => expect(screen.getByText('ACTIVO')).toBeInTheDocument());
  });
});
