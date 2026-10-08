import type { Almacen } from '../api/almacenes.types';
import type { Empresa } from '../api/empresas.types';
import type { Establecimiento } from '../api/establecimientos.types';
import type { Terminal } from '../api/terminales.types';
import {
  ALMACEN_FORM_VACIO,
  EMPRESA_FORM_VACIO,
  ESTABLECIMIENTO_FORM_VACIO,
  TERMINAL_FORM_VACIO,
  toAlmacenFormValues,
  toEmpresaFormValues,
  toEstablecimientoFormValues,
  toTerminalFormValues
} from './form-defaults';

describe('form-defaults', () => {
  it('los valores vacíos traen los valores por defecto del DDL', () => {
    expect(EMPRESA_FORM_VACIO).toMatchObject({
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      ruc: ''
    });
    expect(ESTABLECIMIENTO_FORM_VACIO).toMatchObject({
      codigoAnexoSunat: '0000',
      tipoEstablecimiento: 'BOTICA',
      perfilOperacion: 'ONLINE',
      zonaHoraria: 'America/Lima'
    });
    expect(ALMACEN_FORM_VACIO).toMatchObject({
      tipo: 'GENERAL',
      permiteLotes: true,
      permiteVencimiento: true,
      permiteVenta: false,
      permiteDespacho: true,
      controlTemperatura: false,
      activo: true
    });
    expect(TERMINAL_FORM_VACIO).toMatchObject({ estado: 'ACTIVO', storeEdgeHabilitado: false });
  });

  it('toEmpresaFormValues convierte nulls en cadenas vacías', () => {
    const empresa: Empresa = {
      id: 'e1',
      tenantId: 't1',
      ruc: '20123456786',
      razonSocial: 'Boticas SAC',
      nombreComercial: null,
      direccionFiscal: null,
      ubigeoFiscal: null,
      telefono: null,
      email: null,
      sitioWeb: null,
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: true,
      estado: 'ACTIVO',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: null
    };

    expect(toEmpresaFormValues(empresa)).toEqual({
      ruc: '20123456786',
      razonSocial: 'Boticas SAC',
      nombreComercial: '',
      direccionFiscal: '',
      ubigeoFiscal: '',
      telefono: '',
      email: '',
      sitioWeb: '',
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: true
    });
    expect(toEmpresaFormValues({ ...empresa, nombreComercial: 'Boticas' }).nombreComercial).toBe(
      'Boticas'
    );
  });

  it('toEstablecimientoFormValues convierte nulls y coordenadas numéricas', () => {
    const establecimiento: Establecimiento = {
      id: 's1',
      tenantId: 't1',
      empresaId: 'e1',
      codigo: 'EST001',
      nombre: 'Botica Central',
      tipoEstablecimiento: 'BOTICA',
      categoriaRegulatoriaCodigo: null,
      codigoAnexoSunat: '0001',
      codigoDigemid: null,
      direccion: null,
      ubigeo: null,
      referencia: null,
      latitud: -12.0464,
      longitud: null,
      telefono: null,
      email: null,
      esPrincipal: true,
      permiteVentaOnline: false,
      permiteDelivery: true,
      perfilOperacion: 'STORE_EDGE',
      zonaHoraria: 'America/Lima',
      estadoOperativo: 'ACTIVO',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: null
    };

    expect(toEstablecimientoFormValues(establecimiento)).toEqual({
      codigo: 'EST001',
      nombre: 'Botica Central',
      tipoEstablecimiento: 'BOTICA',
      categoriaRegulatoriaCodigo: '',
      codigoAnexoSunat: '0001',
      codigoDigemid: '',
      direccion: '',
      ubigeo: '',
      referencia: '',
      latitud: '-12.0464',
      longitud: '',
      telefono: '',
      email: '',
      esPrincipal: true,
      permiteVentaOnline: false,
      permiteDelivery: true,
      perfilOperacion: 'STORE_EDGE',
      zonaHoraria: 'America/Lima'
    });
  });

  it('toAlmacenFormValues convierte temperaturas y conserva el estado activo', () => {
    const almacen: Almacen = {
      id: 'a1',
      tenantId: 't1',
      establecimientoId: 's1',
      codigo: 'ALM001',
      nombre: 'Frío',
      tipo: 'REFRIGERADO',
      permiteLotes: true,
      permiteVencimiento: true,
      permiteVenta: false,
      permiteDespacho: true,
      controlTemperatura: true,
      temperaturaMinC: 2,
      temperaturaMaxC: 8,
      activo: false,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: null
    };

    expect(toAlmacenFormValues(almacen)).toEqual({
      codigo: 'ALM001',
      nombre: 'Frío',
      tipo: 'REFRIGERADO',
      permiteLotes: true,
      permiteVencimiento: true,
      permiteVenta: false,
      permiteDespacho: true,
      controlTemperatura: true,
      temperaturaMinC: '2',
      temperaturaMaxC: '8',
      activo: false
    });
  });

  it('toTerminalFormValues convierte nulls en cadenas vacías', () => {
    const terminal: Terminal = {
      id: 'p1',
      tenantId: 't1',
      establecimientoId: 's1',
      codigo: 'POS001',
      nombre: 'Caja 1',
      serieBoletaDefecto: 'B001',
      serieFacturaDefecto: null,
      numeroSerieEquipo: null,
      hostname: null,
      ipEquipo: '10.0.0.15',
      impresoraCodigo: null,
      storeEdgeHabilitado: true,
      estado: 'MANTENIMIENTO',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: null
    };

    expect(toTerminalFormValues(terminal)).toEqual({
      codigo: 'POS001',
      nombre: 'Caja 1',
      serieBoletaDefecto: 'B001',
      serieFacturaDefecto: '',
      numeroSerieEquipo: '',
      hostname: '',
      ipEquipo: '10.0.0.15',
      impresoraCodigo: '',
      storeEdgeHabilitado: true,
      estado: 'MANTENIMIENTO'
    });
  });
});
