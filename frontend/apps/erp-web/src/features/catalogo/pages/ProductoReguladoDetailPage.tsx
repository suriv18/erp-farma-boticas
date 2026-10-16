import { FormError } from '../../../shared/components/FormError';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useLocation } from 'react-router';
import { Button, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { CatalogDetailSection } from '../components/CatalogDetailSection';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import {
  cambiarEstadoProductoRegulado,
  productoReguladoQuery
} from '../api/productos-regulados.api';
import { ESTADOS_REGULATORIOS, type ProductoRegulado } from '../api/productos-regulados.types';
import { EstadoDialog } from '../components/EstadoDialog';
import { PrincipiosAsociadosSection } from '../components/PrincipiosAsociadosSection';

type ClaveTexto = {
  [K in keyof ProductoRegulado]: ProductoRegulado[K] extends string | null ? K : never;
}[keyof ProductoRegulado];

const DATOS: ReadonlyArray<readonly [string, ClaveTexto]> = [
  ['Tipo de producto', 'tipoProducto'],
  ['Tipo de registro', 'tipoRegistro'],
  ['Número de registro', 'numeroRegistro'],
  ['Rubro', 'rubroCodigo'],
  ['Concentración', 'concentracionTexto'],
  ['Presentación regulatoria', 'presentacionRegulatoria'],
  ['Forma farmacéutica', 'formaFarmaceuticaCodigo'],
  ['Vía de administración', 'viaAdministracionCodigo'],
  ['Unidad de medida', 'unidadMedidaCodigo'],
  ['Condición de venta', 'condicionVentaCodigo'],
  ['Clasificación controlada', 'clasificacionControladaCodigo'],
  ['Clasificación ATC', 'clasificacionAtc'],
  ['Tipo de liberación', 'tipoLiberacion'],
  ['Origen de fabricación', 'origenFabricacion'],
  ['País de origen', 'paisOrigen'],
  ['Subpartida nacional', 'subpartidaNacional'],
  ['Titular de registro', 'titularRegistro'],
  ['Fabricante', 'fabricante'],
  ['Importador', 'importador'],
  ['Establecimiento de expendio', 'establecimientoExpendio'],
  ['Vigente desde', 'vigenteDesde'],
  ['Vigente hasta', 'vigenteHasta'],
  ['Fuente', 'fuente'],
  ['Versión de fuente', 'versionFuente']
];

export function ProductoReguladoDetailPage() {
  const productoReguladoId = useRouteParam('productoReguladoId');
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { search } = useLocation();
  const [estadoOpen, setEstadoOpen] = useState(false);

  const result = useQuery(productoReguladoQuery(productoReguladoId));

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['catalogo', 'productos-regulados'] });

  const estadoMutation = useMutation({
    mutationFn: (estado: string) =>
      cambiarEstadoProductoRegulado(apiClient, productoReguladoId, estado),
    onSuccess: () => {
      setEstadoOpen(false);
      void invalidate();
    }
  });

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError)
    return (
      <div className="erp-page space-y-4">
        <Link
          className="text-primary-700 dark:text-primary-300 underline"
          to={'/catalogo/productos-regulados' + search}
        >
          Volver al listado
        </Link>
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
    );

  const producto = result.data;

  return (
    <div className="erp-page">
      <PageHeader
        title={producto.denominacion}
        context={
          <Link to={'/catalogo/productos-regulados' + search}>Catálogo / Productos regulados</Link>
        }
        description={producto.tipoProducto}
        actions={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                void navigate(
                  '/catalogo/productos-regulados/' + productoReguladoId + '/editar' + search
                );
              }}
            >
              Editar
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                estadoMutation.reset();
                setEstadoOpen(true);
              }}
            >
              Cambiar estado
            </Button>
          </>
        }
      />

      <CatalogDetailSection
        title="Identificación y registro"
        description="Información del producto y su registro sanitario."
        items={[
          ['Estado regulatorio', <EstadoBadge key="estado" status={producto.estadoRegulatorio} />],
          ...DATOS.slice(0, 6).map(([label, key]) => [label, valueOrDash(producto[key])] as const)
        ]}
      />
      <CatalogDetailSection
        title="Clasificación"
        description="Forma, vía de administración y condiciones de venta."
        items={DATOS.slice(6, 12).map(
          ([label, key]) => [label, valueOrDash(producto[key])] as const
        )}
      />
      <CatalogDetailSection
        title="Origen y titularidad"
        description="Fabricación y responsables del producto."
        items={DATOS.slice(12, 20).map(
          ([label, key]) => [label, valueOrDash(producto[key])] as const
        )}
      />
      <CatalogDetailSection
        title="Vigencia y fuente"
        description="Fechas de vigencia y procedencia de la información."
        items={DATOS.slice(20, 24).map(
          ([label, key]) => [label, valueOrDash(producto[key])] as const
        )}
      />

      <PrincipiosAsociadosSection producto={producto} />

      {estadoOpen ? (
        <EstadoDialog
          title="Cambiar estado regulatorio"
          estados={ESTADOS_REGULATORIOS}
          current={producto.estadoRegulatorio}
          isSubmitting={estadoMutation.isPending}
          error={estadoMutation.error ? describeApiError(estadoMutation.error) : null}
          onSubmit={(estado) => estadoMutation.mutate(estado)}
          onClose={() => setEstadoOpen(false)}
        />
      ) : null}
    </div>
  );
}
