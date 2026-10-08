import type { Proveedor } from '../features/compras/api/proveedores.types';

export const sampleProveedor: Proveedor = {
  id: 'prov-1',
  tipoDocumento: '6',
  numeroDocumento: '20100070970',
  razonSocial: 'Laboratorios Perú SAC',
  nombreComercial: 'LabPerú',
  direccion: null,
  ubigeo: null,
  telefono: null,
  email: null,
  contactoNombre: null,
  contactoTelefono: null,
  contactoEmail: null,
  condicionPagoDefault: 'CREDITO 30',
  diasCreditoDefault: 30,
  monedaDefault: 'PEN',
  esLaboratorio: true,
  esImportador: false,
  esDistribuidor: true,
  calificacion: 'CONFIABLE',
  estado: 'ACTIVO'
};
