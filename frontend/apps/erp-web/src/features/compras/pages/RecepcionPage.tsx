import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Button, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { invalidateInventario } from '../../inventario';
import { useEstablecimientosEstado } from '../../organizacion';
import { ordenQuery } from '../api/ordenes.api';
import type { Orden } from '../api/ordenes.types';
import { registrarRecepcion } from '../api/recepciones.api';
import type { RegistrarRecepcionPayload } from '../api/recepciones.types';
import { ItemsRecepcionEditor } from '../components/ItemsRecepcionEditor';
import { RecepcionCabeceraForm } from '../components/RecepcionCabeceraForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { puedeRecibir } from '../lib/estado-orden';
import { fechaUtcISO } from '../lib/fechas-compras';
import {
  CABECERA_RECEPCION_VACIA,
  almacenesDeDestino,
  erroresCabeceraRecepcion,
  estadoAlmacenes,
  puedeRegistrar,
  toRegistrarRecepcionPayload,
  type CabeceraRecepcion
} from '../lib/recepcion-cabecera';
import {
  actualizarItem,
  itemIncluido,
  itemsDesdeOrden,
  type ItemBorrador
} from '../lib/recepcion-items';
import { useMutacionCompras } from '../lib/use-mutacion-compras';

function RecepcionForm({ orden }: { orden: Orden }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { establecimientos, isPending, isError } = useEstablecimientosEstado();
  const { claveDe, reiniciar } = useClaveIdempotencia();
  const [cabecera, setCabecera] = useState<CabeceraRecepcion>(CABECERA_RECEPCION_VACIA);
  const [items, setItems] = useState<ItemBorrador[]>(() => itemsDesdeOrden(orden));
  const [intentado, setIntentado] = useState(false);
  const hoy = fechaUtcISO();
  const almacenes = almacenesDeDestino(establecimientos, orden.establecimientoDestinoId);
  const detalle = `/compras/ordenes/${orden.id}`;
  const registro = useMutacionCompras(
    (payload: RegistrarRecepcionPayload) =>
      registrarRecepcion(apiClient, payload, claveDe(payload)),
    () => {
      reiniciar();
      void invalidateInventario(queryClient);
      void navigate(detalle);
    }
  );

  const valido = puedeRegistrar(cabecera, items, hoy);
  const mensajeValidacion = items.some(itemIncluido)
    ? 'Corrige los errores indicados antes de registrar la recepción.'
    : 'Ingresa la cantidad recibida de al menos un producto.';
  const mensajeAccion = intentado && !valido ? mensajeValidacion : registro.mensajeError;

  const registrar = () => {
    setIntentado(true);
    if (valido) {
      registro.mutate(toRegistrarRecepcionPayload(orden.id, cabecera, items));
    }
  };

  return (
    <>
      <RecepcionCabeceraForm
        valores={cabecera}
        errores={intentado ? erroresCabeceraRecepcion(cabecera) : {}}
        almacenes={almacenes}
        estadoAlmacenes={estadoAlmacenes({ isPending, isError }, almacenes.length)}
        onCambiar={(campo, valor) => setCabecera((actual) => ({ ...actual, [campo]: valor }))}
      />
      <section className="space-y-4">
        <h2 className="text-base font-semibold">Productos</h2>
        <ItemsRecepcionEditor
          items={items}
          hoy={hoy}
          mostrarErrores={intentado}
          onCambiar={(numeroLineaOrden, cambios) =>
            setItems((actuales) => actualizarItem(actuales, numeroLineaOrden, cambios))
          }
        />
      </section>
      <div role="group" aria-label="Acciones de la recepción" className="space-y-3">
        {mensajeAccion ? <FormError message={mensajeAccion} /> : null}
        <div className="flex flex-wrap gap-3">
          <Button disabled={registro.isPending} onClick={registrar}>
            Registrar recepción
          </Button>
          <Button variant="secondary" onClick={() => void navigate(detalle)}>
            Cancelar
          </Button>
        </div>
      </div>
    </>
  );
}

export function RecepcionPage() {
  const ordenId = useRouteParam('ordenId');
  const result = useQuery(ordenQuery(ordenId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeErrorCompras(result.error)} />;

  const orden = result.data;

  return (
    <div className="erp-page erp-page--form space-y-6">
      <PageHeader
        title={`Recepción de la orden ${orden.numero}`}
        context={
          <Link to={`/compras/ordenes/${orden.id}`}>{`Compras / Órdenes / ${orden.numero}`}</Link>
        }
        description="Registra la mercadería recibida con su lote y vencimiento."
      />
      {puedeRecibir(orden.estado) ? (
        <RecepcionForm orden={orden} />
      ) : (
        <FormError message="La orden no admite recepciones en su estado actual." />
      )}
    </div>
  );
}
