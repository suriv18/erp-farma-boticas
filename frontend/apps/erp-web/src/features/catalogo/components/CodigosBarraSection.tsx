import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Star, Trash2 } from 'lucide-react';
import { Card, DataTable, EstadoBadge, IconButton } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash, yesNo } from '../../../shared/lib/format';
import {
  agregarCodigoBarra,
  eliminarCodigoBarra,
  marcarCodigoBarraPrincipal
} from '../api/skus.api';
import type { CodigoBarraSku, Sku } from '../api/skus.types';
import { textoOpcional } from '../lib/form-values';
import type { CodigoBarraFormValues } from '../schemas/codigo-barra.schema';
import { CodigoBarraForm } from './CodigoBarraForm';

export type CodigosBarraSectionProps = {
  sku: Sku;
};

export function CodigosBarraSection({ sku }: CodigosBarraSectionProps) {
  const queryClient = useQueryClient();
  const [formKey, setFormKey] = useState(0);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });

  const agregarMutation = useMutation({
    mutationFn: (values: CodigoBarraFormValues) =>
      agregarCodigoBarra(apiClient, sku.id, {
        codigoBarra: values.codigoBarra,
        tipoCodigo: textoOpcional(values.tipoCodigo),
        vigenteDesde: textoOpcional(values.vigenteDesde),
        vigenteHasta: textoOpcional(values.vigenteHasta)
      }),
    onSuccess: () => {
      setFormKey((key) => key + 1);
      void invalidate();
    }
  });

  const principalMutation = useMutation({
    mutationFn: (codigoBarra: string) => marcarCodigoBarraPrincipal(apiClient, sku.id, codigoBarra),
    onSuccess: () => invalidate()
  });

  const eliminarMutation = useMutation({
    mutationFn: (codigoBarra: string) => eliminarCodigoBarra(apiClient, sku.id, codigoBarra),
    onSuccess: () => invalidate()
  });

  const accionError = principalMutation.error ?? eliminarMutation.error;

  return (
    <section className="mt-8" aria-labelledby="codigos-barra-titulo">
      <h2
        id="codigos-barra-titulo"
        className="text-lg font-bold text-neutral-900 dark:text-neutral-50"
      >
        Códigos de barras
      </h2>

      {accionError ? (
        <div className="mt-3">
          <FormError message={describeApiError(accionError)} />
        </div>
      ) : null}

      <div className="mt-4">
        <DataTable<CodigoBarraSku>
          columns={[
            { header: 'Código', cell: (row) => row.codigoBarra },
            { header: 'Tipo', cell: (row) => valueOrDash(row.tipoCodigo) },
            { header: 'Principal', cell: (row) => yesNo(row.esPrincipal) },
            { header: 'Vigente desde', cell: (row) => valueOrDash(row.vigenteDesde) },
            { header: 'Vigente hasta', cell: (row) => valueOrDash(row.vigenteHasta) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <div className="flex items-center gap-1">
                  {row.esPrincipal ? null : (
                    <IconButton
                      icon={Star}
                      label={`Marcar ${row.codigoBarra} como principal`}
                      onClick={() => principalMutation.mutate(row.codigoBarra)}
                    />
                  )}
                  <IconButton
                    icon={Trash2}
                    label={`Eliminar ${row.codigoBarra}`}
                    tone="danger"
                    onClick={() => eliminarMutation.mutate(row.codigoBarra)}
                  />
                </div>
              )
            }
          ]}
          rows={sku.codigosBarra}
          rowKey={(row) => row.codigoBarra}
          emptyMessage="El SKU no tiene códigos de barras registrados."
        />
      </div>

      <Card className="mt-4 p-6">
        <CodigoBarraForm
          key={formKey}
          onSubmit={(values) => agregarMutation.mutate(values)}
          isSubmitting={agregarMutation.isPending}
          error={agregarMutation.error ? describeApiError(agregarMutation.error) : null}
        />
      </Card>
    </section>
  );
}
