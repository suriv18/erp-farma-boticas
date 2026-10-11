import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { formatoMoneda, valueOrDash, yesNo } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { actualizarSku, cambiarEstadoSku, skuQuery } from '../api/skus.api';
import { ESTADOS_SKU } from '../api/skus.types';
import { CodigosBarraSection } from '../components/CodigosBarraSection';
import { EstadoDialog } from '../components/EstadoDialog';
import { SkuForm } from '../components/SkuForm';
import { numeroDeFormulario } from '../lib/form-values';
import { toSkuFormValues, toSkuPayload } from '../lib/sku-form';
import {
  etiquetaDe,
  useOpcionesCategorias,
  useOpcionesMarcas,
  useOpcionesProductosRegulados
} from '../lib/use-opciones';
import type { Opcion } from '../components/CamposFormulario';
import type { SkuFormValues } from '../schemas/sku.schema';

function nombreDe(opciones: readonly Opcion[], id: string | null): string | null {
  return id === null ? null : etiquetaDe(opciones, id);
}

export function SkuDetailPage() {
  const skuId = useRouteParam('skuId');
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [estadoOpen, setEstadoOpen] = useState(false);
  const productos = useOpcionesProductosRegulados();
  const categorias = useOpcionesCategorias();
  const marcas = useOpcionesMarcas();

  const result = useQuery(skuQuery(skuId));

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });

  const updateMutation = useMutation({
    mutationFn: (values: SkuFormValues) => actualizarSku(apiClient, skuId, toSkuPayload(values)),
    onSuccess: () => {
      setEditOpen(false);
      void invalidate();
    }
  });

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
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

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
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={sku.descripcionComercial}
        context={<Link to="/catalogo/skus">Catálogo / SKU</Link>}
        description={`Código interno ${sku.codigoInterno}`}
        actions={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                updateMutation.reset();
                setEditOpen(true);
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

      <Card className="mt-6 p-6">
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoBadge status={sku.estado} />
          </DatoItem>
          {datos.map(([etiqueta, valor]) => (
            <DatoItem key={etiqueta} label={etiqueta}>
              {valor}
            </DatoItem>
          ))}
        </dl>
      </Card>

      <CodigosBarraSection sku={sku} />

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title="Editar SKU" size="lg">
        <SkuForm
          defaultValues={toSkuFormValues(sku)}
          onSubmit={(values) => updateMutation.mutate(values)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.error ? describeApiError(updateMutation.error) : null}
        />
      </Modal>

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
