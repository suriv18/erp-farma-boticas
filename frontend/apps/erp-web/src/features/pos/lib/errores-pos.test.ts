import { ApiError } from '@boticas/api-client';
import { describeErrorPos } from './errores-pos';

const error = (code: string | undefined, status = 409, detail = 'detalle del backend') =>
  new ApiError('falló', status, { ...(code === undefined ? {} : { code }), detail, status });

describe('describeErrorPos', () => {
  it.each([
    [
      'INV_STOCK_INSUFICIENTE',
      'No hay stock suficiente para una de las líneas. Ajusta la cantidad.'
    ],
    ['VEN_TURNO_NO_ABIERTO', 'La terminal no tiene un turno abierto. Abre el turno en Caja.'],
    [
      'VEN_IDEMPOTENCY_CONFLICT',
      'Esta venta ya se envió con datos distintos. Revisa el carrito y vuelve a cobrar.'
    ],
    ['VEN_MONTO_RECIBIDO_INSUFICIENTE', 'El monto recibido no cubre el total de la venta.'],
    ['VEN_TERMINAL_NO_OPERABLE', 'La terminal no está activa para operar.'],
    ['VEN_ALMACEN_NO_OPERABLE', 'El almacén no está habilitado para vender.']
  ])('traduce %s', (code, mensaje) => {
    expect(describeErrorPos(error(code))).toBe(mensaje);
  });

  it('para otro código del backend muestra el detalle y el código', () => {
    expect(describeErrorPos(error('VEN_SKU_NO_OPERABLE'))).toBe(
      'detalle del backend (VEN_SKU_NO_OPERABLE)'
    );
  });

  it('sin código usa el mensaje genérico de la API', () => {
    expect(describeErrorPos(error(undefined, 500))).toBe('detalle del backend');
    expect(describeErrorPos(new Error('x'))).toBe(
      'No se pudo completar la operación. Inténtalo de nuevo.'
    );
  });
});
