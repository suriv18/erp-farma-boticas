import { Card } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { TicketCabecera } from '../../../shared/components/TicketCabecera';
import { formatoFechaHora, formatoMoneda } from '../../../shared/lib/format';
import type { Turno } from '../api/caja.types';

export function ResumenTurno({ turno }: { turno: Turno }) {
  return (
    <Card className="overflow-hidden">
      <TicketCabecera
        titulo="Turno de caja"
        insignia={turno.estado}
        etiqueta="Total del sistema"
        valor={turno.totalSistema === null ? '—' : formatoMoneda(turno.totalSistema)}
      />
      <dl className="grid gap-5 p-5 sm:grid-cols-2">
        <DatoItem label="Fondo inicial">{formatoMoneda(turno.fondoInicial)}</DatoItem>
        <DatoItem label="Apertura">{formatoFechaHora(turno.aperturaAt)}</DatoItem>
        <DatoItem label="Cajero">
          <span className="font-mono text-xs break-all">{turno.cajeroId}</span>
        </DatoItem>
      </dl>
    </Card>
  );
}
