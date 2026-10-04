import type { Sku, SkuPayload } from '../api/skus.types';
import type { SkuFormValues } from '../schemas/sku.schema';
import {
  numeroDeFormulario,
  numeroOpcional,
  textoDeFormulario,
  textoOpcional
} from './form-values';

export const SKU_VACIO: SkuFormValues = {
  tipoSku: 'REGULADO',
  productoReguladoId: '',
  categoriaId: '',
  marcaId: '',
  codigoInterno: '',
  descripcionComercial: '',
  nombreCorto: '',
  presentacionComercial: '',
  unidadVentaCodigo: '',
  contenido: '',
  unidadContenidoCodigo: '',
  pesoGramos: '',
  altoCm: '',
  anchoCm: '',
  largoCm: '',
  permiteVentaFraccion: false,
  factorFraccion: '',
  unidadFraccionCodigo: '',
  requiereLote: true,
  requiereVencimiento: true,
  afectoIgv: true,
  stockMinimoDefault: '0',
  stockMaximoDefault: '',
  precioVentaReferencia: '',
  imagenUri: ''
};

export function toSkuPayload(values: SkuFormValues): SkuPayload {
  return {
    tipoSku: values.tipoSku,
    codigoInterno: values.codigoInterno,
    descripcionComercial: values.descripcionComercial,
    productoReguladoId: textoOpcional(values.productoReguladoId),
    categoriaId: textoOpcional(values.categoriaId),
    marcaId: textoOpcional(values.marcaId),
    nombreCorto: textoOpcional(values.nombreCorto),
    presentacionComercial: textoOpcional(values.presentacionComercial),
    unidadVentaCodigo: textoOpcional(values.unidadVentaCodigo),
    contenido: numeroOpcional(values.contenido),
    unidadContenidoCodigo: textoOpcional(values.unidadContenidoCodigo),
    pesoGramos: numeroOpcional(values.pesoGramos),
    altoCm: numeroOpcional(values.altoCm),
    anchoCm: numeroOpcional(values.anchoCm),
    largoCm: numeroOpcional(values.largoCm),
    permiteVentaFraccion: values.permiteVentaFraccion,
    factorFraccion: numeroOpcional(values.factorFraccion),
    unidadFraccionCodigo: textoOpcional(values.unidadFraccionCodigo),
    requiereLote: values.requiereLote,
    requiereVencimiento: values.requiereVencimiento,
    afectoIgv: values.afectoIgv,
    stockMinimoDefault: Number(values.stockMinimoDefault),
    stockMaximoDefault: numeroOpcional(values.stockMaximoDefault),
    precioVentaReferencia: numeroOpcional(values.precioVentaReferencia),
    imagenUri: textoOpcional(values.imagenUri)
  };
}

export function toSkuFormValues(sku: Sku): SkuFormValues {
  return {
    tipoSku: sku.tipoSku as SkuFormValues['tipoSku'],
    productoReguladoId: textoDeFormulario(sku.productoReguladoId),
    categoriaId: textoDeFormulario(sku.categoriaId),
    marcaId: textoDeFormulario(sku.marcaId),
    codigoInterno: sku.codigoInterno,
    descripcionComercial: sku.descripcionComercial,
    nombreCorto: textoDeFormulario(sku.nombreCorto),
    presentacionComercial: textoDeFormulario(sku.presentacionComercial),
    unidadVentaCodigo: textoDeFormulario(sku.unidadVentaCodigo),
    contenido: numeroDeFormulario(sku.contenido),
    unidadContenidoCodigo: textoDeFormulario(sku.unidadContenidoCodigo),
    pesoGramos: numeroDeFormulario(sku.pesoGramos),
    altoCm: numeroDeFormulario(sku.altoCm),
    anchoCm: numeroDeFormulario(sku.anchoCm),
    largoCm: numeroDeFormulario(sku.largoCm),
    permiteVentaFraccion: sku.permiteVentaFraccion,
    factorFraccion: numeroDeFormulario(sku.factorFraccion),
    unidadFraccionCodigo: textoDeFormulario(sku.unidadFraccionCodigo),
    requiereLote: sku.requiereLote,
    requiereVencimiento: sku.requiereVencimiento,
    afectoIgv: sku.afectoIgv,
    stockMinimoDefault: numeroDeFormulario(sku.stockMinimoDefault),
    stockMaximoDefault: numeroDeFormulario(sku.stockMaximoDefault),
    precioVentaReferencia: numeroDeFormulario(sku.precioVentaReferencia),
    imagenUri: textoDeFormulario(sku.imagenUri)
  };
}
