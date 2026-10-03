import { z } from 'zod';
import { TIPOS_SKU } from '../api/skus.types';
import { decimal, textoMax, textoRango } from './campos';

const medida = (etiqueta: string, enteros: number, decimales: number) =>
  decimal({ etiqueta, enteros, decimales, requerido: false, permiteCero: false });

export const skuSchema = z
  .object({
    tipoSku: z.enum(TIPOS_SKU, { error: 'Selecciona el tipo de SKU.' }),
    productoReguladoId: z.string(),
    categoriaId: z.string(),
    marcaId: z.string(),
    codigoInterno: textoRango('El código interno', 2, 60),
    descripcionComercial: textoRango('La descripción comercial', 2, 500),
    nombreCorto: textoMax('El nombre corto', 200),
    presentacionComercial: textoMax('La presentación comercial', 300),
    unidadVentaCodigo: z.string().min(1, 'Selecciona la unidad de venta.'),
    contenido: medida('El contenido', 14, 4),
    unidadContenidoCodigo: z.string(),
    pesoGramos: medida('El peso', 14, 4),
    altoCm: medida('El alto', 8, 2),
    anchoCm: medida('El ancho', 8, 2),
    largoCm: medida('El largo', 8, 2),
    permiteVentaFraccion: z.boolean(),
    factorFraccion: medida('El factor de fracción', 14, 4),
    unidadFraccionCodigo: z.string(),
    requiereLote: z.boolean(),
    requiereVencimiento: z.boolean(),
    afectoIgv: z.boolean(),
    stockMinimoDefault: decimal({
      etiqueta: 'El stock mínimo',
      enteros: 14,
      decimales: 4,
      requerido: true,
      permiteCero: true
    }),
    stockMaximoDefault: decimal({
      etiqueta: 'El stock máximo',
      enteros: 14,
      decimales: 4,
      requerido: false,
      permiteCero: true
    }),
    imagenUri: textoMax('La URI de imagen', 2000)
  })
  .superRefine((valores, contexto) => {
    if (valores.tipoSku === 'REGULADO' && !valores.productoReguladoId) {
      contexto.addIssue({
        code: 'custom',
        path: ['productoReguladoId'],
        message: 'Un SKU regulado requiere un producto regulado asociado.'
      });
    }
    if (valores.permiteVentaFraccion && !valores.factorFraccion) {
      contexto.addIssue({
        code: 'custom',
        path: ['factorFraccion'],
        message: 'El factor de fracción es obligatorio cuando se permite venta por fracción.'
      });
    }
    if (valores.permiteVentaFraccion && !valores.unidadFraccionCodigo) {
      contexto.addIssue({
        code: 'custom',
        path: ['unidadFraccionCodigo'],
        message: 'La unidad de fracción es obligatoria cuando se permite venta por fracción.'
      });
    }
    if (!valores.permiteVentaFraccion && valores.factorFraccion) {
      contexto.addIssue({
        code: 'custom',
        path: ['factorFraccion'],
        message: 'El factor de fracción debe estar vacío cuando no se permite venta por fracción.'
      });
    }
    if (!valores.permiteVentaFraccion && valores.unidadFraccionCodigo) {
      contexto.addIssue({
        code: 'custom',
        path: ['unidadFraccionCodigo'],
        message: 'La unidad de fracción debe estar vacía cuando no se permite venta por fracción.'
      });
    }
    if (
      valores.stockMaximoDefault &&
      Number(valores.stockMaximoDefault) < Number(valores.stockMinimoDefault)
    ) {
      contexto.addIssue({
        code: 'custom',
        path: ['stockMaximoDefault'],
        message: 'El stock máximo no puede ser menor que el mínimo.'
      });
    }
  });

export type SkuFormValues = z.infer<typeof skuSchema>;
