import type { SkuResumen } from '../features/catalogo';
import type { Lote, Movimiento, Posicion } from '../features/inventario/api/inventario.types';
import type { CorporateStructure } from '../features/organizacion';

export const sampleEstructura: CorporateStructure = {
  asOf: '2026-10-02T10:00:00-05:00',
  companies: [
    {
      id: 'emp-1',
      legalName: 'Boticas SAC',
      tradeName: null,
      status: 'ACTIVE',
      establishments: [
        {
          id: 'est-1',
          code: 'EST001',
          name: 'Botica Central',
          status: 'ACTIVE',
          timeZone: 'America/Lima',
          warehouses: [
            { id: 'alm-1', code: 'ALM001', name: 'Almacén Central', status: 'ACTIVE' },
            { id: 'alm-2', code: 'ALM002', name: 'Almacén Frío', status: 'ACTIVE' }
          ],
          cashRegisters: []
        },
        {
          id: 'est-2',
          code: 'EST002',
          name: 'Botica Norte',
          status: 'ACTIVE',
          timeZone: 'America/Lima',
          warehouses: [{ id: 'alm-3', code: 'ALM003', name: 'Almacén Norte', status: 'ACTIVE' }],
          cashRegisters: []
        }
      ]
    }
  ]
};

export const sampleSku: SkuResumen = {
  id: 'sku-0001-aaaa',
  codigoInterno: 'MED-001',
  descripcionComercial: 'Paracetamol 500 mg',
  tipoSku: 'REGULADO',
  estado: 'ACTIVO'
};

export const samplePosicion: Posicion = {
  id: 'pos-1',
  establecimientoId: 'est-1',
  almacenId: 'alm-1',
  skuId: 'sku-0001-aaaa',
  loteId: 'lote-1',
  numeroLote: 'L001',
  fechaVencimiento: '2099-01-01',
  estadoLote: 'HABILITADO',
  estadoInventario: 'DISPONIBLE',
  cantidadFisica: 100,
  cantidadReservada: 10,
  cantidadDisponible: 90,
  vendible: true,
  version: 1,
  ultimoMovimientoAt: '2026-10-01T15:00:00Z'
};

export const sampleLote: Lote = {
  id: 'lote-1',
  skuId: 'sku-0001-aaaa',
  numeroLote: 'L001',
  fechaVencimiento: '2099-01-01',
  estado: 'HABILITADO',
  motivoEstado: null,
  bloqueadoAt: null,
  vendible: true
};

export const sampleMovimiento: Movimiento = {
  id: 'mov-1',
  posicionId: 'pos-1',
  loteId: 'lote-1',
  tipo: 'AJUSTE_INGRESO',
  naturaleza: 'E',
  cantidad: 5,
  stockAnterior: 100,
  stockPosterior: 105,
  fechaNegocio: '2026-10-02T10:00:00Z'
};
