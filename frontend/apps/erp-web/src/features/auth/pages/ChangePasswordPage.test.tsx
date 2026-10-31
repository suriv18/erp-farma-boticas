import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { RequireAuthentication } from '../components/RequireAuthentication';
import { AuthSessionProvider } from '../model/AuthSessionProvider';
import { saveRefreshToken } from '../model/session-storage';
import { ChangePasswordPage } from './ChangePasswordPage';
import { LoginPage } from './LoginPage';

afterEach(() => {
  sessionStorage.clear();
});

const tokens = (passwordChangeRequired: boolean) => ({
  accessToken: 'access-1',
  refreshToken: 'refresh-2',
  tokenType: 'Bearer',
  accessExpiresAt: '2026-09-05T10:10:00Z',
  refreshExpiresAt: '2026-09-12T10:00:00Z',
  tenantId: 'tenant-abc',
  userId: 'user-xyz',
  sessionId: 'session-1',
  passwordChangeRequired
});

function renderRouter(initialEntry: string) {
  const router = createMemoryRouter(
    [
      { path: '/login', Component: LoginPage },
      { path: '/cambiar-contrasena', Component: ChangePasswordPage },
      {
        Component: RequireAuthentication,
        children: [{ path: '/dashboard', Component: () => <h1>Resumen operativo</h1> }]
      }
    ],
    { initialEntries: [initialEntry] }
  );

  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={new QueryClient()}>
        <AuthSessionProvider>
          <RouterProvider router={router} />
        </AuthSessionProvider>
      </QueryClientProvider>
    )
  };
}

function renderApp(initialEntry: string, passwordChangeRequired: boolean) {
  saveRefreshToken('refresh-existing');
  server.use(http.post('http://localhost/api/v1/auth/refresh', () => HttpResponse.json(tokens(passwordChangeRequired))));
  return renderRouter(initialEntry);
}

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Contraseña actual'), 'Actual2026!');
  await user.type(screen.getByLabelText('Nueva contraseña'), 'Nueva2026!');
  await user.type(screen.getByLabelText('Confirmar nueva contraseña'), 'Nueva2026!');
  await user.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));
}

describe('cambio obligatorio de contraseña', () => {
  it('no deja entrar a rutas de negocio mientras el cambio sea obligatorio', async () => {
    renderApp('/dashboard', true);

    expect(await screen.findByRole('heading', { name: 'Cambia tu contraseña' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Resumen operativo' })).not.toBeInTheDocument();
  });

  it('devuelve al dashboard a quien no tiene cambio pendiente', async () => {
    renderApp('/cambiar-contrasena', false);

    expect(await screen.findByRole('heading', { name: 'Resumen operativo' })).toBeInTheDocument();
  });

  it('envia a login a quien no tiene sesion', async () => {
    renderRouter('/cambiar-contrasena');

    expect(await screen.findByRole('heading', { name: 'Bienvenido de nuevo 👋' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('valida el formulario antes de enviar', async () => {
    const { user } = renderApp('/cambiar-contrasena', true);
    await screen.findByRole('heading', { name: 'Cambia tu contraseña' });

    await user.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));

    expect(await screen.findByText('Ingresa tu contraseña actual.')).toBeInTheDocument();
  });

  it('cambia la contraseña y lleva al login con aviso', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/auth/password/change', async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );
    const { user } = renderApp('/cambiar-contrasena', true);
    await screen.findByRole('heading', { name: 'Cambia tu contraseña' });

    await fillAndSubmit(user);

    expect(
      await screen.findByText('Contraseña actualizada. Inicia sesión con tu nueva contraseña.')
    ).toBeInTheDocument();
    expect(body).toEqual({ currentPassword: 'Actual2026!', newPassword: 'Nueva2026!' });
  });

  it('muestra el rechazo del backend y permite reintentar', async () => {
    server.use(
      http.post('http://localhost/api/v1/auth/password/change', () => HttpResponse.json({}, { status: 400 }))
    );
    const { user } = renderApp('/cambiar-contrasena', true);
    await screen.findByRole('heading', { name: 'Cambia tu contraseña' });

    await fillAndSubmit(user);

    expect(await screen.findByText(/No se pudo cambiar la contraseña\. Verifica/)).toBeInTheDocument();
  });

  it('muestra un error generico ante una falla inesperada', async () => {
    server.use(http.post('http://localhost/api/v1/auth/password/change', () => HttpResponse.error()));
    const { user } = renderApp('/cambiar-contrasena', true);
    await screen.findByRole('heading', { name: 'Cambia tu contraseña' });

    await fillAndSubmit(user);

    expect(await screen.findByText('No se pudo cambiar la contraseña. Intenta nuevamente.')).toBeInTheDocument();
  });
});
