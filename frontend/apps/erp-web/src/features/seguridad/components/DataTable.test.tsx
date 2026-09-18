import { render, screen } from '@testing-library/react';
import { DataTable } from './DataTable';

type Row = { id: string; name: string };

const columns = [
  { header: 'Nombre', cell: (row: Row) => row.name }
];

describe('DataTable', () => {
  it('renderiza una fila por cada elemento', () => {
    render(
      <DataTable
        columns={columns}
        rows={[{ id: '1', name: 'Ana' }, { id: '2', name: 'Luis' }]}
        rowKey={(row: Row) => row.id}
        emptyMessage="Sin registros"
      />
    );
    expect(screen.getByText('Ana')).toBeInTheDocument();
    expect(screen.getByText('Luis')).toBeInTheDocument();
  });

  it('muestra el mensaje vacio cuando no hay filas', () => {
    render(<DataTable columns={columns} rows={[]} rowKey={(row: Row) => row.id} emptyMessage="Sin registros" />);
    expect(screen.getByText('Sin registros')).toBeInTheDocument();
  });

  it('muestra un indicador de carga', () => {
    render(
      <DataTable columns={columns} rows={[]} rowKey={(row: Row) => row.id} emptyMessage="Sin registros" isLoading />
    );
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra un mensaje de error', () => {
    render(
      <DataTable
        columns={columns}
        rows={[]}
        rowKey={(row: Row) => row.id}
        emptyMessage="Sin registros"
        isError
        errorMessage="No se pudo cargar la información."
      />
    );
    expect(screen.getByText('No se pudo cargar la información.')).toBeInTheDocument();
  });
});
