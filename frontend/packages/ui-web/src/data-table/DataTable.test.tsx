import { render, screen } from '@testing-library/react';
import { DataTable } from './DataTable';

type Row = { id: string; nombre: string };

const rows: Row[] = [
  { id: 'a', nombre: 'Alfa' },
  { id: 'b', nombre: 'Beta' }
];

describe('DataTable', () => {
  it('integra la navegación fuera de la región desplazable y la bloquea durante carga o error', () => {
    const props = {
      columns: [{ header: 'Nombre', cell: (row: Row) => row.nombre }],
      rows,
      rowKey: (row: Row) => row.id,
      emptyMessage: 'Sin filas.',
      pagination: { page: 0, size: 20, totalElements: 45, onPageChange: vi.fn() }
    };
    const { rerender } = render(<DataTable {...props} />);
    const region = screen.getByRole('region', { name: 'Tabla de resultados' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(region).not.toContainElement(screen.getByRole('navigation', { name: 'Paginación' }));
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeEnabled();
    rerender(<DataTable {...props} isLoading />);
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    rerender(<DataTable {...props} isError />);
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    rerender(<DataTable {...props} pagination={{ ...props.pagination, disabled: true }} />);
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });
  it('renderiza una fila por cada elemento', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByText('Alfa')).toBeInTheDocument();
    expect(screen.getByText('Beta')).toBeInTheDocument();
  });

  it('muestra el mensaje vacio cuando no hay filas', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByText('Sin filas.')).toBeInTheDocument();
  });

  it('muestra un indicador de carga', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
        isLoading
      />
    );
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra un mensaje de error', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
        isError
        errorMessage="No se pudo cargar la información."
      />
    );
    expect(screen.getByText('No se pudo cargar la información.')).toBeInTheDocument();
  });

  it('expone el indice de fila a cell para armar una columna N°', () => {
    render(
      <DataTable<Row>
        columns={[
          { header: 'N°', cell: (_row, index) => index + 1 },
          { header: 'Nombre', cell: (row) => row.nombre }
        ]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('aplica startIndex para numerar correctamente en paginas siguientes', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'N°', cell: (_row, index) => index + 1 }]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
        startIndex={20}
      />
    );
    expect(screen.getByText('21')).toBeInTheDocument();
    expect(screen.getByText('22')).toBeInTheDocument();
  });

  it('permite una columna de Acciones con contenido interactivo por fila', () => {
    render(
      <DataTable<Row>
        columns={[
          { header: 'Nombre', cell: (row) => row.nombre },
          { header: 'Acciones', cell: (row) => <button type="button">Editar {row.nombre}</button> }
        ]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByRole('button', { name: 'Editar Alfa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Editar Beta' })).toBeInTheDocument();
  });
});
