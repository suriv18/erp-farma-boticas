import { Button, Card } from '@boticas/ui-web';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { Venta } from '../api/ventas.types';

type ComprobanteVentaProps = {
  venta: Venta;
  nombreEstablecimiento: string;
};

type FilaTotalProps = { etiqueta: string; valor: number; negrita?: boolean };

function FilaTotal({ etiqueta, valor, negrita = false }: FilaTotalProps) {
  return (
    <div className={negrita ? 'flex justify-between font-semibold' : 'flex justify-between'}>
      <dt>{etiqueta}</dt>
      <dd>{formatoMoneda(valor)}</dd>
    </div>
  );
}

export function ComprobanteVenta({ venta, nombreEstablecimiento }: ComprobanteVentaProps) {
  return (
    <div className="space-y-4">
      <Card className="comprobante-imprimible mx-auto max-w-sm space-y-3 p-6 text-sm">
        <p className="text-center text-xs font-semibold uppercase">
          Comprobante interno — no válido como comprobante de pago
        </p>
        <div className="text-center">
          <p className="font-semibold">{nombreEstablecimiento}</p>
          <p>{venta.numeroOperacion}</p>
          <p>{formatoFechaHora(venta.fechaVenta)}</p>
          {venta.anulacion ? (
            <p className="font-semibold">ANULADA — {venta.anulacion.motivo}</p>
          ) : null}
        </div>
        <ul className="divide-y divide-neutral-200">
          {venta.lineas.map((linea) => (
            <li key={linea.numeroLinea} className="flex justify-between gap-2 py-2">
              <span>
                {linea.descripcion}
                <br />
                {linea.cantidad} × {formatoMoneda(linea.precioUnitario)}
              </span>
              <span>{formatoMoneda(linea.totalLinea)}</span>
            </li>
          ))}
        </ul>
        <dl className="space-y-1">
          <FilaTotal etiqueta="Subtotal" valor={venta.subtotal} />
          <FilaTotal etiqueta="IGV" valor={venta.impuestoTotal} />
          <FilaTotal etiqueta="Total" valor={venta.total} negrita />
          <FilaTotal etiqueta="Recibido" valor={venta.pago.montoRecibido} />
          <FilaTotal etiqueta="Vuelto" valor={venta.pago.vuelto} />
        </dl>
      </Card>
      <div className="flex justify-center">
        <Button variant="secondary" onClick={() => window.print()}>
          Imprimir comprobante
        </Button>
      </div>
    </div>
  );
}
