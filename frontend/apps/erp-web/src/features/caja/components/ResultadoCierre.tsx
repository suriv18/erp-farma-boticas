import { Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { formatoMoneda, valueOrDash } from '../../../shared/lib/format';
import type { Turno } from '../api/caja.types';

const monedaODash = (valor: number | null) => (valor === null ? '—' : formatoMoneda(valor));

export function ResultadoCierre({ turno }: { turno: Turno }) {
  return (
    <Card>
      <h2 className="mb-4 text-lg font-semibold text-neutral-900 dark:text-neutral-100">
        Turno cerrado
      </h2>
      <dl className="grid gap-4 sm:grid-cols-2">
        <DatoItem label="Total del sistema">{monedaODash(turno.totalSistema)}</DatoItem>
        <DatoItem label="Total declarado">{monedaODash(turno.totalDeclarado)}</DatoItem>
        <DatoItem label="Diferencia">{monedaODash(turno.diferencia)}</DatoItem>
        <DatoItem label="Observación">{valueOrDash(turno.observacionCierre)}</DatoItem>
      </dl>
    </Card>
  );
}
