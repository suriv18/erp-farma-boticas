import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PRODUCTO_REGULADO_VACIO } from '../lib/producto-regulado-form';
import { ProductoReguladoForm, type ProductoReguladoFormProps } from './ProductoReguladoForm';

function renderForm(props: Partial<ProductoReguladoFormProps> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onSubmit = vi.fn();
  return {
    onSubmit,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <ProductoReguladoForm onSubmit={onSubmit} submitLabel="Guardar" {...props} />
      </QueryClientProvider>
    )
  };
}

describe('ProductoReguladoForm', () => {
  it('valida los campos obligatorios antes de enviar', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(
      await screen.findByText('El tipo de producto debe tener al menos 2 caracteres.')
    ).toBeInTheDocument();
    expect(
      screen.getByText('La denominación debe tener al menos 2 caracteres.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('carga las opciones de los catalogos de soporte y envia los valores', async () => {
    const { onSubmit, user } = renderForm();

    expect(await screen.findByRole('option', { name: 'TAB — Tableta' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'ORAL — Vía oral' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'UND — Unidad' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'IIA — Lista II-A' })).toBeInTheDocument();

    await user.type(screen.getByLabelText('Tipo de producto'), 'FARMACEUTICO');
    await user.type(screen.getByLabelText('Denominación'), 'Paracetamol 500 mg');
    await user.selectOptions(screen.getByLabelText('Forma farmacéutica'), 'TAB');
    await user.type(screen.getByLabelText('Vigente desde'), '2026-01-01');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...PRODUCTO_REGULADO_VACIO,
      tipoProducto: 'FARMACEUTICO',
      denominacion: 'Paracetamol 500 mg',
      formaFarmaceuticaCodigo: 'TAB',
      vigenteDesde: '2026-01-01'
    });
  });

  it('muestra valores iniciales, el error del servidor y bloquea el envio', () => {
    renderForm({
      defaultValues: {
        ...PRODUCTO_REGULADO_VACIO,
        tipoProducto: 'FARMACEUTICO',
        denominacion: 'Ibuprofeno'
      },
      error: 'El número de registro ya existe.',
      isSubmitting: true
    });

    expect(screen.getByLabelText('Denominación')).toHaveValue('Ibuprofeno');
    expect(screen.getByRole('alert')).toHaveTextContent('El número de registro ya existe.');
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });
});
