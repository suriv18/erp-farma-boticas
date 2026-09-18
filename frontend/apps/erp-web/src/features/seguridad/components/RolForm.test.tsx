import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RolForm } from './RolForm';

describe('RolForm', () => {
  it('muestra errores de validacion cuando se envia vacio', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<RolForm onSubmit={onSubmit} submitLabel="Crear rol" />);

    await user.click(screen.getByRole('button', { name: 'Crear rol' }));

    expect(await screen.findByText('El código debe tener al menos 3 caracteres.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores cuando el formulario es valido', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<RolForm onSubmit={onSubmit} submitLabel="Crear rol" />);

    await user.type(screen.getByLabelText('Código'), 'ADMIN_LOCAL');
    await user.type(screen.getByLabelText('Nombre'), 'Administrador local');
    await user.selectOptions(screen.getByLabelText('Tipo de rol'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Crear rol' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ code: 'ADMIN_LOCAL', name: 'Administrador local', roleType: 'ESTABLECIMIENTO' })
    );
  });

  it('precarga los valores por defecto al editar', () => {
    render(
      <RolForm
        defaultValues={{ code: 'ADMIN_LOCAL', name: 'Administrador local', roleType: 'ESTABLECIMIENTO', systemRole: false }}
        onSubmit={vi.fn()}
        submitLabel="Guardar cambios"
      />
    );

    expect(screen.getByLabelText('Código')).toHaveValue('ADMIN_LOCAL');
    expect(screen.getByLabelText('Nombre')).toHaveValue('Administrador local');
  });
});
