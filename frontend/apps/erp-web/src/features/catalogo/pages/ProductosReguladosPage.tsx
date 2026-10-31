import { useNavigate } from 'react-router';
import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';
import { useQuery } from '@tanstack/react-query';
import { Button, DataTable, EstadoBadge, ListFilters, PageHeader } from '@boticas/ui-web';
import { valueOrDash } from '../../../shared/lib/format';
import { productosReguladosQuery } from '../api/productos-regulados.api';
import {
  ESTADOS_REGULATORIOS,
  type ProductoReguladoResumen
} from '../api/productos-regulados.types';
import { EnlaceDetalle } from '../components/EnlaceDetalle';
import { FiltroSelect } from '../components/FiltroSelect';
import { opcionesDeValores, useOpcionesSoporte } from '../lib/use-opciones';
import { condicionesVentaApi } from '../support-catalog/configs/condiciones-venta.config';

export function ProductosReguladosPage() {
  const navigate = useNavigate();
  const { params, actualizar } = useParametrosUrl();
  const search = params.get('search') ?? '';
  const setSearch = (value: string) => actualizar({ search: String(value) });
  const condicion = params.get('condicion') ?? '';
  const setCondicion = (value: string) => actualizar({ condicion: String(value) });
  const estado = params.get('estado') ?? '';
  const setEstado = (value: string) => actualizar({ estado: String(value) });
  const page = enteroEnRango(params.get('page'), 0, 1000000, 0);
  const setPage = (value: number) => actualizar({ page: String(value) });
  const size = enteroEnRango(params.get('size'), 1, 100, 20);
  const setSize = (value: number) => actualizar({ size: String(value) });
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

  return (
    <div className="erp-page">
      <PageHeader
        title="Productos regulados"
        context="Catálogo / Productos regulados"
        description="Administra los productos con registro sanitario y su condición de venta."
        actions={
          <Button
            onClick={() => {
              void navigate(
                '/catalogo/productos-regulados/nuevo' + (params.size ? '?' + params.toString() : '')
              );
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
        {search || condicion || estado ? (
          <Button
            variant="ghost"
            onClick={() => {
              setSearch('');
              setCondicion('');
              setEstado('');
              setPage(0);
            }}
          >
            Limpiar filtros
          </Button>
        ) : null}
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
