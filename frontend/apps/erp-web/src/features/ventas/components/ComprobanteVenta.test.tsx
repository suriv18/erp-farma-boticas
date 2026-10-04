import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { formatoFechaHora } from '../../../shared/lib/format';
import { sampleVenta, sampleVentaAnulada } from '../../../test/ventas-fixtures';
import { ComprobanteVenta } from './ComprobanteVenta';

describe('ComprobanteVenta', () => {
  it('muestra el rótulo no fiscal, los datos de la venta y los totales', () => {
    render(<ComprobanteVenta venta={sampleVenta} nombreEstablecimiento="Botica Centro" />);

    expect(
      screen.getByText('Comprobante interno — no válido como comprobante de pago')
    ).toBeInTheDocument();
    expect(screen.getByText('Botica Centro')).toBeInTheDocument();
    expect(screen.getByText('EST001-T01-000001')).toBeInTheDocument();
    expect(screen.getByText(formatoFechaHora(sampleVenta.fechaVenta))).toBeInTheDocument();
    expect(screen.getByText(/Paracetamol 500 mg/)).toBeInTheDocument();
    expect(screen.getByText(/2 × S\/ 12\.50/)).toBeInTheDocument();
    expect(screen.getByText('Subtotal').nextSibling).toHaveTextContent('S/ 21.19');
    expect(screen.getByText('IGV').nextSibling).toHaveTextContent('S/ 3.81');
    expect(screen.getByText('Total').nextSibling).toHaveTextContent('S/ 25.00');
    expect(screen.getByText('Recibido').nextSibling).toHaveTextContent('S/ 30.00');
    expect(screen.getByText('Vuelto').nextSibling).toHaveTextContent('S/ 5.00');
    expect(screen.getByText('Total').parentElement).toHaveClass('font-semibold');
    expect(screen.getByText('Subtotal').parentElement).not.toHaveClass('font-semibold');
    expect(screen.queryByText(/ANULADA/)).not.toBeInTheDocument();
  });

  it('imprime el comprobante', async () => {
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    render(<ComprobanteVenta venta={sampleVenta} nombreEstablecimiento="Botica Centro" />);

    await userEvent.click(screen.getByRole('button', { name: 'Imprimir comprobante' }));

    expect(print).toHaveBeenCalledTimes(1);
    print.mockRestore();
  });

  it('indica la anulación y su motivo', () => {
    render(<ComprobanteVenta venta={sampleVentaAnulada} nombreEstablecimiento="Botica Centro" />);

    expect(screen.getByText('ANULADA — Error de digitación')).toBeInTheDocument();
  });
});
