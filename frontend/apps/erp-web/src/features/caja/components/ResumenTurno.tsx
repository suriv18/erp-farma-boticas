import { Badge, Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { Turno } from '../api/caja.types';

export function ResumenTurno({ turno }: { turno: Turno }) {
  return (
    <Card className="space-y-6 p-6">
      <dl>
        <dt className="text-xs font-semibold tracking-wide text-neutral-500 uppercase dark:text-neutral-400">
          Fondo inicial
        </dt>
        <dd className="mt-1 text-4xl font-semibold tracking-tight text-neutral-950 tabular-nums dark:text-white">
          {formatoMoneda(turno.fondoInicial)}
        </dd>
      </dl>
      <div className="border-t-2 border-dashed border-neutral-200 dark:border-neutral-700" />
      <dl className="grid gap-5 sm:grid-cols-2">
        <DatoItem label="Estado">
          <Badge tone="success">{turno.estado}</Badge>
        </DatoItem>
        <DatoItem label="Apertura">{formatoFechaHora(turno.aperturaAt)}</DatoItem>
        <DatoItem label="Total del sistema">
          {turno.totalSistema === null ? '—' : formatoMoneda(turno.totalSistema)}
        </DatoItem>
        <DatoItem label="Cajero">
          <span className="font-mono text-xs break-all">{turno.cajeroId}</span>
        </DatoItem>
      </dl>
    </Card>
  );
}
