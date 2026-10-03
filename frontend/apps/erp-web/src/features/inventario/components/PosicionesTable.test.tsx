import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { samplePosicion } from '../../../test/inventario-fixtures';
import { PosicionesTable } from './PosicionesTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 2,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(props: Partial<Parameters<typeof PosicionesTable>[0]> = {}) {
  const onAjustar = vi.fn();
  render(
    <MemoryRouter>
      <PosicionesTable
        rows={[
          samplePosicion,
          {
            ...samplePosicion,
            id: 'pos-2',
            loteId: 'lote-2',
            numeroLote: 'L002',
            estadoLote: 'BLOQUEADO',
            vendible: false,
            cantidadFisica: 8,
            cantidadReservada: 0,
            cantidadDisponible: 8
          }
        ]}
        isLoading={false}
        isError={false}
        pagination={pagination}
        onAjustar={onAjustar}
        {...props}
      />
    </MemoryRouter>
  );
  return onAjustar;
}

describe('PosicionesTable', () => {
  it('muestra lote, cantidades, estado y acciones de cada posición', () => {
    renderTable();

    expect(screen.getByText('L001')).toBeInTheDocument();
    expect(screen.getAllByText('sku-0001')).toHaveLength(2);
    const fila = within(screen.getByText('L001').closest('tr') as HTMLElement);
    expect(fila.getByText('100')).toBeInTheDocument();
    expect(fila.getByText('90')).toBeInTheDocument();
    expect(screen.getByText('HABILITADO')).toBeInTheDocument();
    expect(screen.getByText('BLOQUEADO')).toBeInTheDocument();
    expect(screen.getAllByText('No vendible')).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'Ver detalle del lote L002' })).toHaveAttribute(
      'href',
      '/inventario/lotes/lote-2'
    );
  });

  it('dispara el ajuste con la posición de la fila', async () => {
    const onAjustar = renderTable();

    await userEvent.click(screen.getByRole('button', { name: 'Ajustar stock del lote L001' }));

    expect(onAjustar).toHaveBeenCalledWith(samplePosicion);
  });

  it('muestra el mensaje de lista vacía', () => {
    renderTable({ rows: [] });

    expect(
      screen.getByText('No hay stock registrado con los filtros indicados.')
    ).toBeInTheDocument();
  });

  it('muestra el mensaje de error', () => {
    renderTable({ rows: [], isError: true });

    expect(screen.getByText('No se pudo cargar el inventario.')).toBeInTheDocument();
  });
});
