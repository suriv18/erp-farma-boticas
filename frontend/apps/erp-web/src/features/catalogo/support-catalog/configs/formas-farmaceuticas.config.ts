import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type FormaFarmaceutica = SupportCatalogItem & {
  denominacion: string;
  fuente: string | null;
};

export type FormaFarmaceuticaRequest = {
  codigo: string;
  denominacion: string;
  fuente?: string | undefined;
};

export const formaFarmaceuticaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional()
});

export const formaFarmaceuticaResolver = zodResolver(formaFarmaceuticaSchema);

export const formaFarmaceuticaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'fuente', label: 'Fuente', type: 'text' }
];

export const formaFarmaceuticaColumns: SupportCatalogColumn<FormaFarmaceutica>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Fuente', cell: (row) => row.fuente ?? '—' }
];

export const formasFarmaceuticasApi = createSupportCatalogApi<FormaFarmaceutica, FormaFarmaceuticaRequest>(
  'formas-farmaceuticas'
);
