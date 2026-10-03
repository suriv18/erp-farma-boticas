import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PrincipioActivoForm } from './PrincipioActivoForm';

describe('PrincipioActivoForm', () => {
  it('valida la denominacion obligatoria antes de enviar', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<PrincipioActivoForm onSubmit={onSubmit} submitLabel="Crear" />);

    await user.click(screen.getByRole('button', { name: 'Crear' }));

    expect(
      await screen.findByText('La denominación debe tener al menos 2 caracteres.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia los valores capturados', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<PrincipioActivoForm onSubmit={onSubmit} submitLabel="Crear" />);

    await user.type(screen.getByLabelText('Denominación'), 'Paracetamol');
    await user.type(screen.getByLabelText('Código fuente'), 'PA-1');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    expect(onSubmit).toHaveBeenCalledWith({
      codigoFuente: 'PA-1',
      denominacion: 'Paracetamol',
      nombreNormalizado: '',
      fuente: ''
    });
  });

  it('muestra los valores iniciales, el error del servidor y bloquea el envio', () => {
    render(
      <PrincipioActivoForm
        defaultValues={{
          codigoFuente: '',
          denominacion: 'Ibuprofeno',
          nombreNormalizado: '',
          fuente: ''
        }}
        onSubmit={vi.fn()}
        submitLabel="Guardar"
        isSubmitting
        error="Ya existe un principio activo con esa denominación."
      />
    );

    expect(screen.getByLabelText('Denominación')).toHaveValue('Ibuprofeno');
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un principio activo con esa denominación.'
    );
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });
});
