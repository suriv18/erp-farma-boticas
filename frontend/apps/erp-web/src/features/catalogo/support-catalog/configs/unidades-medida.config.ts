import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type UnidadMedida = SupportCatalogItem & {
  denominacion: string;
  simbolo: string | null;
  permiteDecimal: boolean;
  fuente: string | null;
};

export type UnidadMedidaRequest = {
  codigo: string;
  denominacion: string;
  simbolo?: string | undefined;
  permiteDecimal: boolean;
  fuente?: string | undefined;
};

export const unidadMedidaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(150, 'Máximo 150 caracteres.'),
  simbolo: z.string().max(30, 'Máximo 30 caracteres.').optional(),
  permiteDecimal: z.boolean(),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional()
});

export const unidadMedidaResolver = zodResolver(unidadMedidaSchema);

export const unidadMedidaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'simbolo', label: 'Símbolo', type: 'text' },
  { name: 'permiteDecimal', label: 'Permite decimal', type: 'checkbox' },
  { name: 'fuente', label: 'Fuente', type: 'text' }
];

export const unidadMedidaColumns: SupportCatalogColumn<UnidadMedida>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Símbolo', cell: (row) => row.simbolo ?? '—' },
  { header: 'Permite decimal', cell: (row) => (row.permiteDecimal ? 'Sí' : 'No') }
];

export const unidadesMedidaApi = createSupportCatalogApi<UnidadMedida, UnidadMedidaRequest>(
  'unidades-medida'
);
