import { emptyToUndefined, numeroOpcional } from '../../../shared/lib/form-values';
import type { EstablishmentStructure, OrganizationalNode } from '../../organizacion';
import type { RegistrarRecepcionPayload } from '../api/recepciones.types';
import { errorItem, itemIncluido, toItemsPayload, type ItemBorrador } from './recepcion-items';

export type CabeceraRecepcion = {
  almacenId: string;
  documentoProveedorTipo: string;
  documentoProveedorSerie: string;
  documentoProveedorNumero: string;
  guiaRemisionRemitente: string;
  guiaRemisionTransportista: string;
  temperatura: string;
  humedad: string;
  observacion: string;
};

export type ErroresCabeceraRecepcion = Partial<Record<keyof CabeceraRecepcion, string>>;

export const TIPOS_DOCUMENTO_PROVEEDOR: readonly { codigo: string; nombre: string }[] = [
  { codigo: '01', nombre: 'Factura' },
  { codigo: '03', nombre: 'Boleta de venta' }
];

export const CABECERA_RECEPCION_VACIA: CabeceraRecepcion = {
  almacenId: '',
  documentoProveedorTipo: '01',
  documentoProveedorSerie: '',
  documentoProveedorNumero: '',
  guiaRemisionRemitente: '',
  guiaRemisionTransportista: '',
  temperatura: '',
  humedad: '',
  observacion: ''
};

const LONGITUDES: readonly [keyof CabeceraRecepcion, number, string][] = [
  ['documentoProveedorSerie', 20, 'La serie admite hasta 20 caracteres.'],
  ['documentoProveedorNumero', 40, 'El número admite hasta 40 caracteres.'],
  ['guiaRemisionRemitente', 80, 'La guía de remisión admite hasta 80 caracteres.'],
  ['guiaRemisionTransportista', 80, 'La guía de remisión admite hasta 80 caracteres.'],
  ['observacion', 1000, 'La observación admite hasta 1000 caracteres.']
];

const PATRON_DOS_DECIMALES = /^-?\d{1,3}(\.\d{1,2})?$/;

const fueraDeRango = (texto: string, minimo: number, maximo: number): boolean => {
  const limpio = texto.trim();
  return (
    limpio !== '' &&
    !(PATRON_DOS_DECIMALES.test(limpio) && Number(limpio) >= minimo && Number(limpio) <= maximo)
  );
};

export const almacenesDeDestino = (
  establecimientos: EstablishmentStructure[],
  establecimientoId: string
): OrganizationalNode[] =>
  (establecimientos.find(({ id }) => id === establecimientoId)?.warehouses ?? []).filter(
    ({ status }) => status === 'ACTIVE'
  );

export function erroresCabeceraRecepcion(cabecera: CabeceraRecepcion): ErroresCabeceraRecepcion {
  const errores: ErroresCabeceraRecepcion = {};
  if (cabecera.almacenId === '') errores.almacenId = 'Selecciona el almacén de recepción.';
  LONGITUDES.forEach(([campo, maximo, mensaje]) => {
    if (cabecera[campo].trim().length > maximo) errores[campo] = mensaje;
  });
  if (fueraDeRango(cabecera.temperatura, -50, 100)) {
    errores.temperatura = 'La temperatura debe estar entre -50 y 100 °C con hasta 2 decimales.';
  }
  if (fueraDeRango(cabecera.humedad, 0, 100)) {
    errores.humedad = 'La humedad relativa debe estar entre 0 y 100 % con hasta 2 decimales.';
  }
  return errores;
}

export function puedeRegistrar(
  cabecera: CabeceraRecepcion,
  items: ItemBorrador[],
  hoy: string
): boolean {
  const incluidos = items.filter(itemIncluido);
  return (
    Object.keys(erroresCabeceraRecepcion(cabecera)).length === 0 &&
    incluidos.length > 0 &&
    incluidos.every((item) => errorItem(item, hoy) === null)
  );
}

export const toRegistrarRecepcionPayload = (
  ordenCompraId: string,
  cabecera: CabeceraRecepcion,
  items: ItemBorrador[]
): RegistrarRecepcionPayload => ({
  ordenCompraId,
  almacenId: cabecera.almacenId,
  documentoProveedorTipo: cabecera.documentoProveedorTipo,
  documentoProveedorSerie: emptyToUndefined(cabecera.documentoProveedorSerie),
  documentoProveedorNumero: emptyToUndefined(cabecera.documentoProveedorNumero),
  guiaRemisionRemitente: emptyToUndefined(cabecera.guiaRemisionRemitente),
  guiaRemisionTransportista: emptyToUndefined(cabecera.guiaRemisionTransportista),
  temperaturaRecepcionC: numeroOpcional(cabecera.temperatura),
  humedadRelativaPct: numeroOpcional(cabecera.humedad),
  observacion: emptyToUndefined(cabecera.observacion),
  items: toItemsPayload(items)
});
