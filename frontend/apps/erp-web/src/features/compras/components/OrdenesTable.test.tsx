import { screen } from '@testing-library/react';
import { sampleOrdenResumen } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { formatoMoneda } from '../../../shared/lib/format';
import { OrdenesTable } from './OrdenesTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 1,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(overrides: Partial<Parameters<typeof OrdenesTable>[0]> = {}) {
  const Pantalla = () => (
    <OrdenesTable
      rows={[sampleOrdenResumen]}
      isLoading={false}
      isError={false}
      pagination={pagination}
      {...overrides}
    />
  );
  return renderRoute('/compras/ordenes', Pantalla, '/compras/ordenes');
}

describe('OrdenesTable', () => {
  it('muestra número, proveedor, fechas, total, estado y enlace al detalle', () => {
    renderTable();

    expect(screen.getByText('OC-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByText(new Date(2026, 9, 3).toLocaleDateString('es-PE'))).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText(formatoMoneda(64.9).replace(/\s/g, ' '))).toBeInTheDocument();
    expect(screen.getByText('Emitida')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de la orden OC-2026-000001' })
    ).toHaveAttribute('href', '/compras/ordenes/orden-1');
  });

  it('muestra la fecha de entrega estimada cuando existe', () => {
    renderTable({ rows: [{ ...sampleOrdenResumen, fechaEntregaEstimada: '2026-10-20' }] });

    expect(screen.getByText(new Date(2026, 9, 20).toLocaleDateString('es-PE'))).toBeInTheDocument();
  });

  it('muestra el vacío', () => {
    renderTable({ rows: [] });
    expect(
      screen.getByText('No hay órdenes de compra con los filtros indicados.')
    ).toBeInTheDocument();
  });

  it('muestra el estado de carga', () => {
    renderTable({ rows: [], isLoading: true });
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra el error de carga', () => {
    renderTable({ rows: [], isError: true });
    expect(screen.getByText('No se pudo cargar el listado de órdenes.')).toBeInTheDocument();
  });
});
