import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { FiltrosOrdenes } from './FiltrosOrdenes';

const filtros = { proveedorId: '', estado: '', page: 0, size: 20 };

function renderFiltros(overrides: Partial<Parameters<typeof FiltrosOrdenes>[0]> = {}) {
  const onProveedor = vi.fn();
  const onEstado = vi.fn();
  render(
    <FiltrosOrdenes
      proveedores={[sampleProveedor]}
      filtros={filtros}
      onProveedor={onProveedor}
      onEstado={onEstado}
      {...overrides}
    />
  );
  return { onProveedor, onEstado, user: userEvent.setup() };
}

describe('FiltrosOrdenes', () => {
  it('ofrece los proveedores y las etiquetas de estado', () => {
    renderFiltros();

    expect(screen.getByRole('option', { name: 'Laboratorios Perú SAC' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'En aprobación' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Parcialmente recibida' })).toBeInTheDocument();
  });

  it('notifica el proveedor y el estado elegidos', async () => {
    const { onProveedor, onEstado, user } = renderFiltros();

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'prov-1');
    await user.selectOptions(screen.getByLabelText('Estado'), 'EMITIDA');

    expect(onProveedor).toHaveBeenCalledWith('prov-1');
    expect(onEstado).toHaveBeenCalledWith('EMITIDA');
  });

  it('refleja los filtros recibidos', () => {
    renderFiltros({ filtros: { ...filtros, proveedorId: 'prov-1', estado: 'APROBADA' } });

    expect(screen.getByLabelText('Proveedor')).toHaveValue('prov-1');
    expect(screen.getByLabelText('Estado')).toHaveValue('APROBADA');
  });
});
