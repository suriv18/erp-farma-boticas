import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { RequireAuthentication } from '../components/RequireAuthentication';
import { AuthSessionProvider } from '../model/AuthSessionProvider';
import { saveRefreshToken } from '../model/session-storage';
import { LoginPage } from './LoginPage';

afterEach(() => {
  sessionStorage.clear();
});

function renderLogin(initialEntry = '/login') {
  const router = createMemoryRouter(
    [
      { path: '/login', Component: LoginPage },
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
      <AuthSessionProvider>
        <RouterProvider router={router} />
      </AuthSessionProvider>
    )
  };
}

async function fillValidCredentials(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
  await user.type(screen.getByLabelText('Contraseña'), 'Boticas2026!');
}

describe('LoginPage', () => {
  it('muestra validaciones accesibles y permite visualizar la contraseña', async () => {
    const { user } = renderLogin();

    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(await screen.findByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(screen.getByText('La contraseña debe tener al menos 8 caracteres.')).toBeInTheDocument();

    const password = screen.getByLabelText('Contraseña');
    expect(password).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Mostrar contraseña' }));
    expect(password).toHaveAttribute('type', 'text');
  });

  it('navega al dashboard cuando las credenciales son válidas', async () => {
    const { user } = renderLogin();

    await fillValidCredentials(user);
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(await screen.findByRole('heading', { name: 'Resumen operativo' })).toBeInTheDocument();
  });

  it('muestra un mensaje de error cuando el backend rechaza las credenciales', async () => {
    const { user } = renderLogin();

    await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
    await user.type(screen.getByLabelText('Contraseña'), 'ContrasenaIncorrecta1!');
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(
      await screen.findByText('Credenciales incorrectas o cuenta bloqueada.')
    ).toBeInTheDocument();
  });

  it('redirige al login cuando se intenta abrir una ruta privada sin sesión', async () => {
    renderLogin('/dashboard');

    expect(await screen.findByRole('heading', { name: 'Ingresa a tu cuenta' })).toBeInTheDocument();
  });

  it('no redirige al login mientras restaura la sesión desde el refresh token guardado', async () => {
    saveRefreshToken('refresh-existing');
    server.use(
      http.post(
        'http://localhost/api/v1/auth/refresh',
        () =>
          new Promise((resolve) =>
            setTimeout(
              () =>
                resolve(
                  HttpResponse.json({
                    accessToken: 'access-1',
                    refreshToken: 'refresh-2',
                    tokenType: 'Bearer',
                    accessExpiresAt: '2026-09-05T10:10:00Z',
                    refreshExpiresAt: '2026-09-12T10:00:00Z',
                    tenantId: 'tenant-abc',
                    userId: 'user-xyz',
                    sessionId: 'session-1',
                    passwordChangeRequired: false
                  })
                ),
              50
            )
          )
      )
    );

    renderLogin('/dashboard');

    expect(screen.queryByRole('heading', { name: 'Ingresa a tu cuenta' })).not.toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Resumen operativo' })).toBeInTheDocument();
  });
});
