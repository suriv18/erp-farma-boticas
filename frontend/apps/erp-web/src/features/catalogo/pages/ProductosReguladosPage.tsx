import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, ListFilters, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash } from '../../../shared/lib/format';
import { crearProductoRegulado, productosReguladosQuery } from '../api/productos-regulados.api';
import {
  ESTADOS_REGULATORIOS,
  type ProductoReguladoResumen
} from '../api/productos-regulados.types';
import { EnlaceDetalle } from '../components/EnlaceDetalle';
import { FiltroSelect } from '../components/FiltroSelect';
import { ProductoReguladoForm } from '../components/ProductoReguladoForm';
import { toProductoPayload } from '../lib/producto-regulado-form';
import { opcionesDeValores, useOpcionesSoporte } from '../lib/use-opciones';
import { condicionesVentaApi } from '../support-catalog/configs/condiciones-venta.config';
import type { ProductoReguladoFormValues } from '../schemas/producto-regulado.schema';

export function ProductosReguladosPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [condicion, setCondicion] = useState('');
  const [estado, setEstado] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const condiciones = useOpcionesSoporte(condicionesVentaApi);

  const { data, isPending, isError } = useQuery(
    productosReguladosQuery({
      q: search || undefined,
      condicionVentaCodigo: condicion || undefined,
      estadoRegulatorio: estado || undefined,
      page,
      size
    })
  );

  const createMutation = useMutation({
    mutationFn: (values: ProductoReguladoFormValues) =>
      crearProductoRegulado(apiClient, toProductoPayload(values)),
    onSuccess: () => {
      setCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['catalogo', 'productos-regulados'] });
    }
  });

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Productos regulados"
        context="Catálogo / Productos regulados"
        description="Administra los productos con registro sanitario y su condición de venta."
        actions={
          <Button
            onClick={() => {
              createMutation.reset();
              setCreateOpen(true);
            }}
          >
            Nuevo producto regulado
          </Button>
        }
      />

      <ListFilters
        label="Buscar producto regulado"
        placeholder="Denominación, concentración o fabricante"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      >
        <FiltroSelect
          id="filtro-condicion-venta"
          label="Condición de venta"
          value={condicion}
          onValueChange={(value) => {
            setCondicion(value);
            setPage(0);
          }}
          opciones={condiciones}
          opcionVacia="Todas"
        />
        <FiltroSelect
          id="filtro-estado-regulatorio"
          label="Estado regulatorio"
          value={estado}
          onValueChange={(value) => {
            setEstado(value);
            setPage(0);
          }}
          opciones={opcionesDeValores(ESTADOS_REGULATORIOS)}
          opcionVacia="Todos"
        />
      </ListFilters>

      <div className="mt-6">
        <DataTable<ProductoReguladoResumen>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Denominación', cell: (row) => row.denominacion },
            { header: 'Condición de venta', cell: (row) => valueOrDash(row.condicionVentaCodigo) },
            {
              header: 'Estado regulatorio',
              cell: (row) => <EstadoBadge status={row.estadoRegulatorio} />
            },
            {
              header: 'Acciones',
              cell: (row) => (
                <EnlaceDetalle
                  nombre={row.denominacion}
                  href={`/catalogo/productos-regulados/${row.id}`}
                />
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron productos regulados."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de productos regulados."
          startIndex={page * size}
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>

      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Nuevo producto regulado"
        size="lg"
      >
        <ProductoReguladoForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear producto regulado"
          isSubmitting={createMutation.isPending}
          error={createMutation.error ? describeApiError(createMutation.error) : null}
        />
      </Modal>
    </div>
  );
}
