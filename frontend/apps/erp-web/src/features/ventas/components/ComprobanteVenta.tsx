import { Button } from '@boticas/ui-web';
import { Printer } from 'lucide-react';
import { formatoFechaHora } from '../../../shared/lib/format';
import type { Venta } from '../api/ventas.types';
import './comprobante-venta.css';

type ComprobanteVentaProps = {
  venta: Venta;
  nombreEstablecimiento: string;
  showPrintButton?: boolean;
};

type FilaTotalProps = { etiqueta: string; valor: string; negrita?: boolean };

function FilaTotal({ etiqueta, valor, negrita = false }: FilaTotalProps) {
  return (
    <div className={negrita ? 'ticket-total ticket-amount-row font-semibold' : 'ticket-amount-row'}>
      <dt>{etiqueta}</dt>
      <dd>{valor}</dd>
    </div>
  );
}

export function ComprobanteVenta({
  venta,
  nombreEstablecimiento,
  showPrintButton = true
}: ComprobanteVentaProps) {
  const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: venta.moneda });
  const precio = (valor: number) => moneda.format(valor);
  return (
    <div className="space-y-4">
      <article className="comprobante-imprimible" aria-label="Comprobante interno de la venta">
        <header className="ticket-header">
          {nombreEstablecimiento ? <p className="ticket-store">{nombreEstablecimiento}</p> : null}
          <p className="ticket-document">Comprobante interno</p>
        </header>
        <dl className="ticket-section ticket-metadata">
          <div>
            <dt>Operación</dt>
            <dd>{venta.numeroOperacion}</dd>
          </div>
          <div>
            <dt>Fecha y hora</dt>
            <dd>{formatoFechaHora(venta.fechaVenta)}</dd>
          </div>
        </dl>
        {venta.estado === 'ANULADA' || venta.anulacion ? (
          <div className="ticket-cancelled">
            <p className="font-bold">
              ANULADA{venta.anulacion ? ` — ${venta.anulacion.motivo}` : ''}
            </p>
            {venta.anulacion ? <p>{formatoFechaHora(venta.anulacion.anuladaAt)}</p> : null}
          </div>
        ) : null}
        <div className="ticket-section ticket-items-header" aria-hidden="true">
          <span>Producto / cantidad</span>
          <span>Importe</span>
        </div>
        <ul className="ticket-items" aria-label="Productos del comprobante">
          {venta.lineas.map((linea) => (
            <li key={linea.numeroLinea}>
              <p className="ticket-product">{linea.descripcion}</p>
              <div className="ticket-amount-row">
                <span>
                  {linea.cantidad} × {precio(linea.precioUnitario)} · {linea.unidadVentaCodigo}
                </span>
                <span className="font-semibold">{precio(linea.totalLinea)}</span>
              </div>
            </li>
          ))}
        </ul>
        <dl className="ticket-section ticket-totals">
          <FilaTotal etiqueta="Subtotal" valor={precio(venta.subtotal)} />
          {venta.descuentoTotal > 0 ? (
            <FilaTotal etiqueta="Descuento" valor={precio(venta.descuentoTotal)} />
          ) : null}
          <FilaTotal etiqueta="IGV" valor={precio(venta.impuestoTotal)} />
          <FilaTotal etiqueta="Total" valor={precio(venta.total)} negrita />
        </dl>
        <dl className="ticket-section ticket-totals">
          <div className="ticket-amount-row">
            <dt>Medio de pago</dt>
            <dd>{venta.pago.medioPago === 'EFECTIVO' ? 'Efectivo' : venta.pago.medioPago}</dd>
          </div>
          <FilaTotal etiqueta="Recibido" valor={precio(venta.pago.montoRecibido)} />
          <FilaTotal etiqueta="Vuelto" valor={precio(venta.pago.vuelto)} />
        </dl>
        <footer className="ticket-section ticket-footer">
          <p>Gracias por su compra</p>
          <p className="ticket-disclaimer">
            Comprobante interno — no válido como comprobante de pago
          </p>
        </footer>
      </article>
      {showPrintButton ? (
        <div className="flex justify-center">
          <Button variant="secondary" onClick={() => window.print()}>
            <Printer className="size-4" aria-hidden="true" />
            Imprimir comprobante
          </Button>
        </div>
      ) : null}
    </div>
  );
}
