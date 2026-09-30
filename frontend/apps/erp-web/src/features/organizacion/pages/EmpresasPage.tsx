import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, DataTable, EstadoBadge, ListFilters, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { crearEmpresa, empresasQuery } from '../api/empresas.api';
import type { Empresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { EmpresaForm } from '../components/EmpresaForm';
import { describeApiError } from '../lib/describe-api-error';
import { valueOrDash } from '../lib/format';
import { toCrearEmpresaPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EmpresaFormValues } from '../schemas/empresa.schema';

export function EmpresasPage() {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery({
    ...empresasQuery({ tenantId, search, page, size }),
    enabled: tenantId !== ''
  });

  const createMutation = useMutation({
    mutationFn: (values: EmpresaFormValues) =>
      crearEmpresa(apiClient, toCrearEmpresaPayload(tenantId, values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Empresas"
        context="Organización / Empresas"
        description="Administra las empresas operadoras del tenant."
        actions={<Button onClick={() => setCreateOpen(true)}>Nueva empresa</Button>}
      />

      <ListFilters
        label="Buscar empresa"
        placeholder="RUC, razón social o nombre comercial"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      <div className="mt-6">
        <DataTable<Empresa>
          columns={[
            { header: 'RUC', cell: (row) => row.ruc },
            {
              header: 'Razón social',
              cell: (row) => (
                <Link
                  className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  to={`/organizacion/empresas/${row.id}`}
                >
                  {row.razonSocial}
                </Link>
              )
            },
            { header: 'Nombre comercial', cell: (row) => valueOrDash(row.nombreComercial) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Aún no hay empresas registradas."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de empresas."
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nueva empresa" size="lg">
        <EmpresaForm
          onSubmit={(values) => createMutation.mutate(values)}
          onCancel={closeCreate}
          submitLabel="Crear empresa"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>
    </div>
  );
}
