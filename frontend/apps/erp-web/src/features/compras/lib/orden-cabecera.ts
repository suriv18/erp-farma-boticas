import { emptyToUndefined } from '../../../shared/lib/form-values';
import type { CrearOrdenPayload } from '../api/ordenes.types';
import type { Proveedor } from '../api/proveedores.types';
import { errorLinea, toLineasPayload, type LineaBorrador } from './orden-calculo';

export type CabeceraOrden = {
  proveedorId: string;
  establecimientoDestinoId: string;
  fechaEntregaEstimada: string;
  moneda: string;
  tipoCambio: string;
  condicionPago: string;
  diasCredito: string;
  observacion: string;
};

export type ErroresCabecera = Partial<Record<keyof CabeceraOrden, string>>;

export const CABECERA_VACIA: CabeceraOrden = {
  proveedorId: '',
  establecimientoDestinoId: '',
  fechaEntregaEstimada: '',
  moneda: 'PEN',
  tipoCambio: '',
  condicionPago: '',
  diasCredito: '0',
  observacion: ''
};

const PATRON_TIPO_CAMBIO = /^\d{1,10}(\.\d{1,6})?$/;

export const MONEDAS: readonly string[] = ['PEN', 'USD'];

const monedaSoportada = (moneda: string | null): string =>
  moneda !== null && MONEDAS.includes(moneda) ? moneda : 'PEN';

export const cabeceraDesdeProveedor = (
  cabecera: CabeceraOrden,
  proveedor: Proveedor
): CabeceraOrden => ({
  ...cabecera,
  proveedorId: proveedor.id,
  moneda: monedaSoportada(proveedor.monedaDefault),
  condicionPago: proveedor.condicionPagoDefault ?? '',
  diasCredito: String(proveedor.diasCreditoDefault)
});

export function fechaLocalISO(ahora: Date = new Date()): string {
  const mes = String(ahora.getMonth() + 1).padStart(2, '0');
  const dia = String(ahora.getDate()).padStart(2, '0');
  return `${ahora.getFullYear()}-${mes}-${dia}`;
}

const tipoCambioInvalido = (texto: string) =>
  texto !== '' && !(PATRON_TIPO_CAMBIO.test(texto) && Number(texto) > 0);

export function erroresCabecera(cabecera: CabeceraOrden, hoy: string): ErroresCabecera {
  const errores: ErroresCabecera = {};
  if (cabecera.proveedorId === '') errores.proveedorId = 'Selecciona un proveedor.';
  if (cabecera.establecimientoDestinoId === '') {
    errores.establecimientoDestinoId = 'Selecciona el establecimiento de destino.';
  }
  if (cabecera.fechaEntregaEstimada !== '' && cabecera.fechaEntregaEstimada < hoy) {
    errores.fechaEntregaEstimada = 'La fecha de entrega no puede ser anterior a hoy.';
  }
  const monedaValida = MONEDAS.includes(cabecera.moneda);
  if (!monedaValida) {
    errores.moneda = 'La moneda debe ser PEN o USD.';
  }
  const tipoCambio = cabecera.tipoCambio.trim();
  if (tipoCambioInvalido(tipoCambio)) {
    errores.tipoCambio = 'El tipo de cambio debe ser mayor que cero con hasta 6 decimales.';
  } else if (monedaValida && cabecera.moneda !== 'PEN' && tipoCambio === '') {
    errores.tipoCambio = 'El tipo de cambio es obligatorio cuando la moneda no es PEN.';
  }
  if (cabecera.condicionPago.length > 80) {
    errores.condicionPago = 'La condición de pago no debe exceder 80 caracteres.';
  }
  if (!/^\d{1,4}$/.test(cabecera.diasCredito)) {
    errores.diasCredito = 'Los días de crédito deben ser un entero mayor o igual a 0.';
  }
  if (cabecera.observacion.length > 1500) {
    errores.observacion = 'La observación no debe exceder 1500 caracteres.';
  }
  return errores;
}

export const puedeCrear = (
  cabecera: CabeceraOrden,
  lineas: LineaBorrador[],
  hoy: string
): boolean =>
  Object.keys(erroresCabecera(cabecera, hoy)).length === 0 &&
  lineas.length > 0 &&
  lineas.every((linea) => errorLinea(linea) === null);

const tipoCambioOpcional = (texto: string): number | undefined => {
  const limpio = emptyToUndefined(texto);
  return limpio === undefined ? undefined : Number(limpio);
};

export const toCrearOrdenPayload = (
  cabecera: CabeceraOrden,
  lineas: LineaBorrador[]
): CrearOrdenPayload => ({
  proveedorId: cabecera.proveedorId,
  establecimientoDestinoId: cabecera.establecimientoDestinoId,
  fechaEntregaEstimada: emptyToUndefined(cabecera.fechaEntregaEstimada),
  moneda: cabecera.moneda,
  tipoCambio: tipoCambioOpcional(cabecera.tipoCambio),
  condicionPago: emptyToUndefined(cabecera.condicionPago),
  diasCredito: Number(cabecera.diasCredito),
  observacion: emptyToUndefined(cabecera.observacion),
  lineas: toLineasPayload(lineas)
});
