import type { Sku } from '../api/skus.types';
import { SKU_VACIO, toSkuFormValues, toSkuPayload } from './sku-form';

const sku: Sku = {
  id: 'sku-1',
  tenantId: 't-1',
  productoReguladoId: 'pr-1',
  categoriaId: null,
  marcaId: 'm-1',
  tipoSku: 'REGULADO',
  codigoInterno: 'SKU-1',
  descripcionComercial: 'Paracetamol 500 mg x 100',
  nombreCorto: null,
  presentacionComercial: 'Caja x 100',
  unidadVentaCodigo: 'UND',
  contenido: 100,
  unidadContenidoCodigo: null,
  pesoGramos: 12.5,
  altoCm: null,
  anchoCm: null,
  largoCm: null,
  permiteVentaFraccion: true,
  factorFraccion: 10,
  unidadFraccionCodigo: 'UND',
  requiereLote: true,
  requiereVencimiento: false,
  afectoIgv: true,
  stockMinimoDefault: 5,
  stockMaximoDefault: null,
  imagenUri: null,
  codigosBarra: [],
  estado: 'ACTIVO',
  createdBy: 'admin',
  createdAt: '2026-01-01T00:00:00Z',
  updatedBy: null,
  updatedAt: null
};

describe('sku-form', () => {
  it('toSkuFormValues convierte nulos y numeros a valores de formulario', () => {
    expect(toSkuFormValues(sku)).toEqual({
      ...SKU_VACIO,
      productoReguladoId: 'pr-1',
      marcaId: 'm-1',
      codigoInterno: 'SKU-1',
      descripcionComercial: 'Paracetamol 500 mg x 100',
      presentacionComercial: 'Caja x 100',
      unidadVentaCodigo: 'UND',
      contenido: '100',
      pesoGramos: '12.5',
      permiteVentaFraccion: true,
      factorFraccion: '10',
      unidadFraccionCodigo: 'UND',
      requiereVencimiento: false,
      stockMinimoDefault: '5'
    });
  });

  it('toSkuPayload convierte numeros y omite los opcionales vacios', () => {
    const payload = toSkuPayload(toSkuFormValues(sku));

    expect(payload).toMatchObject({
      tipoSku: 'REGULADO',
      productoReguladoId: 'pr-1',
      contenido: 100,
      pesoGramos: 12.5,
      permiteVentaFraccion: true,
      factorFraccion: 10,
      stockMinimoDefault: 5
    });
    expect(payload).not.toHaveProperty('tenantId');
    expect(payload.categoriaId).toBeUndefined();
    expect(payload.stockMaximoDefault).toBeUndefined();
    expect(payload.imagenUri).toBeUndefined();
  });

  it('toSkuPayload con el formulario vacio usa los valores por defecto del DDL', () => {
    expect(toSkuPayload(SKU_VACIO)).toMatchObject({
      tipoSku: 'REGULADO',
      permiteVentaFraccion: false,
      requiereLote: true,
      requiereVencimiento: true,
      afectoIgv: true,
      stockMinimoDefault: 0
    });
  });
});
