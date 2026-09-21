import type { PropsWithChildren } from 'react';
import { X } from 'lucide-react';

export type ModalProps = PropsWithChildren<{
  open: boolean;
  onClose: () => void;
  title: string;
}>;

export function Modal({ open, onClose, title, children }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-neutral-950/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl dark:bg-neutral-900"
      >
        <div className="flex items-center justify-between">
          <h2 id="modal-title" className="text-lg font-bold text-neutral-950 dark:text-white">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="grid size-8 place-items-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
          >
            <X className="size-4.5" aria-hidden="true" />
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}
