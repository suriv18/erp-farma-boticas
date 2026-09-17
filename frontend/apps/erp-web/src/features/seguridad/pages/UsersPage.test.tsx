import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { UsersPage } from './UsersPage';

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
  const router = createMemoryRouter([{ path: '/', Component: UsersPage }]);
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

describe('UsersPage', () => {
  it('lista los usuarios del tenant activo', async () => {
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 })
      )
    );

    renderPage();

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
  });

  it('crea un usuario nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/usuarios', () =>
        HttpResponse.json({
          items: created ? [sampleUsuario] : [],
          page: 0,
          size: 20,
          totalElements: created ? 1 : 0
        })
      ),
      http.post('*/api/v1/usuarios', () => {
        created = true;
        return HttpResponse.json(sampleUsuario, { status: 201 });
      })
    );

    const { user } = renderPage();

    expect(await screen.findByText('No se encontraron usuarios.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo usuario' }));
    await user.type(screen.getByLabelText('Nombre visible'), 'Ada Lovelace');
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    await waitFor(() => expect(screen.getByText('Ada Lovelace')).toBeInTheDocument());
  });

  it('permite buscar por texto', async () => {
    server.use(
      http.get('*/api/v1/usuarios', ({ request }) => {
        const url = new URL(request.url);
        const search = url.searchParams.get('search');
        return HttpResponse.json({
          items: search === 'ada' ? [sampleUsuario] : [],
          page: 0,
          size: 20,
          totalElements: search === 'ada' ? 1 : 0
        });
      })
    );

    const { user } = renderPage();
    await user.type(screen.getByLabelText('Buscar usuario'), 'ada');

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
  });
});
