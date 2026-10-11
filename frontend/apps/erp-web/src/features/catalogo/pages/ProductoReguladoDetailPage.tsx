import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import {
  actualizarProductoRegulado,
  cambiarEstadoProductoRegulado,
  productoReguladoQuery
} from '../api/productos-regulados.api';
import { ESTADOS_REGULATORIOS, type ProductoRegulado } from '../api/productos-regulados.types';
import { EstadoDialog } from '../components/EstadoDialog';
import { PrincipiosAsociadosSection } from '../components/PrincipiosAsociadosSection';
import { ProductoReguladoForm } from '../components/ProductoReguladoForm';
import { toProductoFormValues, toProductoPayload } from '../lib/producto-regulado-form';
import type { ProductoReguladoFormValues } from '../schemas/producto-regulado.schema';

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
  const [editOpen, setEditOpen] = useState(false);
  const [estadoOpen, setEstadoOpen] = useState(false);

  const result = useQuery(productoReguladoQuery(productoReguladoId));

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['catalogo', 'productos-regulados'] });

  const updateMutation = useMutation({
    mutationFn: (values: ProductoReguladoFormValues) =>
      actualizarProductoRegulado(apiClient, productoReguladoId, toProductoPayload(values)),
    onSuccess: () => {
      setEditOpen(false);
      void invalidate();
    }
  });

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
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const producto = result.data;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={producto.denominacion}
        context={<Link to="/catalogo/productos-regulados">Catálogo / Productos regulados</Link>}
        description={producto.tipoProducto}
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
          <DatoItem label="Estado regulatorio">
            <EstadoBadge status={producto.estadoRegulatorio} />
          </DatoItem>
          {DATOS.map(([etiqueta, clave]) => (
            <DatoItem key={clave} label={etiqueta}>
              {valueOrDash(producto[clave])}
            </DatoItem>
          ))}
        </dl>
      </Card>

      <PrincipiosAsociadosSection producto={producto} />

      <Modal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        title="Editar producto regulado"
        size="lg"
      >
        <ProductoReguladoForm
          defaultValues={toProductoFormValues(producto)}
          onSubmit={(values) => updateMutation.mutate(values)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.error ? describeApiError(updateMutation.error) : null}
        />
      </Modal>

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
