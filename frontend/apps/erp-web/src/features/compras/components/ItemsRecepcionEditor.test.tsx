import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ItemBorrador } from '../lib/recepcion-items';
import { ItemsRecepcionEditor } from './ItemsRecepcionEditor';

const HOY = '2026-10-08';
const ITEM: ItemBorrador = {
  numeroLineaOrden: 1,
  descripcion: 'Paracetamol 500 mg',
  unidadMedidaCodigo: 'UND',
  cantidadPendiente: 10,
  numeroLote: '',
  fechaFabricacion: '',
  fechaVencimiento: '',
  cantidadRecibida: '',
  cantidadRechazada: '0',
  motivoRechazo: '',
  costoUnitario: '5.5'
};

function renderEditor(items: ItemBorrador[] = [ITEM], mostrarErrores = false) {
  const onCambiar = vi.fn();
  render(
    <ItemsRecepcionEditor
      items={items}
      hoy={HOY}
      mostrarErrores={mostrarErrores}
      onCambiar={onCambiar}
    />
  );
  return { onCambiar, user: userEvent.setup() };
}

const bloque = () => within(screen.getByRole('group', { name: 'Línea 1 — Paracetamol 500 mg' }));

describe('ItemsRecepcionEditor', () => {
  it('muestra un bloque por línea con lo pendiente y el costo de la orden', () => {
    renderEditor();

    expect(bloque().getByText('Pendiente: 10 UND')).toBeInTheDocument();
    expect(bloque().getByLabelText('Costo unitario')).toHaveValue('5.5');
    expect(bloque().getByLabelText('Cantidad rechazada')).toHaveValue('0');
    expect(
      screen.getByText(
        'Solo se registran las líneas con cantidad recibida. El costo unitario parte del precio de la orden.'
      )
    ).toBeInTheDocument();
  });

  it('notifica cada cambio con el número de línea y el campo editado', async () => {
    const { onCambiar, user } = renderEditor();

    await user.type(bloque().getByLabelText('Número de lote'), 'X');
    await user.type(bloque().getByLabelText('Fecha de fabricación'), '2026-01-01');
    await user.type(bloque().getByLabelText('Fecha de vencimiento'), '2099-12-31');
    await user.type(bloque().getByLabelText('Cantidad recibida'), '4');
    await user.type(bloque().getByLabelText('Cantidad rechazada'), '1');
    await user.type(bloque().getByLabelText('Motivo del rechazo'), 'M');
    await user.type(bloque().getByLabelText('Costo unitario'), '1');

    expect(onCambiar).toHaveBeenCalledWith(1, { numeroLote: 'X' });
    expect(onCambiar).toHaveBeenCalledWith(1, { fechaFabricacion: '2026-01-01' });
    expect(onCambiar).toHaveBeenCalledWith(1, { fechaVencimiento: '2099-12-31' });
    expect(onCambiar).toHaveBeenCalledWith(1, { cantidadRecibida: '4' });
    expect(onCambiar).toHaveBeenCalledWith(1, { cantidadRechazada: '01' });
    expect(onCambiar).toHaveBeenCalledWith(1, { motivoRechazo: 'M' });
    expect(onCambiar).toHaveBeenCalledWith(1, { costoUnitario: '5.51' });
  });

  it('muestra el error de una línea con cantidad cuando se piden los errores', () => {
    renderEditor([{ ...ITEM, cantidadRecibida: '4' }], true);

    expect(bloque().getByRole('alert')).toHaveTextContent(
      'El número de lote es obligatorio y admite hasta 120 caracteres.'
    );
  });

  it('no valida las líneas sin cantidad recibida', () => {
    renderEditor([ITEM], true);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('no muestra errores antes de intentar registrar', () => {
    renderEditor([{ ...ITEM, cantidadRecibida: '4' }]);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('avisa cuando no quedan líneas pendientes', () => {
    renderEditor([]);

    expect(screen.getByText('La orden no tiene líneas pendientes de recibir.')).toBeInTheDocument();
  });
});
