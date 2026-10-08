import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, ListFilters, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { crearEmpresa, empresasQuery } from '../api/empresas.api';
import type { Empresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { AccionesFila } from '../components/AccionesFila';
import { EmpresaEditarDialog } from '../components/EmpresaEditarDialog';
import { EmpresaForm } from '../components/EmpresaForm';
import { describeApiError } from '../lib/describe-api-error';
import { valueOrDash } from '../lib/format';
import { toCrearEmpresaPayload } from '../lib/form-payloads';
import type { EmpresaFormValues } from '../schemas/empresa.schema';

export function EmpresasPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Empresa | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery(empresasQuery({ search, page, size }));

  const createMutation = useMutation({
    mutationFn: (values: EmpresaFormValues) =>
      crearEmpresa(apiClient, toCrearEmpresaPayload(values)),
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
            { header: 'Razón social', cell: (row) => row.razonSocial },
            { header: 'Nombre comercial', cell: (row) => valueOrDash(row.nombreComercial) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <AccionesFila
                  nombre={row.razonSocial}
                  detalleHref={`/organizacion/empresas/${row.id}`}
                  onEditar={() => setEditing(row)}
                />
              )
            }
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

      {editing ? <EmpresaEditarDialog empresa={editing} onClose={() => setEditing(null)} /> : null}
    </div>
  );
}
