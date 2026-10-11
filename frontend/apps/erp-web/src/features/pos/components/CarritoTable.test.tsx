import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { lineaDesdeSku } from '../lib/carrito';
import { CarritoTable } from './CarritoTable';

const linea = lineaDesdeSku(sampleSkuVenta);

function renderTabla(carrito = [linea]) {
  const onCambiar = vi.fn();
  const onQuitar = vi.fn();
  render(<CarritoTable carrito={carrito} onCambiar={onCambiar} onQuitar={onQuitar} />);
  return { onCambiar, onQuitar, user: userEvent.setup() };
}

describe('CarritoTable', () => {
  it('muestra el vacío cuando no hay líneas', () => {
    renderTabla([]);

    expect(screen.getByText('El carrito está vacío.')).toBeInTheDocument();
  });

  it('muestra la línea con cantidad, precio y total', () => {
    renderTabla();

    expect(screen.getByText('MED-001 — Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByLabelText('Cantidad de MED-001')).toHaveValue('1');
    expect(screen.getByLabelText('Precio de MED-001')).toHaveValue('12.5');
    expect(screen.getByText('S/ 12.50')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('notifica los cambios de cantidad y precio', async () => {
    const { onCambiar, user } = renderTabla([{ ...linea, cantidad: '', precio: '' }]);

    await user.type(screen.getByLabelText('Cantidad de MED-001'), '3');
    await user.type(screen.getByLabelText('Precio de MED-001'), '1');

    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { cantidad: '3' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { precio: '1' });
  });

  it('notifica al quitar una línea', async () => {
    const { onQuitar, user } = renderTabla();

    await user.click(screen.getByRole('button', { name: 'Quitar MED-001' }));

    expect(onQuitar).toHaveBeenCalledWith('sku-0001-aaaa');
  });

  it('muestra el error de una línea inválida con total cero', () => {
    renderTabla([{ ...linea, cantidad: '' }]);

    expect(screen.getByRole('alert')).toHaveTextContent(
      'La cantidad debe ser mayor que cero con hasta 4 decimales.'
    );
    expect(screen.getByText('S/ 0.00')).toBeInTheDocument();
  });
});
