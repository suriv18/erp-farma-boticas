import { samplePosicion } from '../../../test/inventario-fixtures';
import {
  AJUSTE_FORM_VACIO,
  ETIQUETAS_TIPO_AJUSTE,
  ajusteDesdePosicion,
  toRegistrarMovimientoPayload
} from './ajuste';

describe('ajuste', () => {
  it('etiqueta en español cada tipo de ajuste', () => {
    expect(ETIQUETAS_TIPO_AJUSTE).toEqual({ AJUSTE_INGRESO: 'Ingreso', AJUSTE_SALIDA: 'Salida' });
  });

  it('el formulario vacío es un ingreso sin datos', () => {
    expect(AJUSTE_FORM_VACIO).toEqual({
      almacenId: '',
      skuId: '',
      tipo: 'AJUSTE_INGRESO',
      loteId: '',
      numeroLote: '',
      fechaVencimiento: '',
      cantidad: '',
      motivo: ''
    });
  });

  it('ajusteDesdePosicion precarga almacén, SKU y lote', () => {
    expect(ajusteDesdePosicion(samplePosicion)).toEqual({
      ...AJUSTE_FORM_VACIO,
      almacenId: 'alm-1',
      skuId: 'sku-0001-aaaa',
      loteId: 'lote-1'
    });
  });

  it('el payload sobre un lote existente envía loteId y omite los datos del lote nuevo', () => {
    expect(
      toRegistrarMovimientoPayload({
        ...ajusteDesdePosicion(samplePosicion),
        tipo: 'AJUSTE_SALIDA',
        cantidad: '2.5',
        motivo: 'Merma'
      })
    ).toEqual({
      almacenId: 'alm-1',
      skuId: 'sku-0001-aaaa',
      tipo: 'AJUSTE_SALIDA',
      cantidad: 2.5,
      motivo: 'Merma',
      loteId: 'lote-1'
    });
  });

  it('el payload con lote nuevo envía número y vencimiento sin loteId', () => {
    expect(
      toRegistrarMovimientoPayload({
        ...AJUSTE_FORM_VACIO,
        almacenId: 'alm-1',
        skuId: 'sku-1',
        numeroLote: ' L-NEW ',
        fechaVencimiento: '2030-01-01',
        cantidad: '10',
        motivo: 'Ingreso inicial'
      })
    ).toEqual({
      almacenId: 'alm-1',
      skuId: 'sku-1',
      tipo: 'AJUSTE_INGRESO',
      cantidad: 10,
      motivo: 'Ingreso inicial',
      numeroLote: 'L-NEW',
      fechaVencimiento: '2030-01-01'
    });
  });
});
