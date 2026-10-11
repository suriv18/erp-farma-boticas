import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { CambiarEstadoDialog } from '../../../shared/components/CambiarEstadoDialog';
import { FormError } from '../../../shared/components/FormError';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { invalidateCompras } from '../api/invalidate';
import {
  actualizarProveedor,
  cambiarEstadoProveedor,
  proveedorQuery
} from '../api/proveedores.api';
import { ESTADOS_PROVEEDOR, type EstadoProveedor } from '../api/proveedores.types';
import { ProveedorForm } from '../components/ProveedorForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { proveedorAFormulario, toProveedorPayload } from '../lib/proveedor-form';
import type { ProveedorFormValues } from '../schemas/proveedor.schema';

export function ProveedorDetailPage() {
  const proveedorId = useRouteParam('proveedorId');
  const queryClient = useQueryClient();
  const [estadoAbierto, setEstadoAbierto] = useState(false);
  const result = useQuery(proveedorQuery(proveedorId));

  const actualizar = useMutation({
    mutationFn: (values: ProveedorFormValues) =>
      actualizarProveedor(apiClient, proveedorId, toProveedorPayload(values)),
    onSuccess: () => invalidateCompras(queryClient)
  });
  const cambiarEstado = useMutation({
    mutationFn: (estado: EstadoProveedor) => cambiarEstadoProveedor(apiClient, proveedorId, estado),
    onSuccess: () => {
      setEstadoAbierto(false);
      return invalidateCompras(queryClient);
    }
  });

  const cerrarEstado = () => {
    setEstadoAbierto(false);
    cambiarEstado.reset();
  };

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeErrorCompras(result.error)} />;

  const proveedor = result.data;

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title={proveedor.razonSocial}
        context={<Link to="/compras/proveedores">Compras / Proveedores</Link>}
        description={`Documento ${proveedor.numeroDocumento}`}
        actions={
          <>
            <EstadoBadge status={proveedor.estado} />
            <Button variant="secondary" onClick={() => setEstadoAbierto(true)}>
              Cambiar estado
            </Button>
          </>
        }
      />
      <Card className="mt-6 space-y-4 p-6">
        <ProveedorForm
          defaultValues={proveedorAFormulario(proveedor)}
          submitLabel="Guardar cambios"
          isSubmitting={actualizar.isPending}
          error={actualizar.isError ? describeErrorCompras(actualizar.error) : null}
          onSubmit={(values) => actualizar.mutate(values)}
        />
        {actualizar.isSuccess ? (
          <p role="status" className="text-success-700 dark:text-success-400 text-sm font-medium">
            Proveedor actualizado.
          </p>
        ) : null}
      </Card>
      {estadoAbierto ? (
        <CambiarEstadoDialog
          title={`Cambiar estado de ${proveedor.razonSocial}`}
          estados={ESTADOS_PROVEEDOR}
          current={proveedor.estado}
          isSubmitting={cambiarEstado.isPending}
          error={cambiarEstado.isError ? describeErrorCompras(cambiarEstado.error) : null}
          onSubmit={(estado) => cambiarEstado.mutate(estado)}
          onClose={cerrarEstado}
        />
      ) : null}
    </div>
  );
}
