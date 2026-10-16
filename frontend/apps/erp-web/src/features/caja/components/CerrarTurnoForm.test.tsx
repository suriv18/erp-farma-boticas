import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CerrarTurnoForm } from './CerrarTurnoForm';

function renderForm(props: { isSubmitting?: boolean; error?: string | null } = {}) {
  const onSubmit = vi.fn();
  render(
    <CerrarTurnoForm
      totalEstimado={350}
      isSubmitting={props.isSubmitting ?? false}
      error={props.error ?? null}
      onSubmit={onSubmit}
    />
  );
  return { onSubmit, user: userEvent.setup() };
}

describe('CerrarTurnoForm', () => {
  it('muestra la diferencia estimada en vivo solo con un total declarado válido', async () => {
    const { user } = renderForm();

    expect(screen.queryByText(/Diferencia estimada/)).not.toBeInTheDocument();
    await user.type(screen.getByLabelText('Total declarado'), '348.5');

    expect(screen.getByText('Diferencia estimada: -S/ 1.50')).toBeInTheDocument();
  });

  it('envía el total declarado y la observación', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Total declarado'), '348.5');
    await user.type(screen.getByLabelText('Observación'), 'Faltante');
    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({ totalDeclarado: '348.5', observacion: 'Faltante' })
    );
  });

  it('muestra el error de validación sin enviar', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Cerrar turno' }));

    expect(
      await screen.findByText(
        'El total declarado debe ser un monto mayor o igual a cero con hasta 2 decimales.'
      )
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error recibido y deshabilita el botón mientras envía', () => {
    renderForm({ isSubmitting: true, error: 'El turno ya fue cerrado.' });

    expect(screen.getByRole('alert')).toHaveTextContent('El turno ya fue cerrado.');
    expect(screen.getByRole('button', { name: 'Cerrar turno' })).toBeDisabled();
  });
});
