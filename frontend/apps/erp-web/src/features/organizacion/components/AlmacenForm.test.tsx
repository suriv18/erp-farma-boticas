import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ALMACEN_FORM_VACIO } from '../lib/form-defaults';
import { AlmacenForm } from './AlmacenForm';

function renderForm(props: Partial<Parameters<typeof AlmacenForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<AlmacenForm onSubmit={onSubmit} submitLabel="Crear almacén" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('AlmacenForm', () => {
  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...ALMACEN_FORM_VACIO,
      codigo: 'ALM001',
      nombre: 'Almacén Central'
    });
  });

  it('al elegir refrigerado marca controlar temperatura y permite editar las temperaturas', async () => {
    const { onSubmit, user } = renderForm();

    expect(screen.getByLabelText('Temperatura mínima (°C)')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Temperatura máxima (°C)')).toHaveAttribute('readonly');
    await user.type(screen.getByLabelText('Código'), 'ALM002');
    await user.type(screen.getByLabelText('Nombre'), 'Cadena de frío');
    await user.selectOptions(screen.getByLabelText('Tipo de almacén'), 'REFRIGERADO');

    expect(screen.getByLabelText('Controla temperatura')).toBeChecked();
    expect(screen.getByLabelText('Temperatura mínima (°C)')).not.toHaveAttribute('readonly');
    await user.click(screen.getByLabelText('Permite venta'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), '2');
    await user.type(screen.getByLabelText('Temperatura máxima (°C)'), '8');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        tipo: 'REFRIGERADO',
        permiteVenta: true,
        controlTemperatura: true,
        temperaturaMinC: '2',
        temperaturaMaxC: '8'
      })
    );
  });

  it('no marca controlar temperatura al elegir un tipo distinto de refrigerado', async () => {
    const { user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Tipo de almacén'), 'CUARENTENA');

    expect(screen.getByLabelText('Controla temperatura')).not.toBeChecked();
  });

  it('deja las temperaturas de solo lectura cuando se desmarca controlar temperatura', async () => {
    const { user } = renderForm();

    await user.click(screen.getByLabelText('Controla temperatura'));
    expect(screen.getByLabelText('Temperatura mínima (°C)')).not.toHaveAttribute('readonly');
    await user.click(screen.getByLabelText('Controla temperatura'));

    expect(screen.getByLabelText('Temperatura mínima (°C)')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Temperatura máxima (°C)')).toHaveAttribute('readonly');
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), 'frio');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('El nombre debe tener al menos 2 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('La temperatura mínima debe ser un número.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error de refrigerado sin controlar temperatura junto a la casilla', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM003');
    await user.type(screen.getByLabelText('Nombre'), 'Cámara fría');
    await user.selectOptions(screen.getByLabelText('Tipo de almacén'), 'REFRIGERADO');
    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(
      await screen.findByText('Un almacén refrigerado debe controlar temperatura.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error cuando la mínima es mayor que la máxima', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM004');
    await user.type(screen.getByLabelText('Nombre'), 'Rango invertido');
    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), '8');
    await user.type(screen.getByLabelText('Temperatura máxima (°C)'), '2');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(
      await screen.findByText('La temperatura mínima no puede ser mayor que la máxima.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en creación no muestra el campo activo y el código es editable', () => {
    renderForm();

    expect(screen.queryByLabelText('Almacén activo')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('en edición muestra activo, precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...ALMACEN_FORM_VACIO, codigo: 'ALM001', nombre: 'Almacén Central' }
    });

    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    const activo = screen.getByLabelText('Almacén activo');
    expect(activo).toBeChecked();
    await user.click(activo);
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'ALM001', activo: false })
    );
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe un almacén con el código indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un almacén con el código indicado.'
    );
    expect(screen.getByRole('button', { name: 'Crear almacén' })).toBeDisabled();
  });
});
