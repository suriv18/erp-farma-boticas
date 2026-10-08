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
};

export function PanelCobro({
  total,
  recibido,
  onRecibido,
  vuelto,
  puedeCobrar,
  isSubmitting,
  error,
  onCobrar
}: PanelCobroProps) {
  return (
    <Card className="space-y-5 p-5 lg:sticky lg:top-6">
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
      <Button
        className="h-14 w-full text-base"
        disabled={!puedeCobrar || isSubmitting}
        onClick={onCobrar}
      >
        {isSubmitting ? 'Cobrando…' : 'Cobrar'}
      </Button>
    </Card>
  );
}
