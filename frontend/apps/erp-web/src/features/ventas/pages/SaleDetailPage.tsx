import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Card, PageHeader } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { useEstablecimientos } from '../../organizacion';
import { ventaQuery } from '../api/ventas.api';
import { ComprobanteVenta } from '../components/ComprobanteVenta';
import { EstadoVentaBadge } from '../components/EstadoVentaBadge';
import { LineasVentaTable } from '../components/LineasVentaTable';

export function SaleDetailPage() {
  const ventaId = useRouteParam('ventaId');
  const establecimientos = useEstablecimientos();
  const result = useQuery(ventaQuery(ventaId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const venta = result.data;
  const nombreEstablecimiento =
    establecimientos.find(({ id }) => id === venta.establecimientoId)?.name ??
    venta.establecimientoId;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={`Venta ${venta.numeroOperacion}`}
        context={<Link to="/ventas">Ventas</Link>}
        description="Detalle de la venta, sus líneas y el pago."
      />

      <Card className="mt-6 p-6">
        <h2 className="mb-4 text-base font-semibold">Resumen</h2>
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoVentaBadge estado={venta.estado} />
          </DatoItem>
          <DatoItem label="Fecha">{formatoFechaHora(venta.fechaVenta)}</DatoItem>
          <DatoItem label="Establecimiento">{nombreEstablecimiento}</DatoItem>
          <DatoItem label="Vendedor">{venta.vendedorId}</DatoItem>
          <DatoItem label="Terminal">{venta.terminalId}</DatoItem>
          <DatoItem label="Total">{formatoMoneda(venta.total)}</DatoItem>
        </dl>
      </Card>

      <section className="mt-6">
        <h2 className="mb-3 text-base font-semibold">Líneas</h2>
        <LineasVentaTable lineas={venta.lineas} />
      </section>

      <Card className="mt-6 p-6">
        <h2 className="mb-4 text-base font-semibold">Pago</h2>
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          <DatoItem label="Medio de pago">{venta.pago.medioPago}</DatoItem>
          <DatoItem label="Monto">{formatoMoneda(venta.pago.monto)}</DatoItem>
          <DatoItem label="Recibido">{formatoMoneda(venta.pago.montoRecibido)}</DatoItem>
          <DatoItem label="Vuelto">{formatoMoneda(venta.pago.vuelto)}</DatoItem>
        </dl>
      </Card>

      {venta.anulacion ? (
        <Card className="mt-6 p-6">
          <h2 className="mb-4 text-base font-semibold">Anulación</h2>
          <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <DatoItem label="Motivo">{venta.anulacion.motivo}</DatoItem>
            <DatoItem label="Anulada el">{formatoFechaHora(venta.anulacion.anuladaAt)}</DatoItem>
            <DatoItem label="Anulada por">{venta.anulacion.anuladaPorId}</DatoItem>
          </dl>
        </Card>
      ) : null}

      <div className="mt-6">
        <ComprobanteVenta venta={venta} nombreEstablecimiento={nombreEstablecimiento} />
      </div>
    </div>
  );
}
