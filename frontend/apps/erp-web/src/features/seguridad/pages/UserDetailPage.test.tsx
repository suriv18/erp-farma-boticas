import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { UserDetailPage } from './UserDetailPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'admin-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const sampleUsuario = {
  id: 'user-1',
  tenantId: 'tenant-1',
  documentType: null,
  documentNumber: null,
  firstNames: 'Ada',
  lastNames: 'Lovelace',
  username: null,
  email: 'ada@boticas.pe',
  displayName: 'Ada Lovelace',
  phone: null,
  credentialChangeRequired: false,
  mfaRequired: false,
  status: 'ACTIVO',
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: null
};

const sampleAsignacion = {
  id: 'assign-1',
  roleId: 'rol-1',
  roleCode: 'ADMIN_LOCAL',
  roleName: 'Administrador local',
  scopeType: 'ESTABLECIMIENTO',
  companyId: null,
  establishmentId: 'est-1',
  warehouseId: null,
  terminalId: null,
  validFrom: null,
  validUntil: null,
  status: 'ACTIVO',
  createdBy: 'admin-1',
  createdAt: '2026-09-01T00:00:00Z'
};

const sampleIdentidad = {
  provider: 'GOOGLE',
  subject: 'google-oauth2|123',
  issuer: 'https://accounts.google.com',
  emailClaim: 'ada@boticas.pe',
  lastLoginAt: null,
  createdAt: '2026-09-01T00:00:00Z'
};

function renderPage(userId = 'user-1') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <MemoryRouter initialEntries={[`/seguridad/usuarios/${userId}`]}>
            <Routes>
              <Route path="/seguridad/usuarios/:userId" element={<UserDetailPage />} />
            </Routes>
          </MemoryRouter>
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

function mockBaseHandlers() {
  server.use(
    http.get('*/api/v1/usuarios', () =>
      HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 })
    ),
    http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () => HttpResponse.json([sampleAsignacion])),
    http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([sampleIdentidad])),
    http.get('*/api/v1/roles', () =>
      HttpResponse.json({
        items: [
          {
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
          }
        ],
        page: 0,
        size: 100,
        totalElements: 1
      })
    ),
    http.get('*/api/v1/estructura-corporativa', () =>
      HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
    )
  );
}

describe('UserDetailPage', () => {
  it('muestra los datos del usuario, sus roles asignados e identidades externas', async () => {
    mockBaseHandlers();
    renderPage();

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    expect(await screen.findByText('google-oauth2|123')).toBeInTheDocument();
  });

  it('revoca una asignacion de rol tras confirmar', async () => {
    mockBaseHandlers();
    let revoked = false;
    server.use(
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () =>
        HttpResponse.json(revoked ? [] : [sampleAsignacion])
      ),
      http.delete('*/api/v1/usuarios/user-1/asignaciones-rol/assign-1', () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Revocar' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar revocación' }));

    await waitFor(() => expect(screen.getByText('Sin roles asignados.')).toBeInTheDocument());
  });

  it('vincula una identidad externa nueva', async () => {
    mockBaseHandlers();
    let linked = false;
    server.use(
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () =>
        HttpResponse.json(linked ? [sampleIdentidad] : [])
      ),
      http.post('*/api/v1/usuarios/user-1/identidades-externas', () => {
        linked = true;
        return HttpResponse.json(sampleIdentidad, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('Sin identidades externas vinculadas.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Vincular identidad' }));
    await user.selectOptions(screen.getByLabelText('Proveedor'), 'GOOGLE');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'google-oauth2|123');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    await waitFor(() => expect(screen.getByText('google-oauth2|123')).toBeInTheDocument());
  });

  it('provisiona una credencial local', async () => {
    mockBaseHandlers();
    let receivedBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios/user-1/credencial-local', async ({ request }) => {
        receivedBody = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByText('Ada Lovelace');
    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Sup3r$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    await waitFor(() =>
      expect(receivedBody).toEqual({ tenantId: 'tenant-1', password: 'Sup3r$eguro123', requireChange: true })
    );
  });

  it('cambia el estado del usuario tras confirmar', async () => {
    mockBaseHandlers();
    let currentStatus = 'ACTIVO';
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({
          items: [{ ...sampleUsuario, status: currentStatus }],
          page: 0,
          size: 20,
          totalElements: 1
        })
      ),
      http.patch('*/api/v1/usuarios/user-1/estado', () => {
        currentStatus = 'INACTIVO';
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();

    await screen.findByText('Ada Lovelace');
    await user.click(screen.getByRole('button', { name: 'Desactivar usuario' }));
    await user.click(screen.getByRole('button', { name: 'Desactivar' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });

  it('muestra un estado de error si el backend falla', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () => HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })),
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () =>
        HttpResponse.json({ title: 'Error interno', status: 500 }, { status: 500 })
      ),
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([])),
      http.get('*/api/v1/roles', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      )
    );

    renderPage();

    expect(await screen.findByText('No se pudo cargar la información.')).toBeInTheDocument();
  });

  it('muestra "Usuario no encontrado" cuando la lista de usuarios carga pero no incluye el id solicitado', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })
      ),
      http.get('*/api/v1/usuarios/user-1/asignaciones-rol', () => HttpResponse.json([])),
      http.get('*/api/v1/usuarios/user-1/identidades-externas', () => HttpResponse.json([])),
      http.get('*/api/v1/roles', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      )
    );

    renderPage();

    expect(await screen.findByText('Usuario no encontrado.')).toBeInTheDocument();
    expect(screen.queryByText('Cargando usuario…')).not.toBeInTheDocument();
  });

  it('muestra el error del backend al fallar la provision de credencial y no limpia el formulario', async () => {
    mockBaseHandlers();
    server.use(
      http.post('*/api/v1/usuarios/user-1/credencial-local', () =>
        HttpResponse.json({ title: 'Contraseña inválida', status: 400 }, { status: 400 })
      )
    );

    const { user } = renderPage();

    await screen.findByText('Ada Lovelace');
    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Sup3r$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(await screen.findByText('Contraseña inválida')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toHaveValue('Sup3r$eguro123');
    expect(screen.getByLabelText('Confirmar contraseña')).toHaveValue('Sup3r$eguro123');
  });
});
