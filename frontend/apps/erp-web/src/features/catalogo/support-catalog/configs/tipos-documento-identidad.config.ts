import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type TipoDocumentoIdentidad = SupportCatalogItem & {
  sigla: string;
  denominacion: string;
  max: number | null;
  min: number | null;
};

export type TipoDocumentoIdentidadRequest = {
  codigo: string;
  sigla: string;
  denominacion: string;
  max?: number | undefined;
  min?: number | undefined;
};

export const tipoDocumentoIdentidadSchema = z.object({
  codigo: z
    .string()
    .length(1, 'El código SUNAT debe ser un único carácter alfanumérico.')
    .regex(/^[0-9A-Z]$/, 'El código debe ser un dígito o una letra mayúscula.'),
  sigla: z.string().min(1, 'La sigla es obligatoria.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  max: z.number().positive('Debe ser un número positivo.').optional(),
  min: z.number().positive('Debe ser un número positivo.').optional()
});

export const tipoDocumentoIdentidadResolver = zodResolver(tipoDocumentoIdentidadSchema);

export const tipoDocumentoIdentidadFields: FieldDef[] = [
  { name: 'codigo', label: 'Código SUNAT', type: 'text' },
  { name: 'sigla', label: 'Sigla', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'min', label: 'Longitud mínima', type: 'number' },
  { name: 'max', label: 'Longitud máxima', type: 'number' }
];

export const tipoDocumentoIdentidadColumns: SupportCatalogColumn<TipoDocumentoIdentidad>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Sigla', cell: (row) => row.sigla },
  { header: 'Denominación', cell: (row) => row.denominacion }
];

export const tiposDocumentoIdentidadApi = createSupportCatalogApi<
  TipoDocumentoIdentidad,
  TipoDocumentoIdentidadRequest
>('tipos-documento-identidad', { paginated: true });
