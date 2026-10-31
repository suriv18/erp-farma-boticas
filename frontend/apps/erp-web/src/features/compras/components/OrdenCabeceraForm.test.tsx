import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { CABECERA_VACIA } from '../lib/orden-cabecera';
import { OrdenCabeceraForm } from './OrdenCabeceraForm';

const establecimientos = (sampleEstructura.companies[0]?.establishments ?? []).slice(0, 2);

function renderForm(overrides: Partial<Parameters<typeof OrdenCabeceraForm>[0]> = {}) {
  const onCambiar = vi.fn();
  render(
    <OrdenCabeceraForm
      valores={CABECERA_VACIA}
      errores={{}}
      proveedores={[sampleProveedor]}
      establecimientos={establecimientos}
      onCambiar={onCambiar}
      {...overrides}
    />
  );
  return { onCambiar, user: userEvent.setup() };
}

describe('OrdenCabeceraForm', () => {
  it('ofrece los proveedores y los establecimientos', () => {
    renderForm();

    expect(screen.getByRole('option', { name: 'Laboratorios Perú SAC' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Botica Central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Botica Norte' })).toBeInTheDocument();
    expect(screen.getByLabelText('Moneda')).toHaveValue('PEN');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('0');
  });

  it('notifica cada campo con su nombre y valor', async () => {
    const { onCambiar, user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'prov-1');
    await user.selectOptions(screen.getByLabelText('Establecimiento de destino'), 'est-1');
    await user.type(screen.getByLabelText('Fecha de entrega estimada'), '2026-10-20');
    await user.type(screen.getByLabelText('Tipo de cambio'), '3');
    await user.type(screen.getByLabelText('Condición de pago'), 'C');
    await user.type(screen.getByLabelText('Observación'), 'O');

    expect(onCambiar).toHaveBeenCalledWith('proveedorId', 'prov-1');
    expect(onCambiar).toHaveBeenCalledWith('establecimientoDestinoId', 'est-1');
    expect(onCambiar).toHaveBeenCalledWith('fechaEntregaEstimada', '2026-10-20');
    expect(onCambiar).toHaveBeenCalledWith('tipoCambio', '3');
    expect(onCambiar).toHaveBeenCalledWith('condicionPago', 'C');
    expect(onCambiar).toHaveBeenCalledWith('observacion', 'O');
  });

  it('notifica los cambios de moneda y días de crédito', async () => {
    const { onCambiar, user } = renderForm();

    await user.selectOptions(screen.getByLabelText('Moneda'), 'USD');
    await user.type(screen.getByLabelText('Días de crédito'), '5');

    expect(screen.getAllByRole('option', { name: /^(PEN|USD)$/ })).toHaveLength(2);
    expect(onCambiar).toHaveBeenCalledWith('moneda', 'USD');
    expect(onCambiar).toHaveBeenCalledWith('diasCredito', '05');
  });

  it('avisa que al elegir proveedor se cargan sus condiciones', () => {
    renderForm();

    expect(
      screen.getByText(
        'Al elegir un proveedor se cargan su moneda y condiciones de pago; puedes modificarlas.'
      )
    ).toBeInTheDocument();
  });

  it('muestra los errores recibidos bajo cada campo', () => {
    renderForm({
      errores: {
        proveedorId: 'Selecciona un proveedor.',
        establecimientoDestinoId: 'Selecciona el establecimiento de destino.'
      }
    });

    expect(screen.getByText('Selecciona un proveedor.')).toBeInTheDocument();
    expect(screen.getByText('Selecciona el establecimiento de destino.')).toBeInTheDocument();
  });
});
