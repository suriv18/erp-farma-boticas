import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { RequireAuthentication } from '../components/RequireAuthentication';
import { AuthSessionProvider } from '../model/AuthSessionProvider';
import { LoginPage } from './LoginPage';

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

    await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
    await user.type(screen.getByLabelText('Contraseña'), 'Boticas2026!');
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(await screen.findByRole('heading', { name: 'Resumen operativo' })).toBeInTheDocument();
  });

  it('redirige al login cuando se intenta abrir una ruta privada sin sesión', async () => {
    renderLogin('/dashboard');

    expect(await screen.findByRole('heading', { name: 'Ingresa a tu cuenta' })).toBeInTheDocument();
  });
});
