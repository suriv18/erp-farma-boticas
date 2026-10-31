import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EstadoDialog } from './EstadoDialog';

function renderDialog(overrides: Partial<Parameters<typeof EstadoDialog>[0]> = {}) {
  const props = {
    title: 'Cambiar estado',
    estados: ['ACTIVO', 'INACTIVO', 'BLOQUEADO'],
    current: 'ACTIVO',
    isSubmitting: false,
    error: null,
    onSubmit: vi.fn(),
    onClose: vi.fn(),
    ...overrides
  };
  return { props, user: userEvent.setup(), ...render(<EstadoDialog {...props} />) };
}

describe('EstadoDialog', () => {
  it('deshabilita el guardado mientras no cambie el estado', () => {
    renderDialog();

    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('envia el nuevo estado seleccionado', async () => {
    const { props, user } = renderDialog();

    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(props.onSubmit).toHaveBeenCalledWith('BLOQUEADO');
  });

  it('muestra el error y bloquea el envio mientras se guarda', async () => {
    const { user } = renderDialog({ error: 'Estado no permitido.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Estado no permitido.');
    await user.selectOptions(screen.getByLabelText('Estado'), 'INACTIVO');
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('cierra el dialogo', async () => {
    const { props, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(props.onClose).toHaveBeenCalled();
  });
});
