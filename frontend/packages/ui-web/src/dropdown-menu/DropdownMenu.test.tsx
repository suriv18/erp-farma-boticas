import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { LogOut, User } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from './DropdownMenu';

function renderMenu(onSelectLogout: () => void) {
  return render(
    <MemoryRouter>
      <DropdownMenu trigger={<button>Abrir menú</button>}>
        <DropdownMenuLabel>Cuenta</DropdownMenuLabel>
        <DropdownMenuItem icon={User} to="/perfil">
          Mi Perfil
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem icon={LogOut} tone="danger" onSelect={onSelectLogout}>
          Cerrar sesión
        </DropdownMenuItem>
      </DropdownMenu>
    </MemoryRouter>
  );
}

describe('DropdownMenu', () => {
  it('no muestra el contenido antes de abrir', () => {
    renderMenu(() => {});
    expect(screen.queryByText('Mi Perfil')).not.toBeInTheDocument();
  });

  it('muestra el contenido al hacer clic en el trigger', async () => {
    const user = userEvent.setup();
    renderMenu(() => {});

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));

    expect(screen.getByText('Cuenta')).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /Mi Perfil/ })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /Cerrar sesión/ })).toBeInTheDocument();
  });

  it('cierra el menú con la tecla Escape', async () => {
    const user = userEvent.setup();
    renderMenu(() => {});

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));
    expect(screen.getByText('Cuenta')).toBeInTheDocument();

    await user.keyboard('{Escape}');
    expect(screen.queryByText('Cuenta')).not.toBeInTheDocument();
  });

  it('el item con "to" renderiza un enlace de navegación', async () => {
    const user = userEvent.setup();
    renderMenu(() => {});

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));

    const link = screen.getByRole('menuitem', { name: /Mi Perfil/ });
    expect(link).toHaveAttribute('href', '/perfil');
  });

  it('el item sin "to" ejecuta onSelect al seleccionarlo', async () => {
    const user = userEvent.setup();
    const onSelectLogout = vi.fn();
    renderMenu(onSelectLogout);

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));
    await user.click(screen.getByRole('menuitem', { name: /Cerrar sesión/ }));

    expect(onSelectLogout).toHaveBeenCalledOnce();
  });
});
