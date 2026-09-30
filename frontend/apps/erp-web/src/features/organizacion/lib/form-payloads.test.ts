import {
  toActualizarAlmacenPayload,
  toActualizarEmpresaPayload,
  toActualizarEstablecimientoPayload,
  toActualizarTerminalPayload,
  toCrearAlmacenPayload,
  toCrearEmpresaPayload,
  toCrearEstablecimientoPayload,
  toCrearTerminalPayload
} from './form-payloads';

const empresaVacia = {
  ruc: '20123456789',
  razonSocial: '  Boticas SAC  ',
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

const establecimientoLleno = {
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA' as const,
  categoriaRegulatoriaCodigo: 'CAT',
  codigoAnexoSunat: '0001',
  codigoDigemid: 'DIG001',
  direccion: 'Av. 2',
  ubigeo: '150101',
  referencia: 'Frente al parque',
  latitud: '-12.0464',
  longitud: '-77.0428',
  telefono: '01444',
  email: 'e@b.pe',
  esPrincipal: true,
  permiteVentaOnline: true,
  permiteDelivery: true,
  perfilOperacion: 'STORE_EDGE' as const,
  zonaHoraria: 'America/Lima'
};

describe('form-payloads', () => {
  it('empresa: recorta, omite opcionales vacíos y no envía el RUC al actualizar', () => {
    const payload = toActualizarEmpresaPayload(empresaVacia);

    expect(payload).toEqual({
      razonSocial: 'Boticas SAC',
      nombreComercial: undefined,
      direccionFiscal: undefined,
      ubigeoFiscal: undefined,
      telefono: undefined,
      email: undefined,
      sitioWeb: undefined,
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: false
    });
    expect('ruc' in payload).toBe(false);
  });

  it('empresa: al crear agrega tenantId y RUC', () => {
    expect(toCrearEmpresaPayload('tenant-1', empresaVacia)).toMatchObject({
      tenantId: 'tenant-1',
      ruc: '20123456789',
      razonSocial: 'Boticas SAC'
    });
  });

  it('establecimiento: convierte coordenadas a número y conserva los opcionales llenos', () => {
    const payload = toActualizarEstablecimientoPayload(establecimientoLleno);

    expect(payload).toMatchObject({
      nombre: 'Botica Central',
      categoriaRegulatoriaCodigo: 'CAT',
      codigoDigemid: 'DIG001',
      latitud: -12.0464,
      longitud: -77.0428,
      perfilOperacion: 'STORE_EDGE'
    });
    expect('codigo' in payload).toBe(false);
  });

  it('establecimiento: omite coordenadas vacías y al crear agrega tenant, empresa y código', () => {
    const payload = toCrearEstablecimientoPayload('tenant-1', 'empresa-1', {
      ...establecimientoLleno,
      latitud: '',
      longitud: ''
    });

    expect(payload).toMatchObject({
      tenantId: 'tenant-1',
      empresaId: 'empresa-1',
      codigo: 'EST001'
    });
    expect(payload.latitud).toBeUndefined();
    expect(payload.longitud).toBeUndefined();
  });

  const almacenValores = {
    codigo: 'ALM001',
    nombre: 'Almacén Central',
    tipo: 'REFRIGERADO' as const,
    permiteLotes: true,
    permiteVencimiento: true,
    permiteVenta: false,
    permiteDespacho: true,
    controlTemperatura: true,
    temperaturaMinC: '2',
    temperaturaMaxC: '',
    activo: false
  };

  it('almacén: convierte temperaturas y al actualizar incluye activo sin código', () => {
    const payload = toActualizarAlmacenPayload(almacenValores);

    expect(payload).toMatchObject({ temperaturaMinC: 2, activo: false, tipo: 'REFRIGERADO' });
    expect(payload.temperaturaMaxC).toBeUndefined();
    expect('codigo' in payload).toBe(false);
  });

  it('almacén: al crear agrega tenant, establecimiento y código y no envía activo', () => {
    const payload = toCrearAlmacenPayload('tenant-1', 'est-1', almacenValores);

    expect(payload).toMatchObject({
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'ALM001'
    });
    expect('activo' in payload).toBe(false);
  });

  const terminalValores = {
    codigo: 'POS001',
    nombre: 'Caja 1',
    serieBoletaDefecto: 'B001',
    serieFacturaDefecto: '',
    numeroSerieEquipo: 'SN-1',
    hostname: '',
    ipEquipo: '10.0.0.15',
    impresoraCodigo: '',
    storeEdgeHabilitado: true,
    estado: 'MANTENIMIENTO' as const
  };

  it('terminal: omite opcionales vacíos y al actualizar incluye estado sin código', () => {
    const payload = toActualizarTerminalPayload(terminalValores);

    expect(payload).toMatchObject({
      nombre: 'Caja 1',
      serieBoletaDefecto: 'B001',
      ipEquipo: '10.0.0.15',
      estado: 'MANTENIMIENTO',
      storeEdgeHabilitado: true
    });
    expect(payload.serieFacturaDefecto).toBeUndefined();
    expect(payload.hostname).toBeUndefined();
    expect('codigo' in payload).toBe(false);
  });

  it('terminal: al crear agrega tenant, establecimiento y código y no envía estado', () => {
    const payload = toCrearTerminalPayload('tenant-1', 'est-1', terminalValores);

    expect(payload).toMatchObject({
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'POS001'
    });
    expect('estado' in payload).toBe(false);
  });
});
