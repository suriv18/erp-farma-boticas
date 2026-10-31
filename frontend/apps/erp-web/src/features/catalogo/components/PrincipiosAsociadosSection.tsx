import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { Card, DataTable, IconButton } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash, yesNo } from '../../../shared/lib/format';
import { asociarPrincipioActivo, desasociarPrincipioActivo } from '../api/productos-regulados.api';
import type { PrincipioActivoAsociado, ProductoRegulado } from '../api/productos-regulados.types';
import { toAsociarPayload } from '../lib/asociacion-principio-activo';
import { numeroDeFormulario } from '../lib/form-values';
import { etiquetaDe, useOpcionesPrincipiosActivos, useOpcionesSoporte } from '../lib/use-opciones';
import type { AsociacionPrincipioActivoFormValues } from '../schemas/asociacion-principio-activo.schema';
import { unidadesMedidaApi } from '../support-catalog/configs/unidades-medida.config';
import { AsociarPrincipioActivoForm } from './AsociarPrincipioActivoForm';

export type PrincipiosAsociadosSectionProps = {
  producto: ProductoRegulado;
};

export function PrincipiosAsociadosSection({ producto }: PrincipiosAsociadosSectionProps) {
  const queryClient = useQueryClient();
  const principios = useOpcionesPrincipiosActivos();
  const unidades = useOpcionesSoporte(unidadesMedidaApi);
  const [formKey, setFormKey] = useState(0);

  const nombreDe = (principioActivoId: string) => etiquetaDe(principios, principioActivoId);

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['catalogo', 'productos-regulados'] });

  const asociarMutation = useMutation({
    mutationFn: (values: AsociacionPrincipioActivoFormValues) =>
      asociarPrincipioActivo(apiClient, producto.id, toAsociarPayload(values)),
    onSuccess: () => {
      setFormKey((key) => key + 1);
      void invalidate();
    }
  });

  const quitarMutation = useMutation({
    mutationFn: (principioActivoId: string) =>
      desasociarPrincipioActivo(apiClient, producto.id, principioActivoId),
    onSuccess: () => invalidate()
  });

  return (
    <section className="mt-8" aria-labelledby="principios-asociados-titulo">
      <h2
        id="principios-asociados-titulo"
        className="text-lg font-bold text-neutral-900 dark:text-neutral-50"
      >
        Principios activos
      </h2>

      {quitarMutation.error ? (
        <div className="mt-3">
          <FormError message={describeApiError(quitarMutation.error)} />
        </div>
      ) : null}

      <div className="mt-4">
        <DataTable<PrincipioActivoAsociado>
          columns={[
            { header: 'Principio activo', cell: (row) => nombreDe(row.principioActivoId) },
            { header: 'Concentración', cell: (row) => valueOrDash(row.concentracionTexto) },
            { header: 'Cantidad', cell: (row) => valueOrDash(numeroDeFormulario(row.cantidad)) },
            { header: 'Unidad', cell: (row) => valueOrDash(row.unidadMedidaCodigo) },
            { header: 'Principal', cell: (row) => yesNo(row.esPrincipal) },
            { header: 'Orden', cell: (row) => row.orden },
            {
              header: 'Acciones',
              cell: (row) => (
                <IconButton
                  icon={Trash2}
                  label={`Quitar ${nombreDe(row.principioActivoId)}`}
                  tone="danger"
                  onClick={() => quitarMutation.mutate(row.principioActivoId)}
                />
              )
            }
          ]}
          rows={producto.principiosActivos}
          rowKey={(row) => row.principioActivoId}
          emptyMessage="El producto no tiene principios activos asociados."
        />
      </div>

      <Card className="mt-4 p-6">
        <AsociarPrincipioActivoForm
          key={formKey}
          principios={principios}
          unidades={unidades}
          onSubmit={(values) => asociarMutation.mutate(values)}
          isSubmitting={asociarMutation.isPending}
          error={asociarMutation.error ? describeApiError(asociarMutation.error) : null}
        />
      </Card>
    </section>
  );
}
