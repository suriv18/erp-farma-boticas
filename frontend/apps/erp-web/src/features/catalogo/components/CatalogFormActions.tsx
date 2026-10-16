import { Button } from '@boticas/ui-web';
import { LoaderCircle } from 'lucide-react';
import { FormError } from '../../../shared/components/FormError';

export function CatalogFormActions({
  submitLabel,
  isSubmitting = false,
  error,
  onCancel,
  fullPage = false
}: {
  fullPage?: boolean;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
  onCancel?: (() => void) | undefined;
}) {
  return (
    <div
      className={`${fullPage ? 'bottom-0' : '-bottom-6'} sticky z-10 space-y-3 border-t border-neutral-200 bg-white py-4 pb-[max(1rem,env(safe-area-inset-bottom))] dark:border-neutral-800 dark:bg-neutral-900`}
    >
      {error ? <FormError message={error} /> : null}
      <div className="flex flex-wrap items-center justify-end gap-3">
        {onCancel ? (
          <Button variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </Button>
        ) : null}
        <Button type="submit" disabled={isSubmitting} aria-busy={isSubmitting}>
          {isSubmitting ? (
            <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
          ) : null}
          {isSubmitting ? 'Guardando…' : submitLabel}
        </Button>
      </div>
    </div>
  );
}
