import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, ListFilters, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { crearSku, skusQuery } from '../api/skus.api';
import { ESTADOS_SKU, TIPOS_SKU, type SkuResumen } from '../api/skus.types';
import { EnlaceDetalle } from '../components/EnlaceDetalle';
import { FiltroSelect } from '../components/FiltroSelect';
import { SkuForm } from '../components/SkuForm';
import { toSkuPayload } from '../lib/sku-form';
import { opcionesDeValores, useOpcionesCategorias, useOpcionesMarcas } from '../lib/use-opciones';
import type { SkuFormValues } from '../schemas/sku.schema';

export function SkusPage() {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [tipo, setTipo] = useState('');
  const [estado, setEstado] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [marcaId, setMarcaId] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const categorias = useOpcionesCategorias();
  const marcas = useOpcionesMarcas();

  const { data, isPending, isError } = useQuery(
    skusQuery({
      q: search || undefined,
      categoriaId: categoriaId || undefined,
      marcaId: marcaId || undefined,
      tipoSku: tipo || undefined,
      estado: estado || undefined,
      page,
      size
    })
  );

  const createMutation = useMutation({
    mutationFn: (values: SkuFormValues) => crearSku(apiClient, toSkuPayload(values)),
    onSuccess: () => {
      setCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });
    }
  });

  const filtrar = (actualizar: (value: string) => void) => (value: string) => {
    actualizar(value);
    setPage(0);
  };

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="SKU comerciales"
        context="Catálogo / SKU"
        description="Administra los SKU comerciales, sus códigos de barras y su estado."
        actions={
          <Button
            onClick={() => {
              createMutation.reset();
              setCreateOpen(true);
            }}
          >
            Nuevo SKU
          </Button>
        }
      />

      <ListFilters
        label="Buscar SKU"
        placeholder="Código interno o descripción"
        value={search}
        onValueChange={filtrar(setSearch)}
      >
        <FiltroSelect
          id="filtro-tipo-sku"
          label="Tipo de SKU"
          value={tipo}
          onValueChange={filtrar(setTipo)}
          opciones={opcionesDeValores(TIPOS_SKU)}
          opcionVacia="Todos"
        />
        <FiltroSelect
          id="filtro-estado-sku"
          label="Estado"
          value={estado}
          onValueChange={filtrar(setEstado)}
          opciones={opcionesDeValores(ESTADOS_SKU)}
          opcionVacia="Todos"
        />
        <FiltroSelect
          id="filtro-categoria-sku"
          label="Categoría"
          value={categoriaId}
          onValueChange={filtrar(setCategoriaId)}
          opciones={categorias}
          opcionVacia="Todas"
        />
        <FiltroSelect
          id="filtro-marca-sku"
          label="Marca"
          value={marcaId}
          onValueChange={filtrar(setMarcaId)}
          opciones={marcas}
          opcionVacia="Todas"
        />
      </ListFilters>

      <div className="mt-6">
        <DataTable<SkuResumen>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Código interno', cell: (row) => row.codigoInterno },
            { header: 'Descripción comercial', cell: (row) => row.descripcionComercial },
            { header: 'Tipo', cell: (row) => row.tipoSku },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <EnlaceDetalle nombre={row.codigoInterno} href={`/catalogo/skus/${row.id}`} />
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron SKU."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de SKU."
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nuevo SKU" size="lg">
        <SkuForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear SKU"
          isSubmitting={createMutation.isPending}
          error={createMutation.error ? describeApiError(createMutation.error) : null}
        />
      </Modal>
    </div>
  );
}
