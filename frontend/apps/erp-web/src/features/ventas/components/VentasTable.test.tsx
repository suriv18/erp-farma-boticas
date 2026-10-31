import { screen } from '@testing-library/react';
import { formatoFechaHora } from '../../../shared/lib/format';
import { renderRoute } from '../../../test/render-route';
import { sampleVentaResumen } from '../../../test/ventas-fixtures';
import { VentasTable } from './VentasTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 45,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(props: Partial<Parameters<typeof VentasTable>[0]> = {}) {
  return renderRoute(
    '/ventas',
    () => (
      <VentasTable
        rows={[sampleVentaResumen]}
        isLoading={false}
        isError={false}
        pagination={pagination}
        {...props}
      />
    ),
    '/ventas'
  );
}

describe('VentasTable', () => {
  it('muestra los datos de la venta y el enlace al detalle', () => {
    renderTable();

    expect(screen.getByText('EST001-T01-000001')).toBeInTheDocument();
    expect(screen.getByText(formatoFechaHora(sampleVentaResumen.fechaVenta))).toBeInTheDocument();
    expect(screen.getByText('S/ 25.00')).toBeInTheDocument();
    expect(screen.getByText('CONFIRMADA')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de la venta EST001-T01-000001' })
    ).toHaveAttribute('href', '/ventas/venta-1');
  });

  it('muestra el mensaje de lista vacía', () => {
    renderTable({ rows: [] });

    expect(screen.getByText('No hay ventas con los filtros indicados.')).toBeInTheDocument();
  });

  it('muestra el mensaje de error', () => {
    renderTable({ rows: [], isError: true });

    expect(screen.getByText('No se pudo cargar el historial de ventas.')).toBeInTheDocument();
  });

  it('muestra el estado de carga', () => {
    renderTable({ rows: [], isLoading: true });

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('notifica los cambios de página y de tamaño', async () => {
    const { user } = renderTable();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(pagination.onPageChange).toHaveBeenCalledWith(1);

    await user.selectOptions(screen.getByLabelText('Filas por página'), '50');
    expect(pagination.onSizeChange).toHaveBeenCalledWith(50);
  });
});
