import { render, screen } from '@testing-library/react';
import { sampleVenta } from '../../../test/ventas-fixtures';
import { LineasVentaTable } from './LineasVentaTable';

describe('LineasVentaTable', () => {
  it('muestra descripción, cantidad, precio, total y lotes de la línea', () => {
    render(<LineasVentaTable lineas={sampleVenta.lineas} />);

    expect(screen.getByText('Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByText('2 UND')).toBeInTheDocument();
    expect(screen.getByText('S/ 12.50')).toBeInTheDocument();
    expect(screen.getByText('S/ 25.00')).toBeInTheDocument();
    expect(screen.getByText('Lote lote-1 × 2')).toBeInTheDocument();
  });

  it('lista todos los lotes consumidos y muestra un guion si no hay', () => {
    const base = sampleVenta.lineas[0]!;
    render(
      <LineasVentaTable
        lineas={[
          {
            ...base,
            lotes: [
              { loteId: 'lote-1', cantidad: 1 },
              { loteId: 'lote-2', cantidad: 1 }
            ]
          },
          { ...base, numeroLinea: 2, lotes: [] }
        ]}
      />
    );

    expect(screen.getByText('Lote lote-1 × 1')).toBeInTheDocument();
    expect(screen.getByText('Lote lote-2 × 1')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('muestra el mensaje cuando no hay líneas', () => {
    render(<LineasVentaTable lineas={[]} />);

    expect(screen.getByText('La venta no tiene líneas.')).toBeInTheDocument();
  });
});
