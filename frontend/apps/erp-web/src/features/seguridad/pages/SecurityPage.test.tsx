import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { SecurityPage } from './SecurityPage';

function renderPage() {
  const router = createMemoryRouter([{ path: '/', Component: SecurityPage }]);
  return render(<RouterProvider router={router} />);
}

describe('SecurityPage', () => {
  it('enlaza a los submodulos de usuarios, roles y permisos', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Usuarios' }).closest('a')).toHaveAttribute(
      'href',
      '/seguridad/usuarios'
    );
    expect(screen.getByRole('heading', { name: 'Roles' }).closest('a')).toHaveAttribute(
      'href',
      '/seguridad/roles'
    );
    expect(screen.getByRole('heading', { name: 'Permisos' }).closest('a')).toHaveAttribute(
      'href',
      '/seguridad/permisos'
    );
  });
});
