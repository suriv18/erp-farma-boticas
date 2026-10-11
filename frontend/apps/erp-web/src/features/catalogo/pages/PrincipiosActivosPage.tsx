import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Power } from 'lucide-react';
import {
  Button,
  DataTable,
  EstadoBadge,
  IconButton,
  ListFilters,
  Modal,
  PageHeader
} from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import {
  actualizarPrincipioActivo,
  cambiarEstadoPrincipioActivo,
  crearPrincipioActivo,
  principiosActivosQuery
} from '../api/principios-activos.api';
import type { PrincipioActivo, PrincipioActivoPayload } from '../api/principios-activos.types';
import { PrincipioActivoForm } from '../components/PrincipioActivoForm';
import { textoDeFormulario, textoOpcional } from '../lib/form-values';
import type { PrincipioActivoFormValues } from '../schemas/principio-activo.schema';

function toPayload(values: PrincipioActivoFormValues): PrincipioActivoPayload {
  return {
    codigoFuente: textoOpcional(values.codigoFuente),
    denominacion: values.denominacion,
    nombreNormalizado: textoOpcional(values.nombreNormalizado),
    fuente: textoOpcional(values.fuente)
  };
}

function toFormValues(item: PrincipioActivo): PrincipioActivoFormValues {
  return {
    codigoFuente: textoDeFormulario(item.codigoFuente),
    denominacion: item.denominacion,
    nombreNormalizado: textoDeFormulario(item.nombreNormalizado),
    fuente: textoDeFormulario(item.fuente)
  };
}

export function PrincipiosActivosPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<PrincipioActivo | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery(
    principiosActivosQuery({ texto: search || undefined })
  );
  const rows = data ?? [];

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['catalogo', 'principios-activos'] });

  const createMutation = useMutation({
    mutationFn: (values: PrincipioActivoFormValues) =>
      crearPrincipioActivo(apiClient, toPayload(values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidate();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: PrincipioActivoFormValues) =>
      actualizarPrincipioActivo(apiClient, editing!.id, toPayload(values)),
    onSuccess: () => {
      setEditing(null);
      void invalidate();
    }
  });

  const statusMutation = useMutation({
    mutationFn: (item: PrincipioActivo) =>
      cambiarEstadoPrincipioActivo(
        apiClient,
        item.id,
        item.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
      ),
    onSuccess: () => invalidate()
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Principios activos"
        context="Catálogo / Principios activos"
        description="Administra los principios activos que componen los productos regulados."
        actions={
          <Button
            onClick={() => {
              createMutation.reset();
              setCreateOpen(true);
            }}
          >
            Nuevo principio activo
          </Button>
        }
      />

      <ListFilters
        label="Buscar principio activo"
        placeholder="Denominación o código fuente"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      {statusMutation.error ? (
        <div className="mt-4">
          <FormError message={describeApiError(statusMutation.error)} />
        </div>
      ) : null}

      <div className="mt-6">
        <DataTable<PrincipioActivo>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Denominación', cell: (row) => row.denominacion },
            { header: 'Código fuente', cell: (row) => row.codigoFuente ?? '—' },
            { header: 'Fuente', cell: (row) => row.fuente ?? '—' },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <div className="flex items-center gap-1">
                  <IconButton
                    icon={Pencil}
                    label={`Editar ${row.denominacion}`}
                    onClick={() => {
                      updateMutation.reset();
                      setEditing(row);
                    }}
                  />
                  <IconButton
                    icon={Power}
                    label={
                      row.estado === 'ACTIVO'
                        ? `Desactivar ${row.denominacion}`
                        : `Activar ${row.denominacion}`
                    }
                    tone={row.estado === 'ACTIVO' ? 'danger' : 'default'}
                    onClick={() => statusMutation.mutate(row)}
                  />
                </div>
              )
            }
          ]}
          rows={rows.slice(page * size, (page + 1) * size)}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron principios activos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de principios activos."
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements: rows.length,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo principio activo">
        <PrincipioActivoForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear principio activo"
          isSubmitting={createMutation.isPending}
          error={createMutation.error ? describeApiError(createMutation.error) : null}
        />
      </Modal>

      <Modal
        open={editing !== null}
        onClose={() => setEditing(null)}
        title="Editar principio activo"
      >
        {editing ? (
          <PrincipioActivoForm
            defaultValues={toFormValues(editing)}
            onSubmit={(values) => updateMutation.mutate(values)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
            error={updateMutation.error ? describeApiError(updateMutation.error) : null}
          />
        ) : null}
      </Modal>
    </div>
  );
}
