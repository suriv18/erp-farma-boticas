import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { formatoImporte } from '../lib/formato-compras';
import { lineaDesdeSku } from '../lib/orden-calculo';
import { LineasEditor } from './LineasEditor';

const importe = (valor: number) => formatoImporte(valor, 'PEN').replace(/\s/g, ' ');

const linea = { ...lineaDesdeSku(sampleSkuVenta, true), precio: '5.5', impuesto: '0.99' };

function renderEditor(lineas = [linea], mostrarErrores = false) {
  const onCambiar = vi.fn();
  const onQuitar = vi.fn();
  const onRestablecerImpuesto = vi.fn();
  render(
    <LineasEditor
      lineas={lineas}
      moneda="PEN"
      mostrarErrores={mostrarErrores}
      onCambiar={onCambiar}
      onQuitar={onQuitar}
      onRestablecerImpuesto={onRestablecerImpuesto}
    />
  );
  return { onCambiar, onQuitar, onRestablecerImpuesto, user: userEvent.setup() };
}

describe('LineasEditor', () => {
  it('muestra el vacío y la nota del IGV sugerido', () => {
    renderEditor([]);

    expect(screen.getByText('Aún no agregaste productos.')).toBeInTheDocument();
    expect(
      screen.getByText(
        'El impuesto se sugiere al 18% en los productos afectos a IGV y puedes corregirlo.'
      )
    ).toBeInTheDocument();
  });

  it('muestra los valores de la línea y su total', () => {
    renderEditor();

    expect(screen.getByText('MED-001 — Paracetamol 500 mg')).toBeInTheDocument();
    expect(screen.getByLabelText('Cantidad de MED-001')).toHaveValue('1');
    expect(screen.getByLabelText('Precio de MED-001')).toHaveValue('5.5');
    expect(screen.getByLabelText('Descuento de MED-001')).toHaveValue('0');
    expect(screen.getByLabelText('Impuesto de MED-001')).toHaveValue('0.99');
    expect(screen.getByLabelText('Tolerancia de exceso de MED-001')).toHaveValue('0');
    expect(screen.getByLabelText('Tolerancia de defecto de MED-001')).toHaveValue('0');
    expect(screen.getByText(importe(6.49))).toBeInTheDocument();
  });

  it('notifica cada cambio con el SKU y el campo editado', async () => {
    const { onCambiar, user } = renderEditor();

    await user.type(screen.getByLabelText('Cantidad de MED-001'), '2');
    await user.type(screen.getByLabelText('Precio de MED-001'), '1');
    await user.type(screen.getByLabelText('Descuento de MED-001'), '1');
    await user.type(screen.getByLabelText('Impuesto de MED-001'), '1');
    await user.type(screen.getByLabelText('Tolerancia de exceso de MED-001'), '1');
    await user.type(screen.getByLabelText('Tolerancia de defecto de MED-001'), '1');

    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { cantidad: '12' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { precio: '5.51' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { descuento: '01' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { impuesto: '0.991' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { toleranciaExceso: '01' });
    expect(onCambiar).toHaveBeenCalledWith('sku-0001-aaaa', { toleranciaDefecto: '01' });
  });

  it('quita una línea', async () => {
    const { onQuitar, user } = renderEditor();

    await user.click(screen.getByRole('button', { name: 'Quitar MED-001' }));

    expect(onQuitar).toHaveBeenCalledWith('sku-0001-aaaa');
  });

  it('muestra el error de una línea inválida y total cero cuando se piden los errores', () => {
    renderEditor([{ ...linea, cantidad: '' }], true);

    expect(screen.getByRole('alert')).toHaveTextContent(
      'La cantidad debe ser mayor que cero con hasta 4 decimales.'
    );
    expect(screen.getByText(importe(0))).toBeInTheDocument();
  });

  it('no muestra el error de una línea inválida antes de intentar crear', () => {
    renderEditor([{ ...linea, cantidad: '' }]);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByText(importe(0))).toBeInTheDocument();
  });

  it('ofrece volver al impuesto sugerido solo si se editó a mano', async () => {
    const { onRestablecerImpuesto, user } = renderEditor([{ ...linea, impuestoManual: true }]);

    await user.click(screen.getByRole('button', { name: 'Usar impuesto sugerido de MED-001' }));

    expect(onRestablecerImpuesto).toHaveBeenCalledWith('sku-0001-aaaa');
  });

  it('no ofrece el impuesto sugerido mientras sigue siendo automático', () => {
    renderEditor();

    expect(
      screen.queryByRole('button', { name: 'Usar impuesto sugerido de MED-001' })
    ).not.toBeInTheDocument();
  });
});
