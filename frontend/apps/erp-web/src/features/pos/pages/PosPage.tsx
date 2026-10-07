import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Badge, Button, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';
import { invalidateCaja, turnoActualQuery } from '../../caja';
import { invalidateInventario } from '../../inventario';
import { useEstablecimientos, usePuestoTrabajo, type PuestoTrabajo } from '../../organizacion';
import { ComprobanteVenta, invalidateVentas, registrarVenta } from '../../ventas';
import { AvisoTurno } from '../components/AvisoTurno';
import { BuscadorProductos } from '../components/BuscadorProductos';
import { CarritoTable } from '../components/CarritoTable';
import { PanelCobro } from '../components/PanelCobro';
import { PuestoVenta } from '../components/PuestoVenta';
import {
  actualizarLinea,
  agregarSku,
  puedeCobrar,
  quitarLinea,
  toRegistrarVentaPayload,
  totalCarrito,
  vueltoDe,
  type Carrito
} from '../lib/carrito';
import { describeErrorPos } from '../lib/errores-pos';

export function PosPage() {
  const queryClient = useQueryClient();
  const { puesto, setPuesto } = usePuestoTrabajo();
  const establecimientos = useEstablecimientos();
  const claveDe = useClaveIdempotencia();
  const [carrito, setCarrito] = useState<Carrito>([]);
  const [recibido, setRecibido] = useState('');
  const { data: turno } = useQuery({
    ...turnoActualQuery(puesto.terminalId),
    enabled: puesto.terminalId !== ''
  });
  const cobro = useMutation({
    mutationFn: () => {
      const payload = toRegistrarVentaPayload(
        carrito,
        puesto.terminalId,
        puesto.almacenId,
        recibido
      );
      return registrarVenta(apiClient, payload, claveDe(payload));
    },
    onSuccess: () =>
      Promise.all([
        invalidateVentas(queryClient),
        invalidateCaja(queryClient),
        invalidateInventario(queryClient)
      ])
  });

  const total = totalCarrito(carrito);
  const sinTerminal = puesto.terminalId === '';

  const vaciar = () => {
    setCarrito([]);
    setRecibido('');
    cobro.reset();
  };

  const cambiarPuesto = (nuevo: PuestoTrabajo) => {
    if (nuevo.terminalId !== puesto.terminalId) vaciar();
    setPuesto(nuevo);
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <PageHeader
        title="Punto de venta"
        context="Operaciones / Punto de venta"
        description="Venta en efectivo."
        actions={
          turno === undefined ? null : (
            <Badge tone={turno === null ? 'warning' : 'success'}>
              {turno === null ? 'Sin turno' : 'Turno abierto'}
            </Badge>
          )
        }
      />
      <PuestoVenta puesto={puesto} onChange={cambiarPuesto} />
      {sinTerminal ? (
        <p className="text-sm text-neutral-500 dark:text-neutral-400">
          Selecciona una terminal para vender.
        </p>
      ) : null}
      {turno === null ? <AvisoTurno /> : null}
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="min-w-0 space-y-6">
          <BuscadorProductos
            almacenId={puesto.almacenId}
            deshabilitado={sinTerminal}
            onElegir={(sku) => setCarrito((actual) => agregarSku(actual, sku))}
          />
          <CarritoTable
            carrito={carrito}
            onCambiar={(skuId, cambios) =>
              setCarrito((actual) => actualizarLinea(actual, skuId, cambios))
            }
            onQuitar={(skuId) => setCarrito((actual) => quitarLinea(actual, skuId))}
          />
        </div>
        <PanelCobro
          total={total}
          recibido={recibido}
          onRecibido={setRecibido}
          vuelto={vueltoDe(total, recibido)}
          puedeCobrar={puedeCobrar({
            carrito,
            recibido,
            hayTurno: turno != null,
            hayAlmacen: puesto.almacenId !== ''
          })}
          isSubmitting={cobro.isPending}
          error={cobro.isError ? describeErrorPos(cobro.error) : null}
          onCobrar={() => cobro.mutate()}
        />
      </div>
      {cobro.data ? (
        <Modal open onClose={vaciar} title="Venta registrada">
          <div className="space-y-4">
            <ComprobanteVenta
              venta={cobro.data}
              nombreEstablecimiento={
                establecimientos.find(({ id }) => id === cobro.data.establecimientoId)?.name ?? ''
              }
            />
            <Button onClick={vaciar}>Nueva venta</Button>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}
