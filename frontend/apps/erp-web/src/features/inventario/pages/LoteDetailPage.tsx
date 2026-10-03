import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, PageHeader } from '@boticas/ui-web';
import { DatoItem } from '../../../shared/components/DatoItem';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { valueOrDash, yesNo } from '../../../shared/lib/format';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { loteQuery } from '../api/lotes.api';
import { BloqueoDialog } from '../components/BloqueoDialog';
import { DesbloqueoDialog } from '../components/DesbloqueoDialog';
import { EstadoLoteBadge } from '../components/EstadoLoteBadge';
import { VencimientoCelda } from '../components/VencimientoCelda';
import { puedeBloquear, puedeDesbloquear } from '../lib/estado-lote';
import { formatearInstante } from '../lib/formato';

export function LoteDetailPage() {
  const loteId = useRouteParam('loteId');
  const [dialogo, setDialogo] = useState<'bloquear' | 'desbloquear' | null>(null);

  const result = useQuery(loteQuery(loteId));

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const lote = result.data;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={`Lote ${lote.numeroLote}`}
        context={<Link to="/inventario">Inventario</Link>}
        description="Detalle del lote y control de bloqueo."
        actions={
          <>
            {puedeBloquear(lote.estado) ? (
              <Button variant="secondary" onClick={() => setDialogo('bloquear')}>
                Bloquear lote
              </Button>
            ) : null}
            {puedeDesbloquear(lote.estado) ? (
              <Button variant="secondary" onClick={() => setDialogo('desbloquear')}>
                Desbloquear lote
              </Button>
            ) : null}
          </>
        }
      />

      <Card className="mt-6 p-6">
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoLoteBadge estado={lote.estado} />
          </DatoItem>
          <DatoItem label="Número de lote">{lote.numeroLote}</DatoItem>
          <DatoItem label="SKU">{lote.skuId}</DatoItem>
          <DatoItem label="Vencimiento">
            <VencimientoCelda fecha={lote.fechaVencimiento} />
          </DatoItem>
          <DatoItem label="Vendible">{yesNo(lote.vendible)}</DatoItem>
          <DatoItem label="Motivo del estado">{valueOrDash(lote.motivoEstado)}</DatoItem>
          <DatoItem label="Bloqueado el">{formatearInstante(lote.bloqueadoAt)}</DatoItem>
        </dl>
      </Card>

      {dialogo === 'bloquear' ? (
        <BloqueoDialog loteId={lote.id} onClose={() => setDialogo(null)} />
      ) : null}
      {dialogo === 'desbloquear' ? (
        <DesbloqueoDialog loteId={lote.id} onClose={() => setDialogo(null)} />
      ) : null}
    </div>
  );
}
