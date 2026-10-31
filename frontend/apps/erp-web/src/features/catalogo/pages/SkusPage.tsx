import { useNavigate } from 'react-router';
import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';
import { useQuery } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, ListFilters, PageHeader } from '@boticas/ui-web';
import { skusQuery } from '../api/skus.api';
import { ESTADOS_SKU, TIPOS_SKU, type SkuResumen } from '../api/skus.types';
import { EnlaceDetalle } from '../components/EnlaceDetalle';
import { FiltroSelect } from '../components/FiltroSelect';
import { opcionesDeValores, useOpcionesCategorias, useOpcionesMarcas } from '../lib/use-opciones';

export function SkusPage() {
  const navigate = useNavigate();
  const { params, actualizar } = useParametrosUrl();
  const search = params.get('search') ?? '';
  const setSearch = (value: string) => actualizar({ search: String(value) });
  const tipo = params.get('tipo') ?? '';
  const setTipo = (value: string) => actualizar({ tipo: String(value) });
  const estado = params.get('estado') ?? '';
  const setEstado = (value: string) => actualizar({ estado: String(value) });
  const categoriaId = params.get('categoriaId') ?? '';
  const setCategoriaId = (value: string) => actualizar({ categoriaId: String(value) });
  const marcaId = params.get('marcaId') ?? '';
  const setMarcaId = (value: string) => actualizar({ marcaId: String(value) });
  const page = enteroEnRango(params.get('page'), 0, 1000000, 0);
  const setPage = (value: number) => actualizar({ page: String(value) });
  const size = enteroEnRango(params.get('size'), 1, 100, 20);
  const setSize = (value: number) => actualizar({ size: String(value) });
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

  const filtrar = (actualizar: (value: string) => void) => (value: string) => {
    actualizar(value);
    setPage(0);
  };

  return (
    <div className="erp-page">
      <PageHeader
        title="SKU comerciales"
        context="Catálogo / SKU"
        description="Administra los SKU comerciales, sus códigos de barras y su estado."
        actions={
          <Button
            onClick={() => {
              void navigate('/catalogo/skus/nuevo' + (params.size ? '?' + params.toString() : ''));
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
        {search || tipo || estado || categoriaId || marcaId ? (
          <Button
            variant="ghost"
            onClick={() => {
              setSearch('');
              setTipo('');
              setEstado('');
              setCategoriaId('');
              setMarcaId('');
              setPage(0);
            }}
          >
            Limpiar filtros
          </Button>
        ) : null}
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
            onSizeChange: (value) => {
              setSize(value);
              setPage(0);
            }
          }}
        />
      </div>
    </div>
  );
}
