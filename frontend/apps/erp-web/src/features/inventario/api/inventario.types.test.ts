import { ESTADOS_LOTE, TIPOS_AJUSTE } from './inventario.types';

describe('inventario.types', () => {
  it('expone los estados de lote del backend', () => {
    expect(ESTADOS_LOTE).toEqual([
      'HABILITADO',
      'CUARENTENA',
      'BLOQUEADO',
      'INMOVILIZADO_RECALL',
      'VENCIDO',
      'BAJA_DESTRUIDO'
    ]);
  });

  it('expone los tipos de ajuste', () => {
    expect(TIPOS_AJUSTE).toEqual(['AJUSTE_INGRESO', 'AJUSTE_SALIDA']);
  });
});
