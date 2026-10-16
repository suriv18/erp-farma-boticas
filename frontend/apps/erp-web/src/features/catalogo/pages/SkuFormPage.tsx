import { useRef, useState } from 'react';
import { UnsavedChanges } from '../../../shared/components/UnsavedChanges';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams, useLocation } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { actualizarSku, crearSku, skuQuery } from '../api/skus.api';
import { SkuForm } from '../components/SkuForm';
import { SKU_VACIO, toSkuFormValues, toSkuPayload } from '../lib/sku-form';
import type { SkuFormValues } from '../schemas/sku.schema';

export function SkuFormPage() {
  const { skuId } = useParams();
  const navigate = useNavigate();
  const { search } = useLocation();
  const [dirty, setDirty] = useState(false);
  const saved = useRef(false);
  const queryClient = useQueryClient();
  const editing = Boolean(skuId);
  const backTo = skuId ? '/catalogo/skus/' + skuId : '/catalogo/skus';
  const result = useQuery({ ...skuQuery(skuId ?? ''), enabled: editing });
  const mutation = useMutation({
    mutationFn: (values: SkuFormValues) =>
      skuId
        ? actualizarSku(apiClient, skuId, toSkuPayload(values))
        : crearSku(apiClient, toSkuPayload(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });
      saved.current = true;
      void navigate(backTo + search);
    }
  });

  return (
    <div className="erp-page erp-page--form">
      <UnsavedChanges dirty={() => dirty && !saved.current} />
      <PageHeader
        title={editing ? 'Editar SKU' : 'Nuevo SKU'}
        context={
          <Link to={backTo + search}>
            {editing ? 'Volver al detalle' : 'Catálogo / SKU comerciales'}
          </Link>
        }
        description={
          editing
            ? 'Actualiza la información del registro y guarda los cambios.'
            : 'Completa la información del nuevo registro. Los campos están organizados por sección.'
        }
      />
      {editing && result.isPending ? (
        <p role="status" className="mt-6 text-sm text-neutral-500">
          Cargando registro…
        </p>
      ) : editing && result.isError ? (
        <div className="mt-6 space-y-4">
          <FormError message={describeApiError(result.error)} />
          <Button
            variant="secondary"
            onClick={() => {
              void result.refetch();
            }}
          >
            Reintentar
          </Button>
        </div>
      ) : (
        <Card className="mt-6 p-4 sm:p-6">
          <SkuForm
            key={skuId ?? 'nuevo'}
            defaultValues={result.data ? toSkuFormValues(result.data) : SKU_VACIO}
            onSubmit={(values) => {
              if (!mutation.isPending) mutation.mutate(values);
            }}
            submitLabel={editing ? 'Guardar cambios' : 'Crear SKU'}
            isSubmitting={mutation.isPending}
            error={mutation.error ? describeApiError(mutation.error) : null}
            onCancel={() => {
              void navigate(backTo + search);
            }}
            onDirtyChange={setDirty}
            fullPage
          />
        </Card>
      )}
    </div>
  );
}
