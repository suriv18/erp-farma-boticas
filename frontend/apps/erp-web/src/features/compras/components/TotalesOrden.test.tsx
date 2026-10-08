import { render, screen } from '@testing-library/react';
import { formatoImporte } from '../lib/formato-compras';
import { TotalesOrden } from './TotalesOrden';

const importe = (valor: number) => formatoImporte(valor, 'PEN').replace(/\s/g, ' ');

describe('TotalesOrden', () => {
  it('muestra subtotal, descuento, impuesto y total en la moneda de la orden', () => {
    render(
      <TotalesOrden totales={{ subtotal: 55, descuento: 5, impuesto: 9, total: 59 }} moneda="PEN" />
    );

    expect(screen.getByText('Subtotal').nextElementSibling).toHaveTextContent(importe(55));
    expect(screen.getByText('Descuento').nextElementSibling).toHaveTextContent(importe(5));
    expect(screen.getByText('Impuesto').nextElementSibling).toHaveTextContent(importe(9));
    expect(screen.getByText('Total').nextElementSibling).toHaveTextContent(importe(59));
  });
});
