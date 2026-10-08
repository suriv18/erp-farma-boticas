import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { valueOrDash } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { useEstablecimientos } from '../../organizacion';
import { aprobarOrden, emitirOrden, ordenQuery } from '../api/ordenes.api';
import { proveedorQuery } from '../api/proveedores.api';
import { AnularOrdenDialog } from '../components/AnularOrdenDialog';
import { EstadoOrdenBadge } from '../components/EstadoOrdenBadge';
import { LineasOrdenTable } from '../components/LineasOrdenTable';
import { describeErrorCompras } from '../lib/errores-compras';
import { puedeAnular, puedeAprobar, puedeEmitir } from '../lib/estado-orden';
import { formatoFecha, formatoImporte } from '../lib/formato-compras';
import { useMutacionCompras } from '../lib/use-mutacion-compras';

export function OrdenDetailPage() {
  const ordenId = useRouteParam('ordenId');
  const establecimientos = useEstablecimientos();
  const [anulando, setAnulando] = useState(false);
  const result = useQuery(ordenQuery(ordenId));
  const proveedor = useQuery({
    ...proveedorQuery(result.data?.proveedorId ?? ''),
    enabled: result.data !== undefined
  });
  const aprobacion = useMutacionCompras(() => aprobarOrden(apiClient, ordenId));
  const emision = useMutacionCompras(() => emitirOrden(apiClient, ordenId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeErrorCompras(result.error)} />;

  const orden = result.data;
  const importe = (valor: number) => formatoImporte(valor, orden.moneda);
  const destino =
    establecimientos.find(({ id }) => id === orden.establecimientoDestinoId)?.name ??
    orden.establecimientoDestinoId;
  const mensajeError = aprobacion.mensajeError ?? emision.mensajeError;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={`Orden ${orden.numero}`}
        context={<Link to="/compras/ordenes">Compras / Órdenes</Link>}
        description="Detalle de la orden, sus líneas y lo recibido."
        actions={
          <>
            <EstadoOrdenBadge estado={orden.estado} />
            {puedeAprobar(orden.estado) ? (
              <Button disabled={aprobacion.isPending} onClick={() => aprobacion.mutate(undefined)}>
                Aprobar
              </Button>
            ) : null}
            {puedeEmitir(orden.estado) ? (
              <Button disabled={emision.isPending} onClick={() => emision.mutate(undefined)}>
                Emitir
              </Button>
            ) : null}
            {puedeAnular(orden.estado) ? (
              <Button variant="secondary" onClick={() => setAnulando(true)}>
                Anular
              </Button>
            ) : null}
          </>
        }
      />

      {mensajeError ? (
        <div className="mt-6">
          <FormError message={mensajeError} />
        </div>
      ) : null}

      <Card className="mt-6 p-6">
        <h2 className="mb-4 text-base font-semibold">Resumen</h2>
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Proveedor">{proveedor.data?.razonSocial ?? orden.proveedorId}</DatoItem>
          <DatoItem label="Destino">{destino}</DatoItem>
          <DatoItem label="Emisión">{formatoFecha(orden.fechaEmision)}</DatoItem>
          <DatoItem label="Entrega estimada">{formatoFecha(orden.fechaEntregaEstimada)}</DatoItem>
          <DatoItem label="Condición de pago">{valueOrDash(orden.condicionPago)}</DatoItem>
          <DatoItem label="Días de crédito">{orden.diasCredito}</DatoItem>
          <DatoItem label="Moneda">{orden.moneda}</DatoItem>
          <DatoItem label="Aprobada el">
            {orden.aprobadoAt === null ? '—' : new Date(orden.aprobadoAt).toLocaleString('es-PE')}
          </DatoItem>
          <DatoItem label="Observación">{valueOrDash(orden.observacion)}</DatoItem>
        </dl>
      </Card>

      <section className="mt-6">
        <h2 className="mb-3 text-base font-semibold">Líneas</h2>
        <LineasOrdenTable lineas={orden.lineas} moneda={orden.moneda} />
      </section>

      <Card className="mt-6 p-6">
        <h2 className="mb-4 text-base font-semibold">Totales</h2>
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <DatoItem label="Subtotal">{importe(orden.subtotal)}</DatoItem>
          <DatoItem label="Descuento">{importe(orden.descuentoTotal)}</DatoItem>
          <DatoItem label="Impuesto">{importe(orden.impuestoTotal)}</DatoItem>
          <DatoItem label="Total">{importe(orden.total)}</DatoItem>
        </dl>
      </Card>

      {anulando ? (
        <AnularOrdenDialog
          ordenId={ordenId}
          numero={orden.numero}
          onClose={() => setAnulando(false)}
        />
      ) : null}
    </div>
  );
}
