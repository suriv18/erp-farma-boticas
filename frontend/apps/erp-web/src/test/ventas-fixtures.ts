import type { Turno } from '../features/caja/api/caja.types';
import type { Venta, VentaResumen } from '../features/ventas/api/ventas.types';

export const sampleTurno: Turno = {
  id: 'turno-1',
  terminalId: 'term-1',
  establecimientoId: 'est-1',
  cajeroId: 'user-1',
  aperturaAt: '2026-10-03T13:00:00Z',
  fondoInicial: 100,
  estado: 'ABIERTO',
  cierreAt: null,
  totalVentasSistema: null,
  totalSistema: null,
  totalDeclarado: null,
  diferencia: null,
  observacionCierre: null
};

export const sampleTurnoCerrado: Turno = {
  ...sampleTurno,
  estado: 'CERRADO',
  cierreAt: '2026-10-03T21:00:00Z',
  totalVentasSistema: 250,
  totalSistema: 350,
  totalDeclarado: 348.5,
  diferencia: -1.5,
  observacionCierre: 'Faltante de monedas'
};

export const sampleVenta: Venta = {
  id: 'venta-1',
  numeroOperacion: 'EST001-T01-000001',
  terminalId: 'term-1',
  turnoId: 'turno-1',
  establecimientoId: 'est-1',
  vendedorId: 'user-1',
  fechaVenta: '2026-10-03T15:30:00Z',
  moneda: 'PEN',
  subtotal: 21.19,
  descuentoTotal: 0,
  impuestoTotal: 3.81,
  total: 25,
  estado: 'CONFIRMADA',
  lineas: [
    {
      numeroLinea: 1,
      skuId: 'sku-0001-aaaa',
      descripcion: 'Paracetamol 500 mg',
      unidadVentaCodigo: 'UND',
      cantidad: 2,
      precioUnitario: 12.5,
      totalLinea: 25,
      lotes: [{ loteId: 'lote-1', cantidad: 2 }]
    }
  ],
  pago: { medioPago: 'EFECTIVO', monto: 25, montoRecibido: 30, vuelto: 5 },
  anulacion: null
};

export const sampleVentaAnulada: Venta = {
  ...sampleVenta,
  estado: 'ANULADA',
  anulacion: {
    anuladaAt: '2026-10-03T16:00:00Z',
    anuladaPorId: 'user-2',
    motivo: 'Error de digitación'
  }
};

export const sampleVentaResumen: VentaResumen = {
  id: 'venta-1',
  numeroOperacion: 'EST001-T01-000001',
  terminalId: 'term-1',
  fechaVenta: '2026-10-03T15:30:00Z',
  total: 25,
  estado: 'CONFIRMADA'
};
