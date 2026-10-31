import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type ClasificacionControlada = SupportCatalogItem & {
  denominacion: string;
  normaFuente: string | null;
  requiereRecetaEspecial: boolean;
  retieneReceta: boolean;
  vigenciaRecetaDias: number | null;
};

export type ClasificacionControladaRequest = {
  codigo: string;
  denominacion: string;
  normaFuente?: string | undefined;
  requiereRecetaEspecial: boolean;
  retieneReceta: boolean;
  vigenciaRecetaDias?: number | undefined;
};

export const clasificacionControladaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(40, 'Máximo 40 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  normaFuente: z.string().max(300, 'Máximo 300 caracteres.').optional(),
  requiereRecetaEspecial: z.boolean(),
  retieneReceta: z.boolean(),
  vigenciaRecetaDias: z.coerce.number().int().positive('Debe ser un número positivo.').optional()
});

export const clasificacionControladaResolver = zodResolver(clasificacionControladaSchema);

export const clasificacionControladaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'normaFuente', label: 'Norma fuente', type: 'text' },
  { name: 'requiereRecetaEspecial', label: 'Requiere receta especial', type: 'checkbox' },
  { name: 'retieneReceta', label: 'Retiene receta', type: 'checkbox' },
  { name: 'vigenciaRecetaDias', label: 'Vigencia de receta (días)', type: 'number' }
];

export const clasificacionControladaColumns: SupportCatalogColumn<ClasificacionControlada>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Requiere receta especial', cell: (row) => (row.requiereRecetaEspecial ? 'Sí' : 'No') }
];

export const clasificacionesControladasApi = createSupportCatalogApi<
  ClasificacionControlada,
  ClasificacionControladaRequest
>('clasificaciones-controladas');
