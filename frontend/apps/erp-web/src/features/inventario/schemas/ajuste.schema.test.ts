import { AJUSTE_FORM_VACIO } from '../lib/ajuste';
import { ajusteSchema, type AjusteFormValues } from './ajuste.schema';

const valido: AjusteFormValues = {
  ...AJUSTE_FORM_VACIO,
  almacenId: 'alm-1',
  skuId: 'sku-1',
  loteId: 'lote-1',
  cantidad: '5',
  motivo: 'Conteo cíclico'
};

const mensajes = (values: AjusteFormValues) => {
  const result = ajusteSchema.safeParse(values);
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
};

describe('ajusteSchema', () => {
  it('acepta un ingreso sobre un lote existente', () => {
    expect(ajusteSchema.safeParse(valido).success).toBe(true);
  });

  it('acepta un ingreso con lote nuevo y vencimiento', () => {
    expect(
      ajusteSchema.safeParse({
        ...valido,
        loteId: '',
        numeroLote: 'L-NEW',
        fechaVencimiento: '2030-01-01'
      }).success
    ).toBe(true);
  });

  it('acepta una salida sobre un lote existente', () => {
    expect(ajusteSchema.safeParse({ ...valido, tipo: 'AJUSTE_SALIDA' }).success).toBe(true);
  });

  it('exige almacén y SKU', () => {
    expect(mensajes({ ...valido, almacenId: '', skuId: '' })).toEqual([
      'Selecciona un almacén.',
      'Selecciona un SKU.'
    ]);
  });

  it.each(['', '0', '-3', 'abc'])('rechaza la cantidad %j', (cantidad) => {
    expect(mensajes({ ...valido, cantidad })).toEqual([
      'La cantidad debe ser un número mayor que cero.'
    ]);
  });

  it('exige motivo y limita su longitud', () => {
    expect(mensajes({ ...valido, motivo: '   ' })).toEqual(['El motivo es obligatorio.']);
    expect(mensajes({ ...valido, motivo: 'x'.repeat(1001) })).toEqual([
      'El motivo no debe exceder 1000 caracteres.'
    ]);
  });

  it('limita el número de lote', () => {
    expect(mensajes({ ...valido, numeroLote: 'x'.repeat(121) })).toEqual([
      'El número de lote no debe exceder 120 caracteres.'
    ]);
  });

  it('rechaza una salida sin lote existente', () => {
    expect(mensajes({ ...valido, tipo: 'AJUSTE_SALIDA', loteId: '' })).toEqual([
      'Una salida requiere un lote existente.'
    ]);
  });

  it('rechaza un ingreso sin lote ni datos del lote nuevo', () => {
    expect(mensajes({ ...valido, loteId: '' })).toEqual([
      'Indica el número del lote.',
      'Indica la fecha de vencimiento.'
    ]);
  });

  it('rechaza un tipo de ajuste desconocido', () => {
    expect(mensajes({ ...valido, tipo: 'OTRO' as AjusteFormValues['tipo'] })).toEqual([
      'Selecciona el tipo de ajuste.'
    ]);
  });
});
