import { useState } from 'react';
import { Printer, ReceiptText } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
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
  const [preview, setPreview] = useState(false);

  if (result.isPending) {
    return (
      <div className="erp-page">
        <p role="status" className="text-sm text-neutral-500 dark:text-neutral-400">
          Cargando…
        </p>
      </div>
    );
  }
  if (result.isError)
    return (
      <div className="erp-page space-y-4">
        <Link to="/ventas" className="text-primary-700 dark:text-primary-300 underline">
          Volver a Ventas
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

  const venta = result.data;
  const nombreEstablecimiento =
    establecimientos.find(({ id }) => id === venta.establecimientoId)?.name ??
    venta.establecimientoId;

  return (
    <div className="erp-page [&_dd]:[overflow-wrap:anywhere] [&_h1]:[overflow-wrap:anywhere]">
      <PageHeader
        title={`Venta ${venta.numeroOperacion}`}
        context={<Link to="/ventas">Ventas</Link>}
        description="Consulta los productos, importes y datos de la operación."
        actions={
          <>
            <Button
              variant="secondary"
              aria-expanded={preview}
              aria-controls="vista-comprobante"
              onClick={() => setPreview(!preview)}
            >
              <ReceiptText className="size-4" aria-hidden="true" />
              {preview ? 'Ocultar comprobante' : 'Ver comprobante'}
            </Button>
            <Button onClick={() => window.print()}>
              <Printer className="size-4" aria-hidden="true" />
              Imprimir comprobante
            </Button>
          </>
        }
      />

      <Card className="mt-6 p-4 sm:p-6">
        <h2 className="sr-only">Resumen</h2>
        <dl className="grid grid-cols-2 gap-5 xl:grid-cols-4">
          <DatoItem label="Total de la venta">
            <span className="text-3xl font-bold tracking-tight tabular-nums">
              {formatoMoneda(venta.total)}
            </span>
          </DatoItem>
          <DatoItem label="Estado">
            <EstadoVentaBadge estado={venta.estado} />
          </DatoItem>
          <DatoItem label="Fecha">{formatoFechaHora(venta.fechaVenta)}</DatoItem>
          <DatoItem label="Establecimiento">{nombreEstablecimiento}</DatoItem>
        </dl>
      </Card>

      {venta.anulacion ? (
        <Card className="border-danger-200 bg-danger-50 dark:border-danger-900 dark:bg-danger-950/30 mt-6 p-4 sm:p-6">
          <h2 className="mb-4 text-base font-semibold">Anulación</h2>
          <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <DatoItem label="Motivo">{venta.anulacion.motivo}</DatoItem>
            <DatoItem label="Anulada el">{formatoFechaHora(venta.anulacion.anuladaAt)}</DatoItem>
            <DatoItem label="Anulada por">{venta.anulacion.anuladaPorId}</DatoItem>
          </dl>
        </Card>
      ) : null}

      <div className="mt-6 grid items-start gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(20rem,1fr)]">
        <div className="min-w-0 space-y-6">
          <section aria-labelledby="productos-heading">
            <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
              <h2 id="productos-heading" className="text-lg font-semibold">
                Productos vendidos
              </h2>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">
                {venta.lineas.length}{' '}
                {venta.lineas.length === 1 ? 'línea de venta' : 'líneas de venta'}
              </p>
            </div>
            <LineasVentaTable lineas={venta.lineas} />
            <p className="mt-2 text-xs text-neutral-500 sm:hidden dark:text-neutral-400">Desliza la tabla para ver importes y lotes.</p>
          </section>
          <Card className="p-4 sm:p-6">
            <h2 className="mb-4 font-semibold">Datos de la operación</h2>
            <dl className="grid grid-cols-2 gap-5">
              <DatoItem label="Vendedor">{venta.vendedorId}</DatoItem>
              <DatoItem label="Terminal">{venta.terminalId}</DatoItem>
              <DatoItem label="Turno de caja">{venta.turnoId}</DatoItem>
              <DatoItem label="Moneda">{venta.moneda}</DatoItem>
            </dl>
          </Card>
        </div>
        <div className="min-w-0 space-y-6">
          <Card className="p-4 sm:p-6">
            <h2 className="mb-4 text-lg font-semibold">Importes de la venta</h2>
            <dl className="space-y-3 text-sm tabular-nums">
              <div className="flex justify-between gap-4">
                <dt>Subtotal</dt>
                <dd>{formatoMoneda(venta.subtotal)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt>Descuento</dt>
                <dd>{formatoMoneda(venta.descuentoTotal)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt>IGV</dt>
                <dd>{formatoMoneda(venta.impuestoTotal)}</dd>
              </div>
              <div className="flex justify-between gap-4 border-t border-neutral-200 pt-4 text-lg font-bold dark:border-neutral-700">
                <dt>Total</dt>
                <dd>{formatoMoneda(venta.total)}</dd>
              </div>
            </dl>
          </Card>
          <Card className="p-4 sm:p-6">
            <h2 className="mb-4 text-lg font-semibold">Pago</h2>
            <dl className="grid grid-cols-2 gap-5 tabular-nums">
              <DatoItem label="Medio de pago">{venta.pago.medioPago}</DatoItem>
              <DatoItem label="Monto">{formatoMoneda(venta.pago.monto)}</DatoItem>
              <DatoItem label="Recibido">{formatoMoneda(venta.pago.montoRecibido)}</DatoItem>
              <DatoItem label="Vuelto">
                <span className="font-semibold">{formatoMoneda(venta.pago.vuelto)}</span>
              </DatoItem>
            </dl>
          </Card>
        </div>
      </div>
      <section
        id="vista-comprobante"
        aria-label="Vista previa del comprobante"
        className={
          preview
            ? 'mt-6 rounded-2xl border border-neutral-200 bg-neutral-100 p-4 sm:p-6 dark:border-neutral-800 dark:bg-neutral-950 print:block'
            : 'hidden print:block'
        }
      >
        <ComprobanteVenta
          venta={venta}
          nombreEstablecimiento={nombreEstablecimiento}
          showPrintButton={false}
        />
      </section>
    </div>
  );
}
