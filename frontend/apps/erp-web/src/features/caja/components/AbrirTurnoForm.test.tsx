import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AbrirTurnoForm } from './AbrirTurnoForm';

function renderForm(props: { isSubmitting?: boolean; error?: string | null } = {}) {
  const onSubmit = vi.fn();
  render(
    <AbrirTurnoForm
      isSubmitting={props.isSubmitting ?? false}
      error={props.error ?? null}
      onSubmit={onSubmit}
    />
  );
  return { onSubmit, user: userEvent.setup() };
}

describe('AbrirTurnoForm', () => {
  it('envía el fondo inicial indicado', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Fondo inicial'), '150.50');
    await user.click(screen.getByRole('button', { name: 'Abrir turno' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({ fondoInicial: '150.50' }));
  });

  it('muestra el error de validación sin enviar', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Abrir turno' }));

    expect(
      await screen.findByText(
        'El fondo inicial debe ser un monto mayor o igual a cero con hasta 2 decimales.'
      )
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error recibido y deshabilita el botón mientras envía', () => {
    renderForm({ isSubmitting: true, error: 'Ya existe un turno abierto.' });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un turno abierto.');
    expect(screen.getByRole('button', { name: 'Abrir turno' })).toBeDisabled();
  });
});
