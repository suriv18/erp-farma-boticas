import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { PrincipiosActivosPage } from './PrincipiosActivosPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <PrincipiosActivosPage />
      </QueryClientProvider>
    )
  };
}

const sample = {
  id: 'pa-1',
  codigoFuente: 'PA-001',
  denominacion: 'Paracetamol',
  nombreNormalizado: 'paracetamol',
  fuente: 'DIGEMID',
  estado: 'ACTIVO'
};

const sinDatos = {
  ...sample,
  id: 'pa-2',
  denominacion: 'Ibuprofeno',
  codigoFuente: null,
  fuente: null
};

describe('PrincipiosActivosPage', () => {
  it('lista los principios activos y muestra marcador para datos vacios', async () => {
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () => HttpResponse.json([sample, sinDatos]))
    );

    renderPage();

    expect(await screen.findByText('Paracetamol')).toBeInTheDocument();
    expect(screen.getByText('PA-001')).toBeInTheDocument();
    expect(screen.getAllByText('—')).toHaveLength(2);
  });

  it('informa cuando el listado falla', async () => {
    server.use(
      http.get(
        '*/api/v1/catalogo/principios-activos',
        () => new HttpResponse(null, { status: 500 })
      )
    );

    renderPage();

    expect(
      await screen.findByText('No se pudo cargar el listado de principios activos.')
    ).toBeInTheDocument();
  });

  it('busca por texto y pagina en el cliente', async () => {
    const received: Array<string | null> = [];
    const muchos = Array.from({ length: 25 }, (_, index) => ({
      ...sample,
      id: `pa-${index}`,
      denominacion: `Principio ${String(index).padStart(2, '0')}`
    }));
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', ({ request }) => {
        received.push(new URL(request.url).searchParams.get('texto'));
        return HttpResponse.json(muchos);
      })
    );

    const { user } = renderPage();
    await screen.findByText('Principio 00');
    expect(screen.queryByText('Principio 24')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(await screen.findByText('Principio 24')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Buscar principio activo'), 'pri');
    await waitFor(() => expect(received).toContain('pri'));
    expect(await screen.findByText('Principio 00')).toBeInTheDocument();
  });

  it('crea un principio activo, limpiando los campos vacios', async () => {
    let created = false;
    let body: unknown;
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () =>
        HttpResponse.json(created ? [sample] : [])
      ),
      http.post('*/api/v1/catalogo/principios-activos', async ({ request }) => {
        body = await request.json();
        created = true;
        return HttpResponse.json(sample, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron principios activos.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo principio activo' }));
    await user.type(screen.getByLabelText('Denominación'), 'Paracetamol');
    await user.click(screen.getByRole('button', { name: 'Crear principio activo' }));

    expect(await screen.findByText('PA-001')).toBeInTheDocument();
    expect(body).toEqual({ denominacion: 'Paracetamol' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('cierra los dialogos de alta y edicion sin guardar', async () => {
    server.use(http.get('*/api/v1/catalogo/principios-activos', () => HttpResponse.json([sample])));

    const { user } = renderPage();
    await screen.findByText('Paracetamol');

    await user.click(screen.getByRole('button', { name: 'Nuevo principio activo' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Editar Paracetamol' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear', async () => {
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () => HttpResponse.json([])),
      http.post('*/api/v1/catalogo/principios-activos', () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Ya existe un principio activo con esa denominación.' },
          { status: 409 }
        )
      )
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron principios activos.');

    await user.click(screen.getByRole('button', { name: 'Nuevo principio activo' }));
    await user.type(screen.getByLabelText('Denominación'), 'Paracetamol');
    await user.click(screen.getByRole('button', { name: 'Crear principio activo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un principio activo con esa denominación.'
    );
  });

  it('edita un principio activo existente', async () => {
    let denominacion = 'Paracetamol';
    let body: unknown;
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () =>
        HttpResponse.json([{ ...sample, denominacion }])
      ),
      http.put('*/api/v1/catalogo/principios-activos/pa-1', async ({ request }) => {
        body = await request.json();
        denominacion = (body as { denominacion: string }).denominacion;
        return HttpResponse.json({ ...sample, denominacion });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Paracetamol');

    await user.click(screen.getByRole('button', { name: 'Editar Paracetamol' }));
    const input = screen.getByLabelText('Denominación');
    await user.clear(input);
    await user.type(input, 'Paracetamol 500');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Paracetamol 500')).toBeInTheDocument();
    expect(body).toEqual({
      codigoFuente: 'PA-001',
      denominacion: 'Paracetamol 500',
      nombreNormalizado: 'paracetamol',
      fuente: 'DIGEMID'
    });
  });

  it('muestra el error del servidor al editar', async () => {
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () => HttpResponse.json([sample])),
      http.put(
        '*/api/v1/catalogo/principios-activos/pa-1',
        () => new HttpResponse(null, { status: 403 })
      )
    );

    const { user } = renderPage();
    await screen.findByText('Paracetamol');

    await user.click(screen.getByRole('button', { name: 'Editar Paracetamol' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();
  });

  it('cambia el estado entre activo e inactivo', async () => {
    let estado = 'ACTIVO';
    const bodies: unknown[] = [];
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () =>
        HttpResponse.json([{ ...sample, estado }])
      ),
      http.patch('*/api/v1/catalogo/principios-activos/pa-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        bodies.push(body);
        estado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Paracetamol');

    await user.click(screen.getByRole('button', { name: 'Desactivar Paracetamol' }));
    expect(await screen.findByText('INACTIVO')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Activar Paracetamol' }));
    expect(await screen.findByText('ACTIVO')).toBeInTheDocument();
    expect(bodies).toEqual([{ status: 'INACTIVO' }, { status: 'ACTIVO' }]);
  });

  it('muestra el error cuando el cambio de estado falla', async () => {
    server.use(
      http.get('*/api/v1/catalogo/principios-activos', () => HttpResponse.json([sample])),
      http.patch(
        '*/api/v1/catalogo/principios-activos/pa-1/estado',
        () => new HttpResponse(null, { status: 404 })
      )
    );

    const { user } = renderPage();
    await screen.findByText('Paracetamol');

    await user.click(screen.getByRole('button', { name: 'Desactivar Paracetamol' }));

    expect(
      await screen.findByText('El recurso no existe o no pertenece a tu organización.')
    ).toBeInTheDocument();
  });
});
