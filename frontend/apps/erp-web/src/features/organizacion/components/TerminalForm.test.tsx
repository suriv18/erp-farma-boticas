import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TERMINAL_FORM_VACIO } from '../lib/form-defaults';
import { TerminalForm } from './TerminalForm';

function renderForm(props: Partial<Parameters<typeof TerminalForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<TerminalForm onSubmit={onSubmit} submitLabel="Crear terminal" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('TerminalForm', () => {
  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'POS001');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 1');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...TERMINAL_FORM_VACIO,
      codigo: 'POS001',
      nombre: 'Caja 1'
    });
  });

  it('permite completar series, equipo, IP e impresora', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'POS002');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 2');
    await user.type(screen.getByLabelText('Serie de boleta'), 'B002');
    await user.type(screen.getByLabelText('Serie de factura'), 'F002');
    await user.type(screen.getByLabelText('Número de serie del equipo'), 'SN-002');
    await user.type(screen.getByLabelText('Hostname'), 'caja-2');
    await user.type(screen.getByLabelText('Dirección IP'), '10.0.0.16');
    await user.type(screen.getByLabelText('Código de impresora'), 'IMP02');
    await user.click(screen.getByLabelText('Habilitar store edge'));
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        serieBoletaDefecto: 'B002',
        serieFacturaDefecto: 'F002',
        numeroSerieEquipo: 'SN-002',
        hostname: 'caja-2',
        ipEquipo: '10.0.0.16',
        impresoraCodigo: 'IMP02',
        storeEdgeHabilitado: true
      })
    );
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Serie de boleta'), 'X001');
    await user.type(screen.getByLabelText('Dirección IP'), '999.1.1.1');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(
      screen.getByText('La serie de boleta debe iniciar con B y tener 4 caracteres.')
    ).toBeInTheDocument();
    expect(screen.getByText('La dirección IP no es válida.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en creación no muestra el estado y el código es editable', () => {
    renderForm();

    expect(screen.queryByLabelText('Estado')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('en edición muestra el estado, precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...TERMINAL_FORM_VACIO, codigo: 'POS001', nombre: 'Caja 1' }
    });

    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    await user.selectOptions(screen.getByLabelText('Estado'), 'MANTENIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'POS001', estado: 'MANTENIMIENTO' })
    );
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe un terminal con el código indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un terminal con el código indicado.'
    );
    expect(screen.getByRole('button', { name: 'Crear terminal' })).toBeDisabled();
  });
});
