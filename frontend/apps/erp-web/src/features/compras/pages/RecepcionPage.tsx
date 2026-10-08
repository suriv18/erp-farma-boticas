import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Button, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { invalidateInventario } from '../../inventario';
import { useEstablecimientos } from '../../organizacion';
import { ordenQuery } from '../api/ordenes.api';
import type { Orden } from '../api/ordenes.types';
import { registrarRecepcion } from '../api/recepciones.api';
import type { RegistrarRecepcionPayload } from '../api/recepciones.types';
import { ItemsRecepcionEditor } from '../components/ItemsRecepcionEditor';
import { RecepcionCabeceraForm } from '../components/RecepcionCabeceraForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { puedeRecibir } from '../lib/estado-orden';
import { fechaLocalISO } from '../lib/orden-cabecera';
import {
  CABECERA_RECEPCION_VACIA,
  almacenesDeDestino,
  erroresCabeceraRecepcion,
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
  const establecimientos = useEstablecimientos();
  const { claveDe, reiniciar } = useClaveIdempotencia();
  const [cabecera, setCabecera] = useState<CabeceraRecepcion>(CABECERA_RECEPCION_VACIA);
  const [items, setItems] = useState<ItemBorrador[]>(() => itemsDesdeOrden(orden));
  const [intentado, setIntentado] = useState(false);
  const hoy = fechaLocalISO();
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

  const registrar = () => {
    setIntentado(true);
    if (puedeRegistrar(cabecera, items, hoy)) {
      registro.mutate(toRegistrarRecepcionPayload(orden.id, cabecera, items));
    }
  };

  return (
    <>
      <RecepcionCabeceraForm
        valores={cabecera}
        errores={intentado ? erroresCabeceraRecepcion(cabecera) : {}}
        almacenes={almacenesDeDestino(establecimientos, orden.establecimientoDestinoId)}
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
        {intentado && !items.some(itemIncluido) ? (
          <FormError message="Ingresa la cantidad recibida de al menos un producto." />
        ) : null}
      </section>
      {registro.mensajeError ? <FormError message={registro.mensajeError} /> : null}
      <div className="flex flex-wrap gap-3">
        <Button disabled={registro.isPending} onClick={registrar}>
          Registrar recepción
        </Button>
        <Button variant="secondary" onClick={() => void navigate(detalle)}>
          Cancelar
        </Button>
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
    <div className="mx-auto max-w-7xl space-y-6">
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
