import { Button, Card } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { TextField } from '../../../shared/components/FormFields';
import { formatoMoneda } from '../../../shared/lib/format';

type PanelCobroProps = {
  total: number;
  recibido: string;
  onRecibido: (valor: string) => void;
  vuelto: number | null;
  puedeCobrar: boolean;
  isSubmitting: boolean;
  error: string | null;
  onCobrar: () => void;
  motivoBloqueo?: string | undefined;
};

export function PanelCobro({
  total,
  recibido,
  onRecibido,
  vuelto,
  puedeCobrar,
  isSubmitting,
  error,
  onCobrar,
  motivoBloqueo
}: PanelCobroProps) {
  return (
    <Card className="space-y-5 p-5">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-semibold">Cobro</h2>
        <span className="text-sm text-neutral-600 dark:text-neutral-300">Efectivo</span>
      </div>
      <dl>
        <dt className="text-xs font-semibold tracking-wide text-neutral-500 uppercase dark:text-neutral-400">
          Total
        </dt>
        <dd className="mt-1 text-4xl font-semibold tracking-tight text-neutral-950 tabular-nums dark:text-white">
          {formatoMoneda(total)}
        </dd>
      </dl>
      <div className="border-t-2 border-dashed border-neutral-200 dark:border-neutral-700" />
      <TextField
        id="pos-recibido"
        label="Monto recibido"
        inputMode="decimal"
        className="h-14 text-right text-2xl font-semibold tabular-nums"
        value={recibido}
        onChange={(event) => onRecibido(event.target.value)}
      />
      <dl className="bg-primary-50 text-primary-900 dark:bg-primary-900/30 dark:text-primary-100 flex items-baseline justify-between rounded-xl px-4 py-3">
        <dt className="text-sm font-semibold">Vuelto</dt>
        <dd className="text-2xl font-semibold tabular-nums">
          {vuelto === null ? '—' : formatoMoneda(vuelto)}
        </dd>
      </dl>
      {error ? <FormError message={error} /> : null}
      {!puedeCobrar && motivoBloqueo ? (
        <p
          id="pos-motivo-cobro"
          role="status"
          className="text-sm text-neutral-600 dark:text-neutral-300"
        >
          {motivoBloqueo}
        </p>
      ) : null}
      <Button
        className="h-14 w-full text-base"
        disabled={!puedeCobrar || isSubmitting}
        onClick={onCobrar}
        aria-describedby={!puedeCobrar && motivoBloqueo ? 'pos-motivo-cobro' : undefined}
      >
        {isSubmitting ? 'Cobrando…' : `Cobrar ${formatoMoneda(total)}`}
      </Button>
    </Card>
  );
}
