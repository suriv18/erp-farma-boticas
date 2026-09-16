import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { RolesPage } from './RolesPage';

const authenticatedSession = {
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <MemoryRouter>
            <RolesPage />
          </MemoryRouter>
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

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

describe('RolesPage', () => {
  it('lista los roles del tenant activo', async () => {
    server.use(
      http.get('*/api/v1/roles', () =>
        HttpResponse.json({ items: [sampleRol], page: 0, size: 20, totalElements: 1 })
      )
    );

    renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
  });

  it('crea un rol nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/roles', () =>
        HttpResponse.json({
          items: created ? [sampleRol] : [],
          page: 0,
          size: 20,
          totalElements: created ? 1 : 0
        })
      ),
      http.post('*/api/v1/roles', () => {
        created = true;
        return HttpResponse.json(sampleRol, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron roles.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo rol' }));
    await user.type(screen.getByLabelText('Código'), 'ADMIN_LOCAL');
    await user.type(screen.getByLabelText('Nombre'), 'Administrador local');
    await user.selectOptions(screen.getByLabelText('Tipo de rol'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Crear rol' }));

    await waitFor(() => expect(screen.getByText('Administrador local')).toBeInTheDocument());
  });
});
