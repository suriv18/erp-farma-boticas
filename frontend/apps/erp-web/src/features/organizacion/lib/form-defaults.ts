import type { Almacen } from '../api/almacenes.types';
import type { Empresa } from '../api/empresas.types';
import type { Establecimiento } from '../api/establecimientos.types';
import type { Terminal } from '../api/terminales.types';
import type { AlmacenFormValues } from '../schemas/almacen.schema';
import type { EmpresaFormValues } from '../schemas/empresa.schema';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import type { TerminalFormValues } from '../schemas/terminal.schema';
import { numberOrEmpty, orEmpty } from './form-values';

export const EMPRESA_FORM_VACIO: EmpresaFormValues = {
  ruc: '',
  razonSocial: '',
  nombreComercial: '',
  direccionFiscal: '',
  ubigeoFiscal: '',
  telefono: '',
  email: '',
  sitioWeb: '',
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false
};

export const ESTABLECIMIENTO_FORM_VACIO: EstablecimientoFormValues = {
  codigo: '',
  nombre: '',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: '',
  codigoAnexoSunat: '0000',
  codigoDigemid: '',
  direccion: '',
  ubigeo: '',
  referencia: '',
  latitud: '',
  longitud: '',
  telefono: '',
  email: '',
  esPrincipal: false,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima'
};

export const ALMACEN_FORM_VACIO: AlmacenFormValues = {
  codigo: '',
  nombre: '',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: false,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: '',
  temperaturaMaxC: '',
  activo: true
};

export const TERMINAL_FORM_VACIO: TerminalFormValues = {
  codigo: '',
  nombre: '',
  serieBoletaDefecto: '',
  serieFacturaDefecto: '',
  numeroSerieEquipo: '',
  hostname: '',
  ipEquipo: '',
  impresoraCodigo: '',
  storeEdgeHabilitado: false,
  estado: 'ACTIVO'
};

export function toEmpresaFormValues(empresa: Empresa): EmpresaFormValues {
  return {
    ruc: empresa.ruc,
    razonSocial: empresa.razonSocial,
    nombreComercial: orEmpty(empresa.nombreComercial),
    direccionFiscal: orEmpty(empresa.direccionFiscal),
    ubigeoFiscal: orEmpty(empresa.ubigeoFiscal),
    telefono: orEmpty(empresa.telefono),
    email: orEmpty(empresa.email),
    sitioWeb: orEmpty(empresa.sitioWeb),
    monedaFuncional: empresa.monedaFuncional,
    zonaHoraria: empresa.zonaHoraria,
    permiteVentaOnline: empresa.permiteVentaOnline
  };
}

export function toEstablecimientoFormValues(
  establecimiento: Establecimiento
): EstablecimientoFormValues {
  return {
    codigo: establecimiento.codigo,
    nombre: establecimiento.nombre,
    tipoEstablecimiento: establecimiento.tipoEstablecimiento,
    categoriaRegulatoriaCodigo: orEmpty(establecimiento.categoriaRegulatoriaCodigo),
    codigoAnexoSunat: establecimiento.codigoAnexoSunat,
    codigoDigemid: orEmpty(establecimiento.codigoDigemid),
    direccion: orEmpty(establecimiento.direccion),
    ubigeo: orEmpty(establecimiento.ubigeo),
    referencia: orEmpty(establecimiento.referencia),
    latitud: numberOrEmpty(establecimiento.latitud),
    longitud: numberOrEmpty(establecimiento.longitud),
    telefono: orEmpty(establecimiento.telefono),
    email: orEmpty(establecimiento.email),
    esPrincipal: establecimiento.esPrincipal,
    permiteVentaOnline: establecimiento.permiteVentaOnline,
    permiteDelivery: establecimiento.permiteDelivery,
    perfilOperacion: establecimiento.perfilOperacion,
    zonaHoraria: establecimiento.zonaHoraria
  };
}

export function toAlmacenFormValues(almacen: Almacen): AlmacenFormValues {
  return {
    codigo: almacen.codigo,
    nombre: almacen.nombre,
    tipo: almacen.tipo,
    permiteLotes: almacen.permiteLotes,
    permiteVencimiento: almacen.permiteVencimiento,
    permiteVenta: almacen.permiteVenta,
    permiteDespacho: almacen.permiteDespacho,
    controlTemperatura: almacen.controlTemperatura,
    temperaturaMinC: numberOrEmpty(almacen.temperaturaMinC),
    temperaturaMaxC: numberOrEmpty(almacen.temperaturaMaxC),
    activo: almacen.activo
  };
}

export function toTerminalFormValues(terminal: Terminal): TerminalFormValues {
  return {
    codigo: terminal.codigo,
    nombre: terminal.nombre,
    serieBoletaDefecto: orEmpty(terminal.serieBoletaDefecto),
    serieFacturaDefecto: orEmpty(terminal.serieFacturaDefecto),
    numeroSerieEquipo: orEmpty(terminal.numeroSerieEquipo),
    hostname: orEmpty(terminal.hostname),
    ipEquipo: orEmpty(terminal.ipEquipo),
    impresoraCodigo: orEmpty(terminal.impresoraCodigo),
    storeEdgeHabilitado: terminal.storeEdgeHabilitado,
    estado: terminal.estado
  };
}
