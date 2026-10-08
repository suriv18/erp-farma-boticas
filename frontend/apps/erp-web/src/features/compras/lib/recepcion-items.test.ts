import { sampleOrden } from '../../../test/compras-fixtures';
import {
  actualizarItem,
  errorItem,
  itemIncluido,
  itemsDesdeOrden,
  toItemsPayload,
  type ItemBorrador
} from './recepcion-items';

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
const valido = (cambios: Partial<ItemBorrador> = {}): ItemBorrador => ({
  ...ITEM,
  numeroLote: 'L2026-01',
  fechaVencimiento: '2027-12-31',
  cantidadRecibida: '4',
  ...cambios
});

describe('itemsDesdeOrden', () => {
  it('arma un ítem por línea pendiente con el costo por defecto del precio de la orden', () => {
    expect(itemsDesdeOrden(sampleOrden)).toEqual([ITEM]);
  });

  it('omite las líneas sin pendiente', () => {
    const orden = {
      ...sampleOrden,
      lineas: sampleOrden.lineas.flatMap((linea) => [
        { ...linea, cantidadRecibida: 10, cantidadPendiente: 0 },
        {
          ...linea,
          numeroLinea: 2,
          descripcion: 'Ibuprofeno 400 mg',
          precioUnitario: 3.25,
          cantidadRecibida: 4,
          cantidadPendiente: 6
        }
      ])
    };

    expect(itemsDesdeOrden(orden)).toEqual([
      {
        ...ITEM,
        numeroLineaOrden: 2,
        descripcion: 'Ibuprofeno 400 mg',
        cantidadPendiente: 6,
        costoUnitario: '3.25'
      }
    ]);
  });
});

describe('actualizarItem e itemIncluido', () => {
  it('cambia solo el ítem indicado', () => {
    const otro = { ...ITEM, numeroLineaOrden: 2 };

    const resultado = actualizarItem([ITEM, otro], 1, { numeroLote: 'L1', cantidadRecibida: '3' });

    expect(resultado[0]).toEqual({ ...ITEM, numeroLote: 'L1', cantidadRecibida: '3' });
    expect(resultado[1]).toBe(otro);
  });

  it.each(['', '   ', '0', '0.0'])(
    'no incluye un ítem con cantidad recibida "%s"',
    (cantidadRecibida) => {
      expect(itemIncluido({ ...ITEM, cantidadRecibida })).toBe(false);
    }
  );

  it.each(['4', ' 4 ', 'abc', '-1'])(
    'incluye un ítem con cantidad recibida "%s"',
    (cantidadRecibida) => {
      expect(itemIncluido({ ...ITEM, cantidadRecibida })).toBe(true);
    }
  );
});

describe('errorItem', () => {
  it('no reporta error en un ítem válido', () => {
    expect(errorItem(valido(), HOY)).toBeNull();
    expect(errorItem(valido({ fechaFabricacion: '2026-01-01' }), HOY)).toBeNull();
    expect(errorItem(valido({ fechaVencimiento: HOY }), HOY)).toBeNull();
    expect(errorItem(valido({ costoUnitario: '0' }), HOY)).toBeNull();
  });

  it.each(['', '   ', 'L'.repeat(121)])('rechaza el lote "%s"', (numeroLote) => {
    expect(errorItem(valido({ numeroLote }), HOY)).toBe(
      'El número de lote es obligatorio y admite hasta 120 caracteres.'
    );
  });

  it('exige la fecha de vencimiento', () => {
    expect(errorItem(valido({ fechaVencimiento: '' }), HOY)).toBe(
      'La fecha de vencimiento es obligatoria.'
    );
  });

  it.each(['2027-12-31', '2028-01-01'])(
    'exige un vencimiento posterior a la fabricación %s',
    (fechaFabricacion) => {
      expect(errorItem(valido({ fechaFabricacion }), HOY)).toBe(
        'La fecha de vencimiento debe ser posterior a la de fabricación.'
      );
    }
  );

  it.each(['abc', '-1', '1.23456', '0'])(
    'rechaza la cantidad recibida "%s"',
    (cantidadRecibida) => {
      expect(errorItem(valido({ cantidadRecibida }), HOY)).toBe(
        'La cantidad recibida debe ser mayor que cero con hasta 4 decimales.'
      );
    }
  );

  it.each(['', '-1', '4.5', '0.00001'])(
    'rechaza la cantidad rechazada "%s"',
    (cantidadRechazada) => {
      expect(errorItem(valido({ cantidadRechazada }), HOY)).toBe(
        'La cantidad rechazada debe estar entre 0 y la cantidad recibida con hasta 4 decimales.'
      );
    }
  );

  it('exige el motivo cuando hay rechazo', () => {
    expect(errorItem(valido({ cantidadRechazada: '1' }), HOY)).toBe(
      'Indica el motivo del rechazo.'
    );
    expect(errorItem(valido({ cantidadRechazada: '1', motivoRechazo: '   ' }), HOY)).toBe(
      'Indica el motivo del rechazo.'
    );
    expect(
      errorItem(valido({ cantidadRechazada: '1', motivoRechazo: 'Empaque dañado' }), HOY)
    ).toBeNull();
  });

  it('limita el motivo a 1000 caracteres solo cuando hay rechazo', () => {
    expect(
      errorItem(valido({ cantidadRechazada: '1', motivoRechazo: 'm'.repeat(1001) }), HOY)
    ).toBe('El motivo del rechazo admite hasta 1000 caracteres.');
    expect(errorItem(valido({ motivoRechazo: 'm'.repeat(1001) }), HOY)).toBeNull();
  });

  it.each(['', '-1', '1.1234567', 'abc'])('rechaza el costo "%s"', (costoUnitario) => {
    expect(errorItem(valido({ costoUnitario }), HOY)).toBe(
      'El costo unitario debe ser mayor o igual a cero con hasta 6 decimales.'
    );
  });

  it('no ingresa a inventario un lote vencido salvo que se rechace por completo', () => {
    expect(errorItem(valido({ fechaVencimiento: '2026-10-07' }), HOY)).toBe(
      'No se puede ingresar un lote vencido; recházalo por completo o corrige la fecha.'
    );
    expect(
      errorItem(
        valido({
          fechaVencimiento: '2026-10-07',
          cantidadRechazada: '4',
          motivoRechazo: 'Vencido'
        }),
        HOY
      )
    ).toBeNull();
  });
});

describe('toItemsPayload', () => {
  it('envía solo los ítems recibidos, con números, lote recortado y motivo solo si hay rechazo', () => {
    expect(
      toItemsPayload([
        valido({ numeroLote: ' L1 ', fechaFabricacion: '2026-01-01' }),
        { ...ITEM, numeroLineaOrden: 2 },
        valido({ numeroLineaOrden: 3, cantidadRechazada: '1', motivoRechazo: ' Empaque dañado ' }),
        valido({ numeroLineaOrden: 4, motivoRechazo: 'ignorado' })
      ])
    ).toEqual([
      {
        numeroLineaOrden: 1,
        numeroLote: 'L1',
        fechaFabricacion: '2026-01-01',
        fechaVencimiento: '2027-12-31',
        cantidadRecibida: 4,
        cantidadRechazada: 0,
        motivoRechazo: undefined,
        costoUnitario: 5.5
      },
      {
        numeroLineaOrden: 3,
        numeroLote: 'L2026-01',
        fechaFabricacion: undefined,
        fechaVencimiento: '2027-12-31',
        cantidadRecibida: 4,
        cantidadRechazada: 1,
        motivoRechazo: 'Empaque dañado',
        costoUnitario: 5.5
      },
      {
        numeroLineaOrden: 4,
        numeroLote: 'L2026-01',
        fechaFabricacion: undefined,
        fechaVencimiento: '2027-12-31',
        cantidadRecibida: 4,
        cantidadRechazada: 0,
        motivoRechazo: undefined,
        costoUnitario: 5.5
      }
    ]);
  });
});
