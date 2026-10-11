import { sampleProveedor } from '../../../test/compras-fixtures';
import { sampleSkuVenta } from '../../../test/ventas-fixtures';
import { lineaDesdeSku } from './orden-calculo';
import {
  CABECERA_VACIA,
  cabeceraDesdeProveedor,
  erroresCabecera,
  fechaLocalISO,
  puedeCrear,
  toCrearOrdenPayload
} from './orden-cabecera';

const HOY = '2026-10-06';
const completa = {
  ...CABECERA_VACIA,
  proveedorId: 'prov-1',
  establecimientoDestinoId: 'est-1',
  moneda: 'PEN',
  diasCredito: '0'
};
const lineaValida = { ...lineaDesdeSku(sampleSkuVenta, true), precio: '5.5', impuesto: '0.99' };

describe('cabecera de la orden', () => {
  it('la cabecera vacía arranca en soles sin proveedor ni destino', () => {
    expect(CABECERA_VACIA).toEqual({
      proveedorId: '',
      establecimientoDestinoId: '',
      fechaEntregaEstimada: '',
      moneda: 'PEN',
      tipoCambio: '',
      condicionPago: '',
      diasCredito: '0',
      observacion: ''
    });
  });

  it('al elegir proveedor precarga moneda, condición de pago y días de crédito', () => {
    expect(cabeceraDesdeProveedor(CABECERA_VACIA, sampleProveedor)).toEqual({
      ...CABECERA_VACIA,
      proveedorId: 'prov-1',
      moneda: 'PEN',
      condicionPago: 'CREDITO 30',
      diasCredito: '30'
    });
  });

  it('usa PEN y condición vacía cuando el proveedor no trae valores por defecto', () => {
    expect(
      cabeceraDesdeProveedor(CABECERA_VACIA, {
        ...sampleProveedor,
        monedaDefault: null,
        condicionPagoDefault: null
      })
    ).toMatchObject({ moneda: 'PEN', condicionPago: '' });
  });

  it('conserva la moneda del proveedor si es USD y cae a PEN si no es soportada', () => {
    expect(
      cabeceraDesdeProveedor(CABECERA_VACIA, { ...sampleProveedor, monedaDefault: 'USD' }).moneda
    ).toBe('USD');
    expect(
      cabeceraDesdeProveedor(CABECERA_VACIA, { ...sampleProveedor, monedaDefault: 'EUR' }).moneda
    ).toBe('PEN');
  });

  it('fechaLocalISO usa la fecha local con ceros a la izquierda', () => {
    expect(fechaLocalISO(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(fechaLocalISO(new Date(2026, 9, 6))).toBe('2026-10-06');
  });

  it('fechaLocalISO usa la fecha de hoy por defecto', () => {
    expect(fechaLocalISO()).toBe(fechaLocalISO(new Date()));
  });
});

describe('erroresCabecera', () => {
  it('no reporta errores en una cabecera completa', () => {
    expect(erroresCabecera(completa, HOY)).toEqual({});
  });

  it('exige proveedor y destino', () => {
    expect(erroresCabecera(CABECERA_VACIA, HOY)).toEqual({
      proveedorId: 'Selecciona un proveedor.',
      establecimientoDestinoId: 'Selecciona el establecimiento de destino.'
    });
  });

  it('rechaza una fecha de entrega anterior a hoy y acepta hoy y el futuro', () => {
    expect(erroresCabecera({ ...completa, fechaEntregaEstimada: '2026-10-05' }, HOY)).toEqual({
      fechaEntregaEstimada: 'La fecha de entrega no puede ser anterior a hoy.'
    });
    expect(erroresCabecera({ ...completa, fechaEntregaEstimada: HOY }, HOY)).toEqual({});
    expect(erroresCabecera({ ...completa, fechaEntregaEstimada: '2026-10-20' }, HOY)).toEqual({});
  });

  it('valida la moneda, el tipo de cambio, la condición, los días y la observación', () => {
    expect(erroresCabecera({ ...completa, moneda: 'EUR' }, HOY)).toEqual({
      moneda: 'La moneda debe ser PEN o USD.'
    });
    expect(erroresCabecera({ ...completa, tipoCambio: '0' }, HOY)).toEqual({
      tipoCambio: 'El tipo de cambio debe ser mayor que cero con hasta 6 decimales.'
    });
    expect(erroresCabecera({ ...completa, tipoCambio: '3.8123456' }, HOY)).toHaveProperty(
      'tipoCambio'
    );
    expect(erroresCabecera({ ...completa, tipoCambio: '3.81' }, HOY)).toEqual({});
    expect(erroresCabecera({ ...completa, condicionPago: 'a'.repeat(81) }, HOY)).toEqual({
      condicionPago: 'La condición de pago no debe exceder 80 caracteres.'
    });
    expect(erroresCabecera({ ...completa, diasCredito: '-1' }, HOY)).toEqual({
      diasCredito: 'Los días de crédito deben ser un entero mayor o igual a 0.'
    });
    expect(erroresCabecera({ ...completa, observacion: 'a'.repeat(1501) }, HOY)).toEqual({
      observacion: 'La observación no debe exceder 1500 caracteres.'
    });
  });
});

describe('tipo de cambio según la moneda', () => {
  it('lo exige cuando la moneda es USD', () => {
    expect(erroresCabecera({ ...completa, moneda: 'USD' }, HOY)).toEqual({
      tipoCambio: 'El tipo de cambio es obligatorio cuando la moneda no es PEN.'
    });
    expect(erroresCabecera({ ...completa, moneda: 'USD', tipoCambio: '  ' }, HOY)).toHaveProperty(
      'tipoCambio'
    );
  });

  it('no lo exige en soles ni cuando ya se escribió', () => {
    expect(erroresCabecera({ ...completa, moneda: 'PEN', tipoCambio: '' }, HOY)).toEqual({});
    expect(erroresCabecera({ ...completa, moneda: 'USD', tipoCambio: '3.81' }, HOY)).toEqual({});
  });

  it('con una moneda inválida solo reporta la moneda', () => {
    expect(erroresCabecera({ ...completa, moneda: 'EUR' }, HOY)).toEqual({
      moneda: 'La moneda debe ser PEN o USD.'
    });
  });
});

describe('puedeCrear', () => {
  it('exige cabecera válida y al menos una línea válida', () => {
    expect(puedeCrear(completa, [lineaValida], HOY)).toBe(true);
    expect(puedeCrear(CABECERA_VACIA, [lineaValida], HOY)).toBe(false);
    expect(puedeCrear(completa, [], HOY)).toBe(false);
    expect(puedeCrear(completa, [{ ...lineaValida, precio: '' }], HOY)).toBe(false);
  });
});

describe('toCrearOrdenPayload', () => {
  it('omite los opcionales vacíos y convierte los números', () => {
    expect(toCrearOrdenPayload(completa, [lineaValida])).toEqual({
      proveedorId: 'prov-1',
      establecimientoDestinoId: 'est-1',
      fechaEntregaEstimada: undefined,
      moneda: 'PEN',
      tipoCambio: undefined,
      condicionPago: undefined,
      diasCredito: 0,
      observacion: undefined,
      lineas: [
        {
          skuId: 'sku-0001-aaaa',
          cantidad: 1,
          unidadMedidaCodigo: 'UND',
          precioUnitario: 5.5,
          descuento: 0,
          impuesto: 0.99,
          toleranciaExcesoPct: 0,
          toleranciaDefectoPct: 0
        }
      ]
    });
  });

  it('incluye fecha, tipo de cambio, condición y observación cuando se llenaron', () => {
    const payload = toCrearOrdenPayload(
      {
        ...completa,
        fechaEntregaEstimada: '2026-10-20',
        tipoCambio: ' 3.81 ',
        condicionPago: ' CREDITO 30 ',
        diasCredito: '30',
        observacion: ' Reposición '
      },
      [lineaValida]
    );

    expect(payload).toMatchObject({
      fechaEntregaEstimada: '2026-10-20',
      tipoCambio: 3.81,
      condicionPago: 'CREDITO 30',
      diasCredito: 30,
      observacion: 'Reposición'
    });
  });
});
