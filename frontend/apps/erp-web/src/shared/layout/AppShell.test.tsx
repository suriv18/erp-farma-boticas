import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import {
  AuthSessionContext,
  type AuthSession
} from '../../features/auth/model/auth-session.context';
import { server } from '../../test/mocks/server';
import { AppShell } from './AppShell';

const session: AuthSession = {
  status: 'authenticated',
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  passwordChangeRequired: false,
  authenticate: vi.fn(),
  changePassword: vi.fn(),
  signOut: vi.fn()
};

function renderShell(value: AuthSession = session) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionContext value={value}>
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route element={<AppShell />}>
              <Route path="/dashboard" element={<p>contenido</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthSessionContext>
    </QueryClientProvider>
  );
}

const cajero = {
  userId: 'user-1',
  tenantId: 'tenant-1',
  displayName: 'Luis Paredes Soto',
  username: 'crit001.manana1',
  email: 'crit001.manana1@boticas.pe',
  assignments: [
    {
      roleCode: 'CAJERO',
      roleName: 'Cajero de establecimiento',
      scopeType: 'ESTABLECIMIENTO',
      scopeName: 'Botica Crítica Norte'
    }
  ]
};

describe('AppShell', () => {
  it('muestra el nombre, el rol y la sucursal del usuario autenticado', async () => {
    server.use(http.get('*/api/v1/auth/me', () => HttpResponse.json(cajero)));

    renderShell();

    expect(
      await screen.findByRole('button', { name: 'Menú de perfil de Luis Paredes Soto' })
    ).toBeInTheDocument();
    expect(screen.getAllByText('Luis Paredes Soto').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Cajero de establecimiento').length).toBeGreaterThan(0);
    expect(screen.getAllByText('LP').length).toBeGreaterThan(0);
    expect(screen.getByText('Botica Crítica Norte')).toBeInTheDocument();
    expect(screen.queryByText('María Rojas')).not.toBeInTheDocument();
  });

  it('indica que carga el perfil mientras llega la respuesta', () => {
    server.use(http.get('*/api/v1/auth/me', () => new Promise(() => undefined)));

    renderShell();

    expect(screen.getByRole('button', { name: 'Menú de perfil de Cargando…' })).toBeInTheDocument();
  });

  it('usa un nombre genérico y oculta rol y sucursal cuando el perfil falla', async () => {
    server.use(
      http.get('*/api/v1/auth/me', () => HttpResponse.json({ detail: 'error' }, { status: 500 }))
    );

    renderShell();

    expect(
      await screen.findByRole('button', { name: 'Menú de perfil de Mi cuenta' })
    ).toBeInTheDocument();
    expect(screen.queryByText('Sucursal activa')).not.toBeInTheDocument();
  });

  it('no consulta el perfil cuando no hay un usuario en la sesión', () => {
    renderShell({ ...session, userId: null });

    expect(screen.getByRole('button', { name: 'Menú de perfil de Mi cuenta' })).toBeInTheDocument();
  });
});
