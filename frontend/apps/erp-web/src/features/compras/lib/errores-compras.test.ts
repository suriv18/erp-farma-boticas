import { ApiError } from '@boticas/api-client';
import { describeErrorCompras } from './errores-compras';

const error = (code: string | undefined, status = 409, detail = 'detalle del backend') =>
  new ApiError('falló', status, { ...(code === undefined ? {} : { code }), detail, status });

describe('describeErrorCompras', () => {
  it.each([
    ['COM_PROVEEDOR_DUPLICADO', 'Ya existe un proveedor con ese documento.'],
    ['COM_PROVEEDOR_NO_OPERABLE', 'El proveedor debe estar activo para emitir órdenes de compra.'],
    [
      'COM_ORDEN_ESTADO_INVALIDO',
      'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
    ],
    [
      'COM_RECEPCION_ORDEN_NO_RECEPCIONABLE',
      'La orden no está en un estado que permita recibir mercadería.'
    ],
    [
      'COM_RECEPCION_EXCEDE_PENDIENTE',
      'La cantidad recibida excede lo pendiente de la orden más su tolerancia.'
    ],
    ['COM_RECEPCION_LINEA_NO_ENCONTRADA', 'Una línea de la recepción no corresponde a la orden.'],
    [
      'COM_IDEMPOTENCY_CONFLICT',
      'Esta recepción ya se envió con datos distintos. Revisa el formulario y vuelve a registrar.'
    ],
    ['COM_ALMACEN_NO_OPERABLE', 'El almacén debe estar activo y controlar lotes.'],
    [
      'COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO',
      'El almacén no pertenece al establecimiento destino de la orden.'
    ],
    [
      'COM_MODIFICACION_CONCURRENTE',
      'Otro usuario modificó este registro. Actualiza la pantalla e inténtalo de nuevo.'
    ],
    ['COM_SKU_NO_OPERABLE', 'Uno de los productos no está activo comercialmente.'],
    ['COM_ESTABLECIMIENTO_NO_OPERABLE', 'El establecimiento destino debe estar activo.'],
    [
      'INV_LOTE_VENCIDO',
      'No se puede ingresar un lote vencido; recházalo por completo o corrige la fecha de vencimiento.'
    ],
    [
      'INV_LOTE_NO_ADMITE_INGRESO',
      'Ya existe un lote con ese número y vencimiento que no admite ingresos de stock.'
    ],
    ['INV_SKU_NO_OPERABLE', 'Uno de los productos no está activo comercialmente.']
  ])('traduce %s', (code, mensaje) => {
    expect(describeErrorCompras(error(code))).toBe(mensaje);
  });

  it('para otro código del backend muestra el detalle y el código', () => {
    expect(describeErrorCompras(error('COM_ORDEN_INVALIDA', 400))).toBe(
      'detalle del backend (COM_ORDEN_INVALIDA)'
    );
  });

  it('sin código usa el mensaje genérico de la API', () => {
    expect(describeErrorCompras(error(undefined, 500))).toBe('detalle del backend');
    expect(describeErrorCompras(error(undefined, 403))).toBe('No tienes permiso para esta acción.');
    expect(describeErrorCompras(new Error('x'))).toBe(
      'No se pudo completar la operación. Inténtalo de nuevo.'
    );
  });
});
