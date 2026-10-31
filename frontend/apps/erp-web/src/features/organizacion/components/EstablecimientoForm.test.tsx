import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ESTABLECIMIENTO_FORM_VACIO } from '../lib/form-defaults';
import { EstablecimientoForm } from './EstablecimientoForm';

function renderForm(props: Partial<Parameters<typeof EstablecimientoForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(
    <EstablecimientoForm onSubmit={onSubmit} submitLabel="Crear establecimiento" {...props} />
  );
  return { onSubmit, user: userEvent.setup() };
}

describe('EstablecimientoForm', () => {
  it('agrupa los campos en cuatro secciones', () => {
    renderForm();

    ['Identificación', 'Regulatorio', 'Ubicación', 'Operación'].forEach((name) => {
      expect(screen.getByRole('group', { name })).toBeInTheDocument();
    });
  });

  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'EST001');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Central');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...ESTABLECIMIENTO_FORM_VACIO,
      codigo: 'EST001',
      nombre: 'Botica Central'
    });
  });

  it('permite completar los campos regulatorios, de ubicación y de operación', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'EST002');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Surco');
    await user.click(screen.getByLabelText('Es principal'));
    await user.clear(screen.getByLabelText('Anexo SUNAT'));
    await user.type(screen.getByLabelText('Anexo SUNAT'), '0002');
    await user.type(screen.getByLabelText('Código DIGEMID'), 'DIG002');
    await user.type(screen.getByLabelText('Latitud'), '-12.0464');
    await user.type(screen.getByLabelText('Longitud'), '-77.0428');
    await user.selectOptions(screen.getByLabelText('Perfil de operación'), 'STORE_EDGE');
    await user.click(screen.getByLabelText('Permite delivery'));
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        codigo: 'EST002',
        esPrincipal: true,
        codigoAnexoSunat: '0002',
        codigoDigemid: 'DIG002',
        latitud: '-12.0464',
        longitud: '-77.0428',
        perfilOperacion: 'STORE_EDGE',
        permiteDelivery: true
      })
    );
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Latitud'), '95');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('El nombre debe tener al menos 2 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('La latitud debe estar entre -90 y 90.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en edición precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...ESTABLECIMIENTO_FORM_VACIO, codigo: 'EST001', nombre: 'Botica Central' }
    });

    const codigo = screen.getByLabelText('Código');
    expect(codigo).toHaveValue('EST001');
    expect(codigo).toHaveAttribute('readonly');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ codigo: 'EST001' }));
  });

  it('en creación el código es editable', () => {
    renderForm();

    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({
      error: 'Ya existe un establecimiento con el código indicado.',
      isSubmitting: true
    });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un establecimiento con el código indicado.'
    );
    expect(screen.getByRole('button', { name: 'Crear establecimiento' })).toBeDisabled();
  });

  it('los campos de solo lectura y numéricos se distinguen de los editables', () => {
    renderForm();

    expect(screen.getByLabelText('Latitud')).toHaveAttribute('inputmode', 'decimal');
    expect(screen.getByLabelText('Longitud')).toHaveAttribute('inputmode', 'decimal');
    expect(screen.getByLabelText('Nombre')).not.toHaveAttribute('inputmode');
    expect(screen.getByLabelText('Nombre')).not.toHaveAttribute('readonly');
  });
});
