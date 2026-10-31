import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { proveedorAFormulario } from '../lib/proveedor-form';
import { ProveedorForm } from './ProveedorForm';

function renderForm(overrides: Partial<Parameters<typeof ProveedorForm>[0]> = {}) {
  const onSubmit = vi.fn();
  const onCancel = vi.fn();
  render(<ProveedorForm submitLabel="Crear proveedor" onSubmit={onSubmit} {...overrides} />);
  return { onSubmit, onCancel, user: userEvent.setup() };
}

describe('ProveedorForm', () => {
  it('arranca con los valores por defecto del backend', () => {
    renderForm();

    expect(screen.getByLabelText('Tipo de documento')).toHaveValue('6');
    expect(screen.getByLabelText('Condición de pago')).toHaveValue('CONTADO');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('0');
    expect(screen.getByLabelText('Moneda')).toHaveValue('PEN');
    expect(screen.getByLabelText('Calificación')).toHaveValue('CONFIABLE');
    expect(screen.getByLabelText('Distribuidor')).toBeChecked();
    expect(screen.getByLabelText('Laboratorio')).not.toBeChecked();
  });

  it('muestra los errores de validación y no envía un formulario inválido', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    expect(await screen.findByText('El número de documento es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('La razón social es obligatoria.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envía los valores del formulario válido', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Número de documento'), '20100070970');
    await user.type(screen.getByLabelText('Razón social'), 'Lab SAC');
    await user.click(screen.getByLabelText('Laboratorio'));
    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0]?.[0]).toMatchObject({
      numeroDocumento: '20100070970',
      razonSocial: 'Lab SAC',
      esLaboratorio: true,
      esDistribuidor: true,
      diasCreditoDefault: '0'
    });
  });

  it('precarga los valores recibidos', () => {
    renderForm({
      defaultValues: proveedorAFormulario(sampleProveedor),
      submitLabel: 'Guardar cambios'
    });

    expect(screen.getByLabelText('Razón social')).toHaveValue('Laboratorios Perú SAC');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('30');
    expect(screen.getByLabelText('Laboratorio')).toBeChecked();
  });

  it('deshabilita el envío mientras guarda y muestra el error recibido', () => {
    renderForm({ isSubmitting: true, error: 'Ya existe un proveedor con ese documento.' });

    expect(screen.getByRole('button', { name: 'Crear proveedor' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un proveedor con ese documento.'
    );
  });

  it('cancela solo cuando se entrega onCancel', async () => {
    const onCancel = vi.fn();
    const { user } = renderForm({ onCancel });

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('no muestra Cancelar sin onCancel', () => {
    renderForm();

    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();
  });
});
