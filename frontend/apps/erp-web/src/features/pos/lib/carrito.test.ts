import { sampleSkuFraccionable, sampleSkuVenta } from '../../../test/ventas-fixtures';
import {
  MAX_LINEAS,
  actualizarLinea,
  agregarSku,
  errorLinea,
  esMontoValido,
  lineaDesdeSku,
  puedeCobrar,
  quitarLinea,
  toRegistrarVentaPayload,
  totalCarrito,
  totalLinea,
  vueltoDe,
  type Carrito
} from './carrito';

const linea = lineaDesdeSku(sampleSkuVenta);

describe('lineaDesdeSku', () => {
  it('precarga cantidad 1 y el precio de referencia', () => {
    expect(linea).toEqual({
      skuId: 'sku-0001-aaaa',
      codigoInterno: 'MED-001',
      descripcion: 'Paracetamol 500 mg',
      unidadVentaCodigo: 'UND',
      permiteVentaFraccion: false,
      cantidad: '1',
      precio: '12.5'
    });
  });

  it('deja el precio vacío cuando el SKU no tiene precio de referencia y usa UND sin unidad', () => {
    const resultado = lineaDesdeSku({ ...sampleSkuFraccionable, unidadVentaCodigo: null });

    expect(resultado.precio).toBe('');
    expect(resultado.unidadVentaCodigo).toBe('UND');
    expect(resultado.permiteVentaFraccion).toBe(true);
  });
});

describe('agregarSku', () => {
  it('agrega una línea nueva', () => {
    expect(agregarSku([], sampleSkuVenta)).toEqual([linea]);
  });

  it('suma una unidad cuando el SKU ya está en el carrito', () => {
    expect(agregarSku([linea], sampleSkuVenta)).toEqual([{ ...linea, cantidad: '2' }]);
  });

  it('trata una cantidad inválida existente como cero al sumar', () => {
    expect(agregarSku([{ ...linea, cantidad: 'x' }], sampleSkuVenta)[0]?.cantidad).toBe('1');
  });

  it('no modifica otras líneas al repetir un SKU', () => {
    const otra = lineaDesdeSku(sampleSkuFraccionable);

    expect(agregarSku([otra, linea], sampleSkuVenta)).toEqual([otra, { ...linea, cantidad: '2' }]);
  });

  it('no agrega más líneas del máximo permitido', () => {
    const lleno: Carrito = Array.from({ length: MAX_LINEAS }, (_, indice) => ({
      ...linea,
      skuId: `sku-${indice}`
    }));

    expect(agregarSku(lleno, sampleSkuVenta)).toBe(lleno);
  });
});

describe('actualizarLinea y quitarLinea', () => {
  it('cambia solo la línea indicada', () => {
    const otra = lineaDesdeSku(sampleSkuFraccionable);

    expect(actualizarLinea([linea, otra], linea.skuId, { cantidad: '3', precio: '10' })).toEqual([
      { ...linea, cantidad: '3', precio: '10' },
      otra
    ]);
  });

  it('quita la línea indicada', () => {
    const otra = lineaDesdeSku(sampleSkuFraccionable);

    expect(quitarLinea([linea, otra], linea.skuId)).toEqual([otra]);
  });
});

describe('errorLinea', () => {
  it('no reporta error en una línea válida', () => {
    expect(errorLinea(linea)).toBeNull();
    expect(errorLinea({ ...linea, precio: '0' })).toBeNull();
  });

  it.each(['', '0', '-1', 'abc', '1.00001', '12345678901'])(
    'rechaza la cantidad "%s"',
    (cantidad) => {
      expect(errorLinea({ ...linea, cantidad })).toBe(
        'La cantidad debe ser mayor que cero con hasta 4 decimales.'
      );
    }
  );

  it('rechaza decimales en un producto que no se vende por fracción', () => {
    expect(errorLinea({ ...linea, cantidad: '1.5' })).toBe(
      'Este producto no se vende por fracción.'
    );
  });

  it('acepta decimales en un producto fraccionable', () => {
    expect(
      errorLinea({ ...lineaDesdeSku(sampleSkuFraccionable), cantidad: '1.5', precio: '2' })
    ).toBeNull();
  });

  it.each(['', '-1', 'abc', '1.00001'])('rechaza el precio "%s"', (precio) => {
    expect(errorLinea({ ...linea, precio })).toBe(
      'El precio debe ser mayor o igual a cero con hasta 4 decimales.'
    );
  });
});

describe('totales', () => {
  it('totalLinea multiplica y redondea a dos decimales', () => {
    expect(totalLinea({ ...linea, cantidad: '3', precio: '0.3333' })).toBe(1);
    expect(
      totalLinea({ ...lineaDesdeSku(sampleSkuFraccionable), cantidad: '1.5', precio: '2.5' })
    ).toBe(3.75);
  });

  it('totalLinea es cero cuando la línea es inválida', () => {
    expect(totalLinea({ ...linea, cantidad: '' })).toBe(0);
  });

  it('totalCarrito suma las líneas', () => {
    expect(
      totalCarrito([
        { ...linea, cantidad: '2' },
        { ...lineaDesdeSku(sampleSkuFraccionable), cantidad: '1.5', precio: '2' }
      ])
    ).toBe(28);
    expect(totalCarrito([])).toBe(0);
  });
});

describe('cobro', () => {
  it.each(['0', '30', '30.5', '30.50'])('acepta el monto %s', (texto) => {
    expect(esMontoValido(texto)).toBe(true);
  });

  it.each(['', '-1', '1.234', 'abc'])('rechaza el monto "%s"', (texto) => {
    expect(esMontoValido(texto)).toBe(false);
  });

  it('vueltoDe devuelve el vuelto o null si el monto no alcanza o no es válido', () => {
    expect(vueltoDe(25, '30')).toBe(5);
    expect(vueltoDe(25, '25')).toBe(0);
    expect(vueltoDe(25.1, '30')).toBe(4.9);
    expect(vueltoDe(25, '20')).toBeNull();
    expect(vueltoDe(25, 'abc')).toBeNull();
  });

  const base = { carrito: [linea], recibido: '20', hayTurno: true, hayAlmacen: true };

  it('puedeCobrar exige turno, almacén, carrito válido y monto suficiente', () => {
    expect(puedeCobrar(base)).toBe(true);
    expect(puedeCobrar({ ...base, hayTurno: false })).toBe(false);
    expect(puedeCobrar({ ...base, hayAlmacen: false })).toBe(false);
    expect(puedeCobrar({ ...base, carrito: [] })).toBe(false);
    expect(puedeCobrar({ ...base, carrito: [{ ...linea, cantidad: '' }] })).toBe(false);
    expect(puedeCobrar({ ...base, recibido: '5' })).toBe(false);
  });

  it('toRegistrarVentaPayload arma el cuerpo numérico para el backend', () => {
    expect(toRegistrarVentaPayload([{ ...linea, cantidad: '2' }], 'term-1', 'alm-1', '30')).toEqual(
      {
        terminalId: 'term-1',
        almacenId: 'alm-1',
        lineas: [{ skuId: 'sku-0001-aaaa', cantidad: 2, precioUnitario: 12.5 }],
        pago: { montoRecibido: 30 }
      }
    );
  });
});
