import type { ActualizarAlmacenPayload, CrearAlmacenPayload } from '../api/almacenes.types';
import type { ActualizarEmpresaPayload, CrearEmpresaPayload } from '../api/empresas.types';
import type {
  ActualizarEstablecimientoPayload,
  CrearEstablecimientoPayload
} from '../api/establecimientos.types';
import type { ActualizarTerminalPayload, CrearTerminalPayload } from '../api/terminales.types';
import type { AlmacenFormValues } from '../schemas/almacen.schema';
import type { EmpresaFormValues } from '../schemas/empresa.schema';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import type { TerminalFormValues } from '../schemas/terminal.schema';
import { emptyToUndefined, toNumberOrUndefined } from './form-values';

export function toActualizarEmpresaPayload(values: EmpresaFormValues): ActualizarEmpresaPayload {
  return {
    razonSocial: values.razonSocial.trim(),
    nombreComercial: emptyToUndefined(values.nombreComercial),
    direccionFiscal: emptyToUndefined(values.direccionFiscal),
    ubigeoFiscal: emptyToUndefined(values.ubigeoFiscal),
    telefono: emptyToUndefined(values.telefono),
    email: emptyToUndefined(values.email),
    sitioWeb: emptyToUndefined(values.sitioWeb),
    monedaFuncional: values.monedaFuncional,
    zonaHoraria: values.zonaHoraria,
    permiteVentaOnline: values.permiteVentaOnline
  };
}

export function toCrearEmpresaPayload(
  tenantId: string,
  values: EmpresaFormValues
): CrearEmpresaPayload {
  return { ...toActualizarEmpresaPayload(values), tenantId, ruc: values.ruc };
}

export function toActualizarEstablecimientoPayload(
  values: EstablecimientoFormValues
): ActualizarEstablecimientoPayload {
  return {
    nombre: values.nombre.trim(),
    tipoEstablecimiento: values.tipoEstablecimiento,
    categoriaRegulatoriaCodigo: emptyToUndefined(values.categoriaRegulatoriaCodigo),
    codigoAnexoSunat: values.codigoAnexoSunat,
    codigoDigemid: emptyToUndefined(values.codigoDigemid),
    direccion: emptyToUndefined(values.direccion),
    ubigeo: emptyToUndefined(values.ubigeo),
    referencia: emptyToUndefined(values.referencia),
    latitud: toNumberOrUndefined(values.latitud),
    longitud: toNumberOrUndefined(values.longitud),
    telefono: emptyToUndefined(values.telefono),
    email: emptyToUndefined(values.email),
    esPrincipal: values.esPrincipal,
    permiteVentaOnline: values.permiteVentaOnline,
    permiteDelivery: values.permiteDelivery,
    perfilOperacion: values.perfilOperacion,
    zonaHoraria: values.zonaHoraria
  };
}

export function toCrearEstablecimientoPayload(
  tenantId: string,
  empresaId: string,
  values: EstablecimientoFormValues
): CrearEstablecimientoPayload {
  return {
    ...toActualizarEstablecimientoPayload(values),
    tenantId,
    empresaId,
    codigo: values.codigo.trim()
  };
}

function almacenDatos(values: AlmacenFormValues) {
  return {
    nombre: values.nombre.trim(),
    tipo: values.tipo,
    permiteLotes: values.permiteLotes,
    permiteVencimiento: values.permiteVencimiento,
    permiteVenta: values.permiteVenta,
    permiteDespacho: values.permiteDespacho,
    controlTemperatura: values.controlTemperatura,
    temperaturaMinC: toNumberOrUndefined(values.temperaturaMinC),
    temperaturaMaxC: toNumberOrUndefined(values.temperaturaMaxC)
  };
}

export function toActualizarAlmacenPayload(values: AlmacenFormValues): ActualizarAlmacenPayload {
  return { ...almacenDatos(values), activo: values.activo };
}

export function toCrearAlmacenPayload(
  tenantId: string,
  establecimientoId: string,
  values: AlmacenFormValues
): CrearAlmacenPayload {
  return { ...almacenDatos(values), tenantId, establecimientoId, codigo: values.codigo.trim() };
}

function terminalDatos(values: TerminalFormValues) {
  return {
    nombre: values.nombre.trim(),
    serieBoletaDefecto: emptyToUndefined(values.serieBoletaDefecto),
    serieFacturaDefecto: emptyToUndefined(values.serieFacturaDefecto),
    numeroSerieEquipo: emptyToUndefined(values.numeroSerieEquipo),
    hostname: emptyToUndefined(values.hostname),
    ipEquipo: emptyToUndefined(values.ipEquipo),
    impresoraCodigo: emptyToUndefined(values.impresoraCodigo),
    storeEdgeHabilitado: values.storeEdgeHabilitado
  };
}

export function toActualizarTerminalPayload(values: TerminalFormValues): ActualizarTerminalPayload {
  return { ...terminalDatos(values), estado: values.estado };
}

export function toCrearTerminalPayload(
  tenantId: string,
  establecimientoId: string,
  values: TerminalFormValues
): CrearTerminalPayload {
  return { ...terminalDatos(values), tenantId, establecimientoId, codigo: values.codigo.trim() };
}
