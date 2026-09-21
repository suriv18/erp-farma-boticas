import { Button, Modal } from '@boticas/ui-web';

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
      <p className="text-sm text-neutral-600 dark:text-neutral-300">{description}</p>
      {errorMessage ? (
        <p role="alert" className="text-danger-700 dark:text-danger-400 mt-3 text-sm font-medium">
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
          className={
            tone === 'danger'
              ? 'bg-danger-700 hover:bg-danger-800 focus-visible:outline-danger-700'
              : undefined
          }
          onClick={onConfirm}
          disabled={isPending}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
