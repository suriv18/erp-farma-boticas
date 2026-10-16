import { useEffect, useRef, type PropsWithChildren } from 'react';
import { Dialog } from 'radix-ui';
import { X } from 'lucide-react';
import { cn } from '../lib/cn';

export type ModalProps = PropsWithChildren<{
  open: boolean;
  onClose: () => void;
  title: string;
  size?: 'md' | 'lg' | 'drawer';
}>;

const sizes = { md: 'max-w-lg', lg: 'max-w-3xl', drawer: 'h-dvh max-w-72 rounded-none' } as const;

export function Modal({ open, onClose, title, size = 'md', children }: ModalProps) {
  const returnFocus = useRef<HTMLElement | null>(null);
  useEffect(() => {
    if (open) return;
    // Safari touch clicks do not necessarily focus the button that opens a dialog.
    const rememberTrigger = (event: Event) => {
      if (event.target instanceof Element) {
        returnFocus.current = event.target.closest<HTMLElement>(
          'button, a, input, select, [tabindex]'
        );
      }
    };
    document.addEventListener('pointerdown', rememberTrigger, true);
    document.addEventListener('focusin', rememberTrigger, true);
    return () => {
      document.removeEventListener('pointerdown', rememberTrigger, true);
      document.removeEventListener('focusin', rememberTrigger, true);
    };
  }, [open]);

  return (
    <Dialog.Root
      open={open}
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
    >
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-neutral-950/40" />
        <Dialog.Content
          aria-describedby={undefined}
          onOpenAutoFocus={() => {
            if (
              !returnFocus.current?.isConnected &&
              document.activeElement instanceof HTMLElement &&
              document.activeElement !== document.body
            ) {
              returnFocus.current = document.activeElement;
            }
          }}
          onCloseAutoFocus={(event) => {
            event.preventDefault();
            returnFocus.current?.focus();
          }}
          onPointerDownOutside={(event) => {
            if (size !== 'drawer') event.preventDefault();
          }}
          className={cn(
            'fixed z-50 max-h-[90vh] w-[calc(100%-2rem)] overflow-y-auto rounded-2xl bg-white p-6 shadow-xl dark:bg-neutral-900',
            size === 'drawer'
              ? 'inset-y-0 left-0 max-h-dvh'
              : 'top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2',
            sizes[size]
          )}
        >
          <div className="flex items-center justify-between">
            <Dialog.Title className="text-lg font-bold text-neutral-950 dark:text-white">
              {title}
            </Dialog.Title>
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
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
