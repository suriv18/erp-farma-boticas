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
import { formatoMoneda } from '../../../shared/lib/format';
import { errorLinea } from '../lib/carrito';

export function PosPage() {
  const queryClient = useQueryClient();
  const { puesto, setPuesto } = usePuestoTrabajo();
  const establecimientos = useEstablecimientos();
  const { claveDe, reiniciar } = useClaveIdempotencia();
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
    onSuccess: () => {
      reiniciar();
      return Promise.all([
        invalidateVentas(queryClient),
        invalidateCaja(queryClient),
        invalidateInventario(queryClient)
      ]);
    }
  });

  const total = totalCarrito(carrito);
  const sinTerminal = puesto.terminalId === '';
  const motivoBloqueo = sinTerminal
    ? 'Selecciona una terminal para vender.'
    : turno === undefined
      ? 'Comprobando el turno de caja…'
      : turno === null
        ? 'Abre un turno de caja para cobrar.'
        : puesto.almacenId === ''
          ? 'Selecciona un almacén.'
          : carrito.length === 0
            ? 'Agrega un producto para comenzar la venta.'
            : carrito.some((linea) => errorLinea(linea) !== null)
              ? 'Revisa la cantidad y el precio de los productos.'
              : vueltoDe(total, recibido) === null
                ? 'Ingresa un monto válido igual o mayor al total.'
                : undefined;

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
    <div className="erp-page space-y-4 pb-24 xl:pb-0">
      <PageHeader
        title="Punto de venta"
        context="Operaciones / Punto de venta"
        description="Busca productos, revisa tu carrito y cobra en efectivo."
        actions={
          turno === undefined ? null : (
            <Badge tone={turno === null ? 'warning' : 'success'}>
              {turno === null ? 'Sin turno' : 'Turno abierto'}
            </Badge>
          )
        }
      />
      <PuestoVenta puesto={puesto} onChange={cambiarPuesto} />
      {turno === null ? <AvisoTurno /> : null}
      <div className="grid items-start gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
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
        <section
          id="pos-cobro"
          aria-label="Cobro de la venta"
          tabIndex={-1}
          className="min-w-0 scroll-mt-6 xl:sticky xl:top-6"
        >
          <PanelCobro
            motivoBloqueo={motivoBloqueo}
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
        </section>
      </div>
      {!cobro.data && carrito.length > 0 ? (
        <div className="fixed inset-x-0 bottom-0 z-20 flex items-center justify-between gap-3 border-t border-neutral-200 bg-white px-4 pt-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] shadow-lg lg:left-56 xl:hidden dark:border-neutral-700 dark:bg-neutral-900">
          <span className="font-semibold tabular-nums">Total {formatoMoneda(total)}</span>
          <Button
            onClick={() => {
              document.getElementById('pos-cobro')?.scrollIntoView({ block: 'start' });
              document.getElementById('pos-recibido')?.focus({ preventScroll: true });
            }}
          >
            Ir al cobro
          </Button>
        </div>
      ) : null}
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
