import { FormError } from '../../../shared/components/FormError';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useLocation } from 'react-router';
import { Button, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { CatalogDetailSection } from '../components/CatalogDetailSection';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { formatoMoneda, valueOrDash, yesNo } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { cambiarEstadoSku, skuQuery } from '../api/skus.api';
import { ESTADOS_SKU } from '../api/skus.types';
import { CodigosBarraSection } from '../components/CodigosBarraSection';
import { EstadoDialog } from '../components/EstadoDialog';
import { numeroDeFormulario } from '../lib/form-values';
import {
  etiquetaDe,
  useOpcionesCategorias,
  useOpcionesMarcas,
  useOpcionesProductosRegulados
} from '../lib/use-opciones';
import type { Opcion } from '../components/CamposFormulario';

function nombreDe(opciones: readonly Opcion[], id: string | null): string | null {
  return id === null ? null : etiquetaDe(opciones, id);
}

export function SkuDetailPage() {
  const skuId = useRouteParam('skuId');
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { search } = useLocation();
  const [estadoOpen, setEstadoOpen] = useState(false);
  const productos = useOpcionesProductosRegulados();
  const categorias = useOpcionesCategorias();
  const marcas = useOpcionesMarcas();

  const result = useQuery(skuQuery(skuId));

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });

  const estadoMutation = useMutation({
    mutationFn: (estado: string) => cambiarEstadoSku(apiClient, skuId, estado),
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
          to={'/catalogo/skus' + search}
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

  const sku = result.data;
  const datos: ReadonlyArray<readonly [string, string]> = [
    ['Tipo de SKU', sku.tipoSku],
    ['Código interno', sku.codigoInterno],
    ['Producto regulado', valueOrDash(nombreDe(productos, sku.productoReguladoId))],
    ['Categoría', valueOrDash(nombreDe(categorias, sku.categoriaId))],
    ['Marca', valueOrDash(nombreDe(marcas, sku.marcaId))],
    ['Nombre corto', valueOrDash(sku.nombreCorto)],
    ['Presentación comercial', valueOrDash(sku.presentacionComercial)],
    ['Unidad de venta', valueOrDash(sku.unidadVentaCodigo)],
    ['Contenido', valueOrDash(numeroDeFormulario(sku.contenido))],
    ['Unidad de contenido', valueOrDash(sku.unidadContenidoCodigo)],
    ['Peso (g)', valueOrDash(numeroDeFormulario(sku.pesoGramos))],
    ['Alto (cm)', valueOrDash(numeroDeFormulario(sku.altoCm))],
    ['Ancho (cm)', valueOrDash(numeroDeFormulario(sku.anchoCm))],
    ['Largo (cm)', valueOrDash(numeroDeFormulario(sku.largoCm))],
    ['Permite venta por fracción', yesNo(sku.permiteVentaFraccion)],
    ['Factor de fracción', valueOrDash(numeroDeFormulario(sku.factorFraccion))],
    ['Unidad de fracción', valueOrDash(sku.unidadFraccionCodigo)],
    ['Requiere lote', yesNo(sku.requiereLote)],
    ['Requiere vencimiento', yesNo(sku.requiereVencimiento)],
    ['Afecto a IGV', yesNo(sku.afectoIgv)],
    ['Stock mínimo por defecto', numeroDeFormulario(sku.stockMinimoDefault)],
    ['Stock máximo por defecto', valueOrDash(numeroDeFormulario(sku.stockMaximoDefault))],
    [
      'Precio de venta de referencia',
      sku.precioVentaReferencia === null ? '—' : formatoMoneda(sku.precioVentaReferencia)
    ],
    ['URI de imagen', valueOrDash(sku.imagenUri)]
  ];

  return (
    <div className="erp-page">
      <PageHeader
        title={sku.descripcionComercial}
        context={<Link to={'/catalogo/skus' + search}>Catálogo / SKU</Link>}
        description={`Código interno ${sku.codigoInterno}`}
        actions={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                void navigate('/catalogo/skus/' + skuId + '/editar' + search);
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
        title="Identificación comercial"
        description="Datos principales y clasificación del SKU."
        items={[['Estado', <EstadoBadge key="estado" status={sku.estado} />], ...datos.slice(0, 7)]}
      />
      <CatalogDetailSection
        title="Unidades y medidas"
        description="Presentación, contenido y dimensiones del producto."
        items={datos.slice(7, 14)}
      />
      <CatalogDetailSection
        title="Venta por fracción"
        description="Configuración de unidades para la venta fraccionada."
        items={datos.slice(14, 17)}
      />
      <CatalogDetailSection
        title="Control, stock y precio"
        description="Trazabilidad, límites de stock y precio de referencia."
        items={datos.slice(17, 24)}
      />

      <CodigosBarraSection sku={sku} />

      {estadoOpen ? (
        <EstadoDialog
          title="Cambiar estado del SKU"
          estados={ESTADOS_SKU}
          current={sku.estado}
          isSubmitting={estadoMutation.isPending}
          error={estadoMutation.error ? describeApiError(estadoMutation.error) : null}
          onSubmit={(estado) => estadoMutation.mutate(estado)}
          onClose={() => setEstadoOpen(false)}
        />
      ) : null}
    </div>
  );
}
