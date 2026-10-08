import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CambiarEstadoDialog } from './CambiarEstadoDialog';
import type { CambiarEstadoDialogProps } from './CambiarEstadoDialog';

type Estado = 'ACTIVO' | 'SUSPENDIDO' | 'BLOQUEADO';

const estados = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;

function renderDialog(overrides: Partial<CambiarEstadoDialogProps<Estado>> = {}) {
  const onSubmit = vi.fn();
  const onClose = vi.fn();
  render(
    <CambiarEstadoDialog
      title="Cambiar estado de la empresa"
      estados={estados}
      current="ACTIVO"
      isSubmitting={false}
      error={null}
      onSubmit={onSubmit}
      onClose={onClose}
      {...overrides}
    />
  );
  return { onSubmit, onClose, user: userEvent.setup() };
}

describe('CambiarEstadoDialog', () => {
  it('muestra el estado actual seleccionado y deshabilita guardar mientras no cambie', () => {
    renderDialog();

    expect(
      screen.getByRole('heading', { name: 'Cambiar estado de la empresa' })
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Estado')).toHaveValue('ACTIVO');
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('envía el nuevo estado seleccionado', async () => {
    const { onSubmit, user } = renderDialog();

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(onSubmit).toHaveBeenCalledWith('SUSPENDIDO');
  });

  it('deshabilita guardar mientras se envía', async () => {
    const { onSubmit, user } = renderDialog({ isSubmitting: true });

    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error recibido', () => {
    renderDialog({ error: 'No tienes permiso para esta acción.' });

    expect(screen.getByRole('alert')).toHaveTextContent('No tienes permiso para esta acción.');
  });

  it('llama a onClose desde el botón cerrar', async () => {
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledOnce();
  });

  it('muestra el aviso de consecuencias y exige confirmarlas antes de guardar', async () => {
    const { onSubmit, user } = renderDialog({
      consecuencias: { SUSPENDIDO: 'No admitirá altas nuevas.' }
    });

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');

    expect(screen.getByRole('note')).toHaveTextContent('No admitirá altas nuevas.');
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(onSubmit).toHaveBeenCalledWith('SUSPENDIDO');
  });

  it('vuelve a deshabilitar guardar al desmarcar la confirmación', async () => {
    const { user } = renderDialog({ consecuencias: { SUSPENDIDO: 'No admitirá altas nuevas.' } });

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeEnabled();
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));

    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('no pide confirmación para un estado sin consecuencias', async () => {
    const { user } = renderDialog({ consecuencias: { SUSPENDIDO: 'No admitirá altas nuevas.' } });

    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');

    expect(screen.queryByRole('note')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeEnabled();
  });

  it('reinicia la confirmación al cambiar de estado', async () => {
    const { user } = renderDialog({
      consecuencias: { SUSPENDIDO: 'Aviso uno.', BLOQUEADO: 'Aviso dos.' }
    });

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');

    expect(screen.getByRole('note')).toHaveTextContent('Aviso dos.');
    expect(screen.getByLabelText('Entiendo las consecuencias')).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('no muestra el aviso del estado actual', () => {
    renderDialog({ consecuencias: { ACTIVO: 'No debería verse.' } });

    expect(screen.queryByRole('note')).not.toBeInTheDocument();
  });
});
