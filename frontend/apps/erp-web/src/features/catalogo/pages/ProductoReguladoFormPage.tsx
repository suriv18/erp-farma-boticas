import { useRef, useState } from 'react';
import { UnsavedChanges } from '../../../shared/components/UnsavedChanges';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams, useLocation } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import {
  actualizarProductoRegulado,
  crearProductoRegulado,
  productoReguladoQuery
} from '../api/productos-regulados.api';
import { ProductoReguladoForm } from '../components/ProductoReguladoForm';
import {
  PRODUCTO_REGULADO_VACIO,
  toProductoFormValues,
  toProductoPayload
} from '../lib/producto-regulado-form';
import type { ProductoReguladoFormValues } from '../schemas/producto-regulado.schema';

export function ProductoReguladoFormPage() {
  const { productoReguladoId } = useParams();
  const navigate = useNavigate();
  const { search } = useLocation();
  const [dirty, setDirty] = useState(false);
  const saved = useRef(false);
  const queryClient = useQueryClient();
  const editing = Boolean(productoReguladoId);
  const backTo = productoReguladoId
    ? '/catalogo/productos-regulados/' + productoReguladoId
    : '/catalogo/productos-regulados';
  const result = useQuery({ ...productoReguladoQuery(productoReguladoId ?? ''), enabled: editing });
  const mutation = useMutation({
    mutationFn: (values: ProductoReguladoFormValues) =>
      productoReguladoId
        ? actualizarProductoRegulado(apiClient, productoReguladoId, toProductoPayload(values))
        : crearProductoRegulado(apiClient, toProductoPayload(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['catalogo', 'productos-regulados'] });
      saved.current = true;
      void navigate(backTo + search);
    }
  });

  return (
    <div className="erp-page erp-page--form">
      <UnsavedChanges dirty={() => dirty && !saved.current} />
      <PageHeader
        title={editing ? 'Editar producto regulado' : 'Nuevo producto regulado'}
        context={
          <Link to={backTo + search}>
            {editing ? 'Volver al detalle' : 'Catálogo / Productos regulados'}
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
          <ProductoReguladoForm
            key={productoReguladoId ?? 'nuevo'}
            defaultValues={
              result.data ? toProductoFormValues(result.data) : PRODUCTO_REGULADO_VACIO
            }
            onSubmit={(values) => {
              if (!mutation.isPending) mutation.mutate(values);
            }}
            submitLabel={editing ? 'Guardar cambios' : 'Crear producto regulado'}
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
