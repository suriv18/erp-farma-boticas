import { Button } from '@boticas/ui-web';
import { Modal } from './Modal';

export type ConfirmActionDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  tone?: 'default' | 'danger';
  isPending?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  errorMessage?: string | undefined;
};

export function ConfirmActionDialog({
  open,
  title,
  description,
  confirmLabel,
  tone = 'default',
  isPending = false,
  onConfirm,
  onCancel,
  errorMessage
}: ConfirmActionDialogProps) {
  if (!open) return null;

  return (
    <Modal open={open} onClose={onCancel} title={title}>
      <p className="text-sm text-slate-600">{description}</p>
      {errorMessage ? (
        <p role="alert" className="mt-3 text-sm font-medium text-rose-700">
          {errorMessage}
        </p>
      ) : null}
      <div className="mt-6 flex justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isPending}>
          Cancelar
        </Button>
        <Button
          type="button"
          variant="primary"
          className={tone === 'danger' ? 'bg-rose-700 hover:bg-rose-800 focus-visible:outline-rose-700' : undefined}
          onClick={onConfirm}
          disabled={isPending}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
