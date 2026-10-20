import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthSessionContext } from '../../features/auth/model/auth-session.context';
import { server } from '../../test/mocks/server';
import { RequireSectionAccess } from './RequireSectionAccess';

const session = {
  status: 'authenticated' as const,
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  passwordChangeRequired: false,
  authenticate: vi.fn(),
  changePassword: vi.fn(),
  signOut: vi.fn()
};

const perfil = (permissions: string[]) => ({
  userId: 'user-1',
  tenantId: 'tenant-1',
  displayName: 'Luis',
  username: 'luis',
  email: null,
  assignments: [],
  permissions
});

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionContext value={session}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route element={<RequireSectionAccess />}>
              <Route path="/caja" element={<p>pantalla de caja</p>} />
              <Route path="/seguridad" element={<p>pantalla de seguridad</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthSessionContext>
    </QueryClientProvider>
  );
}

describe('RequireSectionAccess', () => {
  it('muestra la sección cuando el usuario tiene el permiso', async () => {
    server.use(
      http.get('*/api/v1/auth/me', () => HttpResponse.json(perfil(['ventas.turnos.abrir'])))
    );
    renderAt('/caja');

    expect(await screen.findByText('pantalla de caja')).toBeInTheDocument();
  });

  it('muestra la página 403 cuando falta el permiso', async () => {
    server.use(
      http.get('*/api/v1/auth/me', () => HttpResponse.json(perfil(['ventas.turnos.abrir'])))
    );
    renderAt('/seguridad');

    expect(
      await screen.findByRole('heading', { name: 'No tienes acceso a esta sección' })
    ).toBeInTheDocument();
    expect(screen.queryByText('pantalla de seguridad')).not.toBeInTheDocument();
  });

  it('no muestra nada mientras carga el perfil', () => {
    server.use(http.get('*/api/v1/auth/me', () => new Promise(() => undefined)));
    const { container } = renderAt('/caja');

    expect(container).toBeEmptyDOMElement();
  });
});
