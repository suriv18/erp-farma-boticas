import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CABECERA_RECEPCION_VACIA } from '../lib/recepcion-cabecera';
import { RecepcionCabeceraForm } from './RecepcionCabeceraForm';

const almacenes = [
  { id: 'alm-1', code: 'ALM001', name: 'Almacén Central', status: 'ACTIVE' as const },
  { id: 'alm-2', code: 'ALM002', name: 'Almacén Frío', status: 'ACTIVE' as const }
];

function renderForm(overrides: Partial<Parameters<typeof RecepcionCabeceraForm>[0]> = {}) {
  const onCambiar = vi.fn();
  render(
    <RecepcionCabeceraForm
      valores={CABECERA_RECEPCION_VACIA}
      errores={{}}
      almacenes={almacenes}
      onCambiar={onCambiar}
      {...overrides}
    />
  );
  return { onCambiar, user: userEvent.setup() };
}

describe('RecepcionCabeceraForm', () => {
  it('ofrece los almacenes recibidos y los tipos de documento con factura por defecto', () => {
    renderForm();

    expect(screen.getByRole('option', { name: 'Selecciona un almacén' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Almacén Frío' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Factura' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Boleta de venta' })).toBeInTheDocument();
    expect(screen.getByLabelText('Tipo de documento')).toHaveValue('01');
  });

  it('notifica cada campo con su nombre y valor', async () => {
    const { onCambiar, user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Almacén'), 'alm-2');
    await user.selectOptions(screen.getByLabelText('Tipo de documento'), '03');
    await user.type(screen.getByLabelText('Serie del documento'), 'F');
    await user.type(screen.getByLabelText('Número del documento'), '1');
    await user.type(screen.getByLabelText('Guía de remisión del remitente'), 'T');
    await user.type(screen.getByLabelText('Guía de remisión del transportista'), 'V');
    await user.type(screen.getByLabelText('Temperatura (°C)'), '4');
    await user.type(screen.getByLabelText('Humedad relativa (%)'), '6');
    await user.type(screen.getByLabelText('Observación'), 'O');

    expect(onCambiar).toHaveBeenCalledWith('almacenId', 'alm-2');
    expect(onCambiar).toHaveBeenCalledWith('documentoProveedorTipo', '03');
    expect(onCambiar).toHaveBeenCalledWith('documentoProveedorSerie', 'F');
    expect(onCambiar).toHaveBeenCalledWith('documentoProveedorNumero', '1');
    expect(onCambiar).toHaveBeenCalledWith('guiaRemisionRemitente', 'T');
    expect(onCambiar).toHaveBeenCalledWith('guiaRemisionTransportista', 'V');
    expect(onCambiar).toHaveBeenCalledWith('temperatura', '4');
    expect(onCambiar).toHaveBeenCalledWith('humedad', '6');
    expect(onCambiar).toHaveBeenCalledWith('observacion', 'O');
  });

  it('muestra los errores recibidos bajo cada campo', () => {
    renderForm({
      errores: {
        almacenId: 'Selecciona el almacén de recepción.',
        temperatura: 'La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.'
      }
    });

    expect(screen.getByText('Selecciona el almacén de recepción.')).toBeInTheDocument();
    expect(
      screen.getByText('La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.')
    ).toBeInTheDocument();
  });
});
