import { SKU_VACIO } from '../lib/sku-form';
import { skuSchema, type SkuFormValues } from './sku.schema';

const valido: SkuFormValues = {
  ...SKU_VACIO,
  tipoSku: 'NO_REGULADO',
  codigoInterno: 'SKU-1',
  descripcionComercial: 'Alcohol 70%',
  unidadVentaCodigo: 'UND'
};

function mensajes(valores: Partial<Record<keyof SkuFormValues, unknown>>) {
  const resultado = skuSchema.safeParse({ ...valido, ...valores });
  return resultado.success ? [] : resultado.error.issues.map((issue) => issue.message);
}

describe('skuSchema', () => {
  it('acepta un SKU no regulado minimo', () => {
    expect(mensajes({})).toEqual([]);
  });

  it.each([
    [{ tipoSku: '' }, 'Selecciona el tipo de SKU.'],
    [{ codigoInterno: 'a' }, 'El código interno debe tener al menos 2 caracteres.'],
    [{ codigoInterno: 'x'.repeat(61) }, 'El código interno no debe exceder 60 caracteres.'],
    [{ descripcionComercial: 'a' }, 'La descripción comercial debe tener al menos 2 caracteres.'],
    [
      { descripcionComercial: 'x'.repeat(501) },
      'La descripción comercial no debe exceder 500 caracteres.'
    ],
    [{ nombreCorto: 'x'.repeat(201) }, 'El nombre corto no debe exceder 200 caracteres.'],
    [
      { presentacionComercial: 'x'.repeat(301) },
      'La presentación comercial no debe exceder 300 caracteres.'
    ],
    [{ unidadVentaCodigo: '' }, 'Selecciona la unidad de venta.'],
    [{ contenido: '0' }, 'El contenido debe ser un número mayor que cero con hasta 4 decimales.'],
    [{ pesoGramos: '1.12345' }, 'El peso debe ser un número mayor que cero con hasta 4 decimales.'],
    [{ altoCm: '1.234' }, 'El alto debe ser un número mayor que cero con hasta 2 decimales.'],
    [{ anchoCm: '-1' }, 'El ancho debe ser un número mayor que cero con hasta 2 decimales.'],
    [{ largoCm: 'abc' }, 'El largo debe ser un número mayor que cero con hasta 2 decimales.'],
    [
      { stockMinimoDefault: '' },
      'El stock mínimo debe ser un número mayor o igual a cero con hasta 4 decimales.'
    ],
    [
      { stockMinimoDefault: '-1' },
      'El stock mínimo debe ser un número mayor o igual a cero con hasta 4 decimales.'
    ],
    [{ imagenUri: 'x'.repeat(2001) }, 'La URI de imagen no debe exceder 2000 caracteres.']
  ])('rechaza %j', (valores, mensaje) => {
    expect(mensajes(valores)).toContain(mensaje);
  });

  it('exige un producto regulado cuando el SKU es regulado', () => {
    expect(mensajes({ tipoSku: 'REGULADO' })).toEqual([
      'Un SKU regulado requiere un producto regulado asociado.'
    ]);
    expect(mensajes({ tipoSku: 'REGULADO', productoReguladoId: 'pr-1' })).toEqual([]);
  });

  it('exige factor y unidad de fraccion cuando se permite venta por fraccion', () => {
    expect(mensajes({ permiteVentaFraccion: true })).toEqual([
      'El factor de fracción es obligatorio cuando se permite venta por fracción.',
      'La unidad de fracción es obligatoria cuando se permite venta por fracción.'
    ]);
    expect(
      mensajes({ permiteVentaFraccion: true, factorFraccion: '10', unidadFraccionCodigo: 'UND' })
    ).toEqual([]);
  });

  it('prohibe factor y unidad de fraccion cuando no se permite venta por fraccion', () => {
    expect(mensajes({ factorFraccion: '10', unidadFraccionCodigo: 'UND' })).toEqual([
      'El factor de fracción debe estar vacío cuando no se permite venta por fracción.',
      'La unidad de fracción debe estar vacía cuando no se permite venta por fracción.'
    ]);
  });

  it('valida el precio de venta de referencia opcional con hasta 4 decimales', () => {
    expect(mensajes({ precioVentaReferencia: '' })).toEqual([]);
    expect(mensajes({ precioVentaReferencia: '12.5' })).toEqual([]);
    expect(mensajes({ precioVentaReferencia: '-1' })).toEqual([
      'El precio de venta de referencia debe ser un número mayor o igual a cero con hasta 4 decimales.'
    ]);
    expect(mensajes({ precioVentaReferencia: '1.00001' })).toEqual([
      'El precio de venta de referencia debe ser un número mayor o igual a cero con hasta 4 decimales.'
    ]);
  });

  it('impide un stock maximo menor que el minimo', () => {
    expect(mensajes({ stockMinimoDefault: '10', stockMaximoDefault: '5' })).toEqual([
      'El stock máximo no puede ser menor que el mínimo.'
    ]);
    expect(mensajes({ stockMinimoDefault: '10', stockMaximoDefault: '10' })).toEqual([]);
    expect(mensajes({ stockMinimoDefault: '10', stockMaximoDefault: '' })).toEqual([]);
  });
});
