import { screen } from '@testing-library/react';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { ProveedoresTable } from './ProveedoresTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 1,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(overrides: Partial<Parameters<typeof ProveedoresTable>[0]> = {}) {
  const Pantalla = () => (
    <ProveedoresTable
      rows={[sampleProveedor]}
      isLoading={false}
      isError={false}
      pagination={pagination}
      {...overrides}
    />
  );
  return renderRoute('/compras/proveedores', Pantalla, '/compras/proveedores');
}

describe('ProveedoresTable', () => {
  it('muestra documento, razón social, roles, condición, estado y enlace al detalle', () => {
    renderTable();

    expect(screen.getByText('20100070970')).toBeInTheDocument();
    expect(screen.getByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByText('Laboratorio')).toBeInTheDocument();
    expect(screen.getByText('Distribuidor')).toBeInTheDocument();
    expect(screen.getByText('CREDITO 30')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de Laboratorios Perú SAC' })
    ).toHaveAttribute('href', '/compras/proveedores/prov-1');
  });

  it('muestra guiones cuando no hay roles ni condición de pago', () => {
    renderTable({
      rows: [
        {
          ...sampleProveedor,
          esLaboratorio: false,
          esDistribuidor: false,
          condicionPagoDefault: null
        }
      ]
    });

    expect(screen.getAllByText('—')).toHaveLength(2);
  });

  it('muestra el vacío', () => {
    renderTable({ rows: [] });
    expect(screen.getByText('No hay proveedores con los filtros indicados.')).toBeInTheDocument();
  });

  it('muestra el estado de carga', () => {
    renderTable({ rows: [], isLoading: true });
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra el error de carga', () => {
    renderTable({ rows: [], isError: true });
    expect(screen.getByText('No se pudo cargar el listado de proveedores.')).toBeInTheDocument();
  });
});
