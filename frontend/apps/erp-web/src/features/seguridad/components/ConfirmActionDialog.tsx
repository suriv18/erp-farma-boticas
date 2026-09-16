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
};

export function ConfirmActionDialog({
  open,
  title,
  description,
  confirmLabel,
  tone = 'default',
  isPending = false,
  onConfirm,
  onCancel
}: ConfirmActionDialogProps) {
  if (!open) return null;

  return (
    <Modal open={open} onClose={onCancel} title={title}>
      <p className="text-sm text-slate-600">{description}</p>
      <div className="mt-6 flex justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isPending}>
          Cancelar
        </Button>
        <Button
          type="button"
          variant={tone === 'danger' ? 'primary' : 'primary'}
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
