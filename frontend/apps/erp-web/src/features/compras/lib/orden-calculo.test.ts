import { sampleSkuFraccionable, sampleSkuVenta } from '../../../test/ventas-fixtures';
import {
  MAX_LINEAS_ORDEN,
  actualizarLinea,
  agregarSku,
  errorLinea,
  importeBruto,
  lineaDesdeSku,
  quitarLinea,
  restablecerImpuesto,
  toLineasPayload,
  totalLinea,
  totalesOrden,
  type LineaBorrador
} from './orden-calculo';

const base = lineaDesdeSku(sampleSkuVenta, true);
const conPrecio = (cambios: Partial<LineaBorrador> = {}): LineaBorrador => ({
  ...base,
  precio: '5.5',
  impuesto: '0.99',
  ...cambios
});

describe('lineaDesdeSku', () => {
  it('arranca con cantidad 1, precio vacío y sin impuesto', () => {
    expect(base).toEqual({
      skuId: 'sku-0001-aaaa',
      codigoInterno: 'MED-001',
      descripcion: 'Paracetamol 500 mg',
      unidadMedidaCodigo: 'UND',
      afectoIgv: true,
      cantidad: '1',
      precio: '',
      descuento: '0',
      impuesto: '0',
      impuestoManual: false,
      toleranciaExceso: '0',
      toleranciaDefecto: '0'
    });
  });

  it('usa UND cuando el SKU no tiene unidad de venta', () => {
    expect(
      lineaDesdeSku({ ...sampleSkuFraccionable, unidadVentaCodigo: null }, false)
    ).toMatchObject({
      unidadMedidaCodigo: 'UND',
      afectoIgv: false
    });
  });
});

describe('agregarSku, actualizarLinea y quitarLinea', () => {
  it('agrega un SKU nuevo', () => {
    expect(agregarSku([], sampleSkuVenta, true)).toEqual([base]);
  });

  it('ignora un SKU que ya está en la orden', () => {
    const lineas = [base];

    expect(agregarSku(lineas, sampleSkuVenta, true)).toBe(lineas);
  });

  it('no agrega más líneas del máximo permitido', () => {
    const lleno = Array.from({ length: MAX_LINEAS_ORDEN }, (_, indice) => ({
      ...base,
      skuId: `sku-${indice}`
    }));

    expect(agregarSku(lleno, sampleSkuVenta, true)).toBe(lleno);
  });

  it('recalcula el impuesto sugerido al cambiar precio, cantidad o descuento mientras no se edite a mano', () => {
    const conPrecioEscrito = actualizarLinea([base], base.skuId, { precio: '5.5' });
    expect(conPrecioEscrito[0]?.impuesto).toBe('0.99');

    const conCantidad = actualizarLinea(conPrecioEscrito, base.skuId, { cantidad: '2' });
    expect(conCantidad[0]?.impuesto).toBe('1.98');

    const conDescuento = actualizarLinea(conCantidad, base.skuId, { descuento: '1' });
    expect(conDescuento[0]?.impuesto).toBe('1.8');
  });

  it('no sugiere impuesto en productos no afectos', () => {
    const noAfecta = lineaDesdeSku(sampleSkuVenta, false);

    expect(actualizarLinea([noAfecta], noAfecta.skuId, { precio: '5.5' })[0]?.impuesto).toBe('0');
  });

  it('con un descuento inválido sugiere sobre el importe sin descuento', () => {
    const lineas = actualizarLinea([base], base.skuId, { precio: '10', descuento: 'abc' });

    expect(lineas[0]?.impuesto).toBe('1.8');
  });

  it('al editar el impuesto a mano deja de recalcularlo', () => {
    const manual = actualizarLinea([conPrecio()], base.skuId, { impuesto: '2' });
    expect(manual[0]).toMatchObject({ impuesto: '2', impuestoManual: true });

    const despues = actualizarLinea(manual, base.skuId, { cantidad: '3' });
    expect(despues[0]?.impuesto).toBe('2');
  });

  it('restablecer el impuesto vuelve al sugerido y lo recalcula de nuevo', () => {
    const manual = actualizarLinea([conPrecio()], base.skuId, { impuesto: '2' });

    const restablecida = restablecerImpuesto(manual, base.skuId);
    expect(restablecida[0]).toMatchObject({ impuesto: '0.99', impuestoManual: false });

    const despues = actualizarLinea(restablecida, base.skuId, { cantidad: '2' });
    expect(despues[0]?.impuesto).toBe('1.98');
  });

  it('restablecer el impuesto no toca las otras líneas', () => {
    const otra = { ...conPrecio({ skuId: 'sku-0002-bbbb' }), impuestoManual: true, impuesto: '7' };

    const resultado = restablecerImpuesto([conPrecio(), otra], 'sku-0001-aaaa');

    expect(resultado[1]).toBe(otra);
  });

  it('cambia solo la línea indicada', () => {
    const otra = { ...base, skuId: 'sku-0002-bbbb', codigoInterno: 'JAR-002' };

    const resultado = actualizarLinea([base, otra], base.skuId, { cantidad: '5' });

    expect(resultado[0]?.cantidad).toBe('5');
    expect(resultado[1]).toBe(otra);
  });

  it('quita la línea indicada', () => {
    const otra = { ...base, skuId: 'sku-0002-bbbb' };

    expect(quitarLinea([base, otra], base.skuId)).toEqual([otra]);
  });
});

describe('errorLinea', () => {
  it('no reporta error en una línea válida', () => {
    expect(errorLinea(conPrecio())).toBeNull();
    expect(errorLinea(conPrecio({ precio: '0', impuesto: '0' }))).toBeNull();
  });

  it.each(['', '0', '-1', 'abc', '1.00001', '1234567890'])(
    'rechaza la cantidad "%s"',
    (cantidad) => {
      expect(errorLinea(conPrecio({ cantidad }))).toBe(
        'La cantidad debe ser mayor que cero con hasta 4 decimales.'
      );
    }
  );

  it.each(['', '-1', 'abc', '1.0000001'])('rechaza el precio "%s"', (precio) => {
    expect(errorLinea(conPrecio({ precio }))).toBe(
      'El precio debe ser mayor o igual a cero con hasta 6 decimales.'
    );
  });

  it.each(['', '-1', '1.234'])('rechaza el descuento "%s"', (descuento) => {
    expect(errorLinea(conPrecio({ descuento }))).toBe(
      'El descuento debe ser mayor o igual a cero con hasta 2 decimales.'
    );
  });

  it.each(['', '-1', '1.234'])('rechaza el impuesto "%s"', (impuesto) => {
    expect(errorLinea(conPrecio({ impuesto }))).toBe(
      'El impuesto debe ser mayor o igual a cero con hasta 2 decimales.'
    );
  });

  it.each(['', '101', '-1', '1.00001', 'x'])('rechaza la tolerancia de exceso "%s"', (valor) => {
    expect(errorLinea(conPrecio({ toleranciaExceso: valor }))).toBe(
      'Las tolerancias deben estar entre 0 y 100 con hasta 4 decimales.'
    );
  });

  it('rechaza la tolerancia de defecto fuera de rango', () => {
    expect(errorLinea(conPrecio({ toleranciaDefecto: '100.5' }))).toBe(
      'Las tolerancias deben estar entre 0 y 100 con hasta 4 decimales.'
    );
    expect(errorLinea(conPrecio({ toleranciaDefecto: '100' }))).toBeNull();
  });

  it('rechaza un descuento que deja el total de la línea en negativo', () => {
    expect(errorLinea(conPrecio({ descuento: '10', impuesto: '0' }))).toBe(
      'El descuento no puede superar el importe de la línea más el impuesto.'
    );
  });
});

describe('totales', () => {
  it('importeBruto multiplica y redondea a dos decimales y es cero si la línea es inválida', () => {
    expect(importeBruto(conPrecio({ cantidad: '3', precio: '0.3333' }))).toBe(1);
    expect(importeBruto(conPrecio({ cantidad: '1', precio: '4.145' }))).toBe(4.15);
    expect(importeBruto(conPrecio({ cantidad: '' }))).toBe(0);
    expect(importeBruto(conPrecio({ precio: '' }))).toBe(0);
  });

  it('totalLinea suma bruto menos descuento más impuesto y es cero si hay error', () => {
    expect(totalLinea(conPrecio({ cantidad: '2', descuento: '1', impuesto: '1.8' }))).toBe(11.8);
    expect(totalLinea(conPrecio({ cantidad: '' }))).toBe(0);
  });

  it('totalesOrden suma solo las líneas válidas', () => {
    const segunda = conPrecio({
      skuId: 'sku-0002-bbbb',
      cantidad: '2',
      precio: '10',
      descuento: '1',
      impuesto: '3.42'
    });
    const invalida = conPrecio({ skuId: 'sku-0003-cccc', cantidad: '' });

    expect(totalesOrden([conPrecio(), segunda, invalida])).toEqual({
      subtotal: 25.5,
      descuento: 1,
      impuesto: 4.41,
      total: 28.91
    });
    expect(totalesOrden([])).toEqual({ subtotal: 0, descuento: 0, impuesto: 0, total: 0 });
  });

  it('toLineasPayload arma el cuerpo numérico para el backend', () => {
    expect(
      toLineasPayload([
        conPrecio({ cantidad: '2', toleranciaExceso: '5', toleranciaDefecto: '2.5' })
      ])
    ).toEqual([
      {
        skuId: 'sku-0001-aaaa',
        cantidad: 2,
        unidadMedidaCodigo: 'UND',
        precioUnitario: 5.5,
        descuento: 0,
        impuesto: 0.99,
        toleranciaExcesoPct: 5,
        toleranciaDefectoPct: 2.5
      }
    ]);
  });
});
