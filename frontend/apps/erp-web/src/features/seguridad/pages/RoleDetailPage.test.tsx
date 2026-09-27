import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthSessionContext, type AuthSession } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { RoleDetailPage } from './RoleDetailPage';

const authenticatedSession: AuthSession = {
  status: 'authenticated',
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const sampleRol = {
  id: 'rol-1',
  tenantId: 'tenant-1',
  code: 'ADMIN_LOCAL',
  name: 'Administrador local',
  description: null,
  roleType: 'ESTABLECIMIENTO',
  systemRole: false,
  permissionCodes: [],
  status: 'ACTIVO',
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: null
};

const systemRol = { ...sampleRol, id: 'rol-2', code: 'SUPERADMIN', systemRole: true };

const inactiveRol = { ...sampleRol, id: 'rol-3', status: 'INACTIVO' };

function renderPage(roleId = 'rol-1', session = authenticatedSession) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={session}>
          <MemoryRouter initialEntries={[`/seguridad/roles/${roleId}`]}>
            <Routes>
              <Route path="/seguridad/roles/:roleId" element={<RoleDetailPage />} />
            </Routes>
          </MemoryRouter>
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

describe('RoleDetailPage', () => {
  it('muestra el detalle del rol obtenido por GET individual', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/catalogo/*', () => HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 }))
    );

    renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    expect(screen.getByText('ADMIN_LOCAL')).toBeInTheDocument();
  });

  it('edita el rol y refresca el detalle', async () => {
    let currentRol = sampleRol;
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(currentRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.put('*/api/v1/roles/rol-1', async ({ request }) => {
        const body = (await request.json()) as { name: string };
        currentRol = { ...currentRol, name: body.name };
        return HttpResponse.json(currentRol);
      })
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Editar rol' }));
    const dialog = screen.getByRole('dialog');
    const nameInput = within(dialog).getByLabelText('Nombre');
    await user.clear(nameInput);
    await user.type(nameInput, 'Administrador local editado');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Administrador local editado' })).toBeInTheDocument());
  });

  it('no muestra el boton editar para un rol de sistema', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-2', () => HttpResponse.json(systemRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 }))
    );

    renderPage('rol-2');
    await screen.findByText(systemRol.name);

    expect(screen.queryByRole('button', { name: 'Editar rol' })).not.toBeInTheDocument();
  });

  it('muestra el error del servidor cuando el code editado esta duplicado', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.put('*/api/v1/roles/rol-1', () =>
        HttpResponse.json({ detail: 'Ya existe un rol con el código indicado.' }, { status: 409 })
      )
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Editar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(dialog).findByText('Ya existe un rol con el código indicado.')).toBeInTheDocument();
  });

  it('guarda los permisos seleccionados', async () => {
    let permissionsPayload: string[] | undefined;
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () =>
        HttpResponse.json({
          items: [{ code: 'PERM_A', name: 'Permiso A', moduleName: 'Modulo 1' }],
          page: 0,
          size: 100,
          totalElements: 1
        })
      ),
      http.put('*/api/v1/roles/rol-1/permissions', async ({ request }) => {
        const body = (await request.json()) as { permissionCodes: string[] };
        permissionsPayload = body.permissionCodes;
        return HttpResponse.json({ ...sampleRol, permissionCodes: body.permissionCodes });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('checkbox', { name: 'Permiso A' }));
    await user.click(screen.getByRole('button', { name: 'Guardar permisos' }));

    await waitFor(() => expect(permissionsPayload).toEqual(['PERM_A']));
  });

  it('desactiva un rol activo tras confirmar', async () => {
    let lastStatus: string | undefined;
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.patch('*/api/v1/roles/rol-1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        lastStatus = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Desactivar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Desactivar' }));

    await waitFor(() => expect(lastStatus).toBe('INACTIVO'));
  });

  it('cierra el dialogo de desactivacion al cancelar', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 }))
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Desactivar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('cierra el modal de edicion al presionar cerrar', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 }))
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Editar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Cerrar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('activa un rol inactivo tras confirmar', async () => {
    let lastStatus: string | undefined;
    server.use(
      http.get('*/api/v1/roles/rol-3', () => HttpResponse.json(inactiveRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.patch('*/api/v1/roles/rol-3/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        lastStatus = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage('rol-3');
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Activar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Activar' }));

    await waitFor(() => expect(lastStatus).toBe('ACTIVO'));
  });

  it('usa una lista vacia cuando el catalogo de permisos falla', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ detail: 'error' }, { status: 500 }))
    );

    renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('muestra un mensaje generico cuando la edicion falla por un error de red', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.put('*/api/v1/roles/rol-1', () => HttpResponse.error())
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Editar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(dialog).findByText('No se pudo actualizar el rol.')).toBeInTheDocument();
  });

  it('no ejecuta la consulta del rol cuando no hay tenantId', () => {
    renderPage('rol-1', { ...authenticatedSession, tenantId: null });

    expect(screen.getByText('Cargando rol…')).toBeInTheDocument();
  });

  it('no ejecuta la consulta del rol cuando no hay roleId en la ruta', () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <MemoryRouter initialEntries={['/seguridad/roles/sin-id']}>
            <Routes>
              <Route path="/seguridad/roles/sin-id" element={<RoleDetailPage />} />
            </Routes>
          </MemoryRouter>
        </AuthSessionContext>
      </QueryClientProvider>
    );

    expect(screen.getByText('Cargando rol…')).toBeInTheDocument();
  });
});
