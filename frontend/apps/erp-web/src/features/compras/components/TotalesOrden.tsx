import { Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { formatoImporte } from '../lib/formato-compras';
import type { TotalesOrden as Totales } from '../lib/orden-calculo';

type TotalesOrdenProps = { totales: Totales; moneda: string };

export function TotalesOrden({ totales, moneda }: TotalesOrdenProps) {
  return (
    <Card className="p-6">
      <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <DatoItem label="Subtotal">{formatoImporte(totales.subtotal, moneda)}</DatoItem>
        <DatoItem label="Descuento">{formatoImporte(totales.descuento, moneda)}</DatoItem>
        <DatoItem label="Impuesto">{formatoImporte(totales.impuesto, moneda)}</DatoItem>
        <DatoItem label="Total">{formatoImporte(totales.total, moneda)}</DatoItem>
      </dl>
    </Card>
  );
}
