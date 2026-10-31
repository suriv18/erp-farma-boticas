import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CodigoBarraForm } from './CodigoBarraForm';

function renderForm(overrides: Partial<Parameters<typeof CodigoBarraForm>[0]> = {}) {
  const onSubmit = vi.fn();
  return {
    onSubmit,
    user: userEvent.setup(),
    ...render(
      <CodigoBarraForm onSubmit={onSubmit} isSubmitting={false} error={null} {...overrides} />
    )
  };
}

describe('CodigoBarraForm', () => {
  it('exige el codigo de barras', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Agregar código de barras' }));

    expect(await screen.findByText('El código de barras es obligatorio.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia el codigo con el tipo EAN13 por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código de barras'), '7750001000012');
    await user.click(screen.getByRole('button', { name: 'Agregar código de barras' }));

    expect(onSubmit).toHaveBeenCalledWith({
      codigoBarra: '7750001000012',
      tipoCodigo: 'EAN13',
      vigenteDesde: '',
      vigenteHasta: ''
    });
  });

  it('muestra el error del servidor y bloquea el envio', () => {
    renderForm({ error: 'Ya existe un SKU con el código de barras indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un SKU con el código de barras indicado.'
    );
    expect(screen.getByRole('button', { name: 'Agregar código de barras' })).toBeDisabled();
  });
});
