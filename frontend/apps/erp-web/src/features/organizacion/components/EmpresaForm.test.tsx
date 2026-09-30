import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EMPRESA_FORM_VACIO } from '../lib/form-defaults';
import { EmpresaForm } from './EmpresaForm';

function renderForm(props: Partial<Parameters<typeof EmpresaForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<EmpresaForm onSubmit={onSubmit} submitLabel="Crear empresa" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('EmpresaForm', () => {
  it('envía los valores válidos con los valores por defecto de moneda y zona horaria', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.type(screen.getByLabelText('Razón social'), 'Boticas SAC');
    await user.click(screen.getByLabelText('Permite venta online'));
    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...EMPRESA_FORM_VACIO,
      ruc: '20123456789',
      razonSocial: 'Boticas SAC',
      permiteVentaOnline: true
    });
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(
      await screen.findByText('El RUC debe tener 11 dígitos e iniciar con 10 o 20.')
    ).toBeInTheDocument();
    expect(
      screen.getByText('La razón social debe tener al menos 2 caracteres.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en edición precarga los datos y deja el RUC de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...EMPRESA_FORM_VACIO, ruc: '20123456789', razonSocial: 'Boticas SAC' }
    });

    const ruc = screen.getByLabelText('RUC');
    expect(ruc).toHaveValue('20123456789');
    expect(ruc).toHaveAttribute('readonly');
    await user.clear(screen.getByLabelText('Razón social'));
    await user.type(screen.getByLabelText('Razón social'), 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ ruc: '20123456789', razonSocial: 'Boticas del Perú SAC' })
    );
  });

  it('en creación el RUC es editable', () => {
    renderForm();

    expect(screen.getByLabelText('RUC')).not.toHaveAttribute('readonly');
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe una empresa con el RUC indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe una empresa con el RUC indicado.'
    );
    expect(screen.getByRole('button', { name: 'Crear empresa' })).toBeDisabled();
  });

  it('muestra Cancelar después del botón de envío solo cuando recibe onCancel', async () => {
    const onCancel = vi.fn();
    const { user } = renderForm({ onCancel });

    const [submit, cancel] = screen.getAllByRole('button').slice(-2);
    expect(submit).toHaveTextContent('Crear empresa');
    expect(cancel).toHaveTextContent('Cancelar');
    await user.click(cancel as HTMLElement);

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('no muestra Cancelar sin onCancel', () => {
    renderForm();

    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();
  });

  it('no muestra alerta ni deshabilita el envío sin error ni guardado en curso', () => {
    renderForm();

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear empresa' })).toBeEnabled();
  });
});
