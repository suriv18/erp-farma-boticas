import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createSupportCatalogApi } from '../support-catalog.api';
import type { FieldDef, SupportCatalogItem } from '../support-catalog.types';
import type { SupportCatalogColumn } from '../SupportCatalogPage';

export type CondicionVenta = SupportCatalogItem & {
  denominacion: string;
  requiereReceta: boolean;
  requiereRetencion: boolean;
  fuente: string | null;
  versionFuente: string | null;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
};

export type CondicionVentaRequest = {
  codigo: string;
  denominacion: string;
  requiereReceta: boolean;
  requiereRetencion: boolean;
  fuente?: string | undefined;
  versionFuente?: string | undefined;
};

export const condicionVentaSchema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio.').max(30, 'Máximo 30 caracteres.'),
  denominacion: z
    .string()
    .min(2, 'La denominación debe tener al menos 2 caracteres.')
    .max(200, 'Máximo 200 caracteres.'),
  requiereReceta: z.boolean(),
  requiereRetencion: z.boolean(),
  fuente: z.string().max(300, 'Máximo 300 caracteres.').optional(),
  versionFuente: z.string().max(100, 'Máximo 100 caracteres.').optional()
});

export const condicionVentaResolver = zodResolver(condicionVentaSchema);

export const condicionVentaFields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' },
  { name: 'requiereReceta', label: 'Requiere receta', type: 'checkbox' },
  { name: 'requiereRetencion', label: 'Requiere retención', type: 'checkbox' },
  { name: 'fuente', label: 'Fuente', type: 'text' },
  { name: 'versionFuente', label: 'Versión de fuente', type: 'text' }
];

export const condicionVentaColumns: SupportCatalogColumn<CondicionVenta>[] = [
  { header: 'Código', cell: (row) => row.codigo },
  { header: 'Denominación', cell: (row) => row.denominacion },
  { header: 'Requiere receta', cell: (row) => (row.requiereReceta ? 'Sí' : 'No') }
];

export const condicionesVentaApi = createSupportCatalogApi<CondicionVenta, CondicionVentaRequest>(
  'condiciones-venta'
);
