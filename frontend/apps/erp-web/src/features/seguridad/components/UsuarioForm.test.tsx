import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UsuarioForm } from './UsuarioForm';

describe('UsuarioForm', () => {
  it('muestra un error cuando el correo es invalido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<UsuarioForm onSubmit={onSubmit} submitLabel="Crear usuario" />);

    await user.type(screen.getByLabelText('Correo'), 'no-es-un-correo');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<UsuarioForm onSubmit={onSubmit} submitLabel="Crear usuario" />);

    await user.type(screen.getByLabelText('Nombre visible'), 'Ada Lovelace');
    await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ displayName: 'Ada Lovelace', email: 'ada@boticas.pe' })
    );
  });
});
