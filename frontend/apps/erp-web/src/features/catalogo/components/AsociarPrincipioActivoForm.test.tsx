import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AsociarPrincipioActivoForm } from './AsociarPrincipioActivoForm';

function renderForm(overrides: Partial<Parameters<typeof AsociarPrincipioActivoForm>[0]> = {}) {
  const onSubmit = vi.fn();
  return {
    onSubmit,
    user: userEvent.setup(),
    ...render(
      <AsociarPrincipioActivoForm
        principios={[{ value: 'pa-1', label: 'Paracetamol' }]}
        unidades={[{ value: 'MG', label: 'MG — Miligramo' }]}
        onSubmit={onSubmit}
        isSubmitting={false}
        error={null}
        {...overrides}
      />
    )
  };
}

describe('AsociarPrincipioActivoForm', () => {
  it('exige seleccionar un principio activo', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Asociar principio activo' }));

    expect(await screen.findByText('Selecciona un principio activo.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia la asociacion capturada', async () => {
    const { onSubmit, user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Principio activo'), 'pa-1');
    await user.type(screen.getByLabelText('Concentración'), '500 mg');
    await user.type(screen.getByLabelText('Cantidad'), '500');
    await user.selectOptions(screen.getByLabelText('Unidad de medida'), 'MG');
    await user.click(screen.getByLabelText('Es principio activo principal'));
    await user.click(screen.getByRole('button', { name: 'Asociar principio activo' }));

    expect(onSubmit).toHaveBeenCalledWith({
      principioActivoId: 'pa-1',
      concentracionTexto: '500 mg',
      cantidad: '500',
      unidadMedidaCodigo: 'MG',
      esPrincipal: false,
      orden: '1'
    });
  });

  it('muestra el error del servidor y bloquea el envio', () => {
    renderForm({ error: 'El principio activo ya está asociado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('El principio activo ya está asociado.');
    expect(screen.getByRole('button', { name: 'Asociar principio activo' })).toBeDisabled();
  });
});
