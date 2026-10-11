import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { crearProveedor } from '../api/proveedores.api';
import { invalidateCompras } from '../api/invalidate';
import { ProveedorForm } from '../components/ProveedorForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { toProveedorPayload } from '../lib/proveedor-form';
import type { ProveedorFormValues } from '../schemas/proveedor.schema';

export function NuevoProveedorPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const crear = useMutation({
    mutationFn: (values: ProveedorFormValues) =>
      crearProveedor(apiClient, toProveedorPayload(values)),
    onSuccess: (proveedor) => {
      void invalidateCompras(queryClient);
      void navigate(`/compras/proveedores/${proveedor.id}`);
    }
  });

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="Nuevo proveedor"
        context={<Link to="/compras/proveedores">Compras / Proveedores</Link>}
        description="Registra un proveedor para poder comprarle."
      />
      <Card className="mt-6 p-6">
        <ProveedorForm
          submitLabel="Crear proveedor"
          isSubmitting={crear.isPending}
          error={crear.isError ? describeErrorCompras(crear.error) : null}
          onSubmit={(values) => crear.mutate(values)}
          onCancel={() => void navigate('/compras/proveedores')}
        />
      </Card>
    </div>
  );
}
