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
    <Card className="space-y-4 p-4">
      <dl className="grid grid-cols-2 gap-2">
        <dt>Total</dt>
        <dd>{formatoMoneda(total)}</dd>
        <dt>Vuelto</dt>
        <dd>{vuelto === null ? '—' : formatoMoneda(vuelto)}</dd>
      </dl>
      <TextField
        id="pos-recibido"
        label="Monto recibido"
        inputMode="decimal"
        value={recibido}
        onChange={(event) => onRecibido(event.target.value)}
      />
      {error ? <FormError message={error} /> : null}
      <Button disabled={!puedeCobrar || isSubmitting} onClick={onCobrar}>
        {isSubmitting ? 'Cobrando…' : 'Cobrar'}
      </Button>
    </Card>
  );
}
