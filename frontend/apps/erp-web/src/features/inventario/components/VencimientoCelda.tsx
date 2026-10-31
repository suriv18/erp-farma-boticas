import { Badge } from '@boticas/ui-web';
import { nivelVencimiento } from '../lib/vencimiento';

export function VencimientoCelda({ fecha }: { fecha: string }) {
  const nivel = nivelVencimiento(fecha, new Date());
  return (
    <span className="inline-flex flex-wrap items-center gap-2">
      {fecha}
      {nivel === 'vencido' ? <Badge tone="danger">Vencido</Badge> : null}
      {nivel === 'proximo' ? <Badge tone="warning">Por vencer</Badge> : null}
    </span>
  );
}
