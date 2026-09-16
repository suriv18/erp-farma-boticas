import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmActionDialog } from './ConfirmActionDialog';

describe('ConfirmActionDialog', () => {
  it('no renderiza nada cuando open es false', () => {
    render(
      <ConfirmActionDialog
        open={false}
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    );
    expect(screen.queryByText('Revocar sesión')).not.toBeInTheDocument();
  });

  it('llama a onConfirm al confirmar', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <ConfirmActionDialog
        open
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={onConfirm}
        onCancel={() => {}}
      />
    );
    await user.click(screen.getByRole('button', { name: 'Revocar' }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('llama a onCancel al cancelar', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <ConfirmActionDialog
        open
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={() => {}}
        onCancel={onCancel}
      />
    );
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('deshabilita el boton de confirmar cuando isPending es true', () => {
    render(
      <ConfirmActionDialog
        open
        title="Revocar sesión"
        description="Esta acción no se puede deshacer."
        confirmLabel="Revocar"
        onConfirm={() => {}}
        onCancel={() => {}}
        isPending
      />
    );
    expect(screen.getByRole('button', { name: 'Revocar' })).toBeDisabled();
  });
});
