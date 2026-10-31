import { render, screen } from '@testing-library/react';
import { sampleOrden } from '../../../test/compras-fixtures';
import { formatoImporte } from '../lib/formato-compras';
import { LineasOrdenTable } from './LineasOrdenTable';

const importe = (valor: number) => formatoImporte(valor, 'PEN').replace(/\s/g, ' ');

describe('LineasOrdenTable', () => {
  it('muestra producto, cantidades, importes y el recibido y pendiente de cada línea', () => {
    render(<LineasOrdenTable lineas={sampleOrden.lineas} moneda="PEN" />);

    expect(screen.getByText('Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByText('10 UND')).toBeInTheDocument();
    expect(screen.getByText(importe(5.5))).toBeInTheDocument();
    expect(screen.getByText(importe(9.9))).toBeInTheDocument();
    expect(screen.getByText(importe(64.9))).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '0' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '10' })).toBeInTheDocument();
  });

  it('muestra el vacío cuando no hay líneas', () => {
    render(<LineasOrdenTable lineas={[]} moneda="PEN" />);

    expect(screen.getByText('La orden no tiene líneas.')).toBeInTheDocument();
  });
});
