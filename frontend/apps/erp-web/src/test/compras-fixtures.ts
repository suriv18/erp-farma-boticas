import type { Proveedor } from '../features/compras/api/proveedores.types';
import type { Orden, OrdenResumen } from '../features/compras/api/ordenes.types';

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

export const sampleOrden: Orden = {
  id: 'orden-1',
  numero: 'OC-2026-000001',
  proveedorId: 'prov-1',
  establecimientoDestinoId: 'est-1',
  fechaEmision: '2026-10-03',
  fechaEntregaEstimada: null,
  moneda: 'PEN',
  tipoCambio: null,
  condicionPago: 'CREDITO 30',
  diasCredito: 30,
  subtotal: 55,
  descuentoTotal: 0,
  impuestoTotal: 9.9,
  total: 64.9,
  estado: 'BORRADOR',
  observacion: 'Reposición',
  aprobadoAt: null,
  lineas: [
    {
      numeroLinea: 1,
      skuId: 'sku-0001-aaaa',
      descripcion: 'Paracetamol 500 mg',
      cantidad: 10,
      unidadMedidaCodigo: 'UND',
      precioUnitario: 5.5,
      descuento: 0,
      impuesto: 9.9,
      totalLinea: 64.9,
      toleranciaExcesoPct: 0,
      toleranciaDefectoPct: 0,
      cantidadRecibida: 0,
      cantidadPendiente: 10
    }
  ]
};

export const sampleOrdenResumen: OrdenResumen = {
  id: 'orden-1',
  numero: 'OC-2026-000001',
  proveedorId: 'prov-1',
  proveedorRazonSocial: 'Laboratorios Perú SAC',
  establecimientoDestinoId: 'est-1',
  fechaEmision: '2026-10-03',
  fechaEntregaEstimada: null,
  moneda: 'PEN',
  total: 64.9,
  estado: 'EMITIDA'
};
