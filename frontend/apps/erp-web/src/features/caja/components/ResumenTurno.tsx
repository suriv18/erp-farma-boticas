import { Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { Turno } from '../api/caja.types';

export function ResumenTurno({ turno }: { turno: Turno }) {
  return (
    <Card>
      <dl className="grid gap-4 sm:grid-cols-2">
        <DatoItem label="Estado">{turno.estado}</DatoItem>
        <DatoItem label="Fondo inicial">{formatoMoneda(turno.fondoInicial)}</DatoItem>
        <DatoItem label="Apertura">{formatoFechaHora(turno.aperturaAt)}</DatoItem>
        <DatoItem label="Cajero">{turno.cajeroId}</DatoItem>
        <DatoItem label="Total del sistema">
          {turno.totalSistema === null ? '—' : formatoMoneda(turno.totalSistema)}
        </DatoItem>
      </dl>
    </Card>
  );
}
