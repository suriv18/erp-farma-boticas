import type { Almacen } from '../features/organizacion/api/almacenes.types';
import type { Empresa } from '../features/organizacion/api/empresas.types';
import type { Establecimiento } from '../features/organizacion/api/establecimientos.types';
import type { Terminal } from '../features/organizacion/api/terminales.types';

export const sampleEmpresa: Empresa = {
  id: 'empresa-1',
  tenantId: 'tenant-1',
  ruc: '20123456789',
  razonSocial: 'Boticas SAC',
  nombreComercial: null,
  direccionFiscal: null,
  ubigeoFiscal: null,
  telefono: null,
  email: null,
  sitioWeb: null,
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export const sampleEstablecimiento: Establecimiento = {
  id: 'est-1',
  tenantId: 'tenant-1',
  empresaId: 'empresa-1',
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: null,
  codigoAnexoSunat: '0001',
  codigoDigemid: null,
  direccion: null,
  ubigeo: null,
  referencia: null,
  latitud: null,
  longitud: null,
  telefono: null,
  email: null,
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima',
  estadoOperativo: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export const sampleAlmacen: Almacen = {
  id: 'alm-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'ALM001',
  nombre: 'Almacén Central',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: false,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: null,
  temperaturaMaxC: null,
  activo: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export const sampleTerminal: Terminal = {
  id: 'term-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'POS001',
  nombre: 'Caja 1',
  serieBoletaDefecto: null,
  serieFacturaDefecto: null,
  numeroSerieEquipo: null,
  hostname: null,
  ipEquipo: null,
  impresoraCodigo: null,
  storeEdgeHabilitado: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export function pagina<T>(
  items: T[],
  overrides: Partial<{ page: number; size: number; totalElements: number }> = {}
) {
  return { items, page: 0, size: 20, totalElements: items.length, ...overrides };
}
