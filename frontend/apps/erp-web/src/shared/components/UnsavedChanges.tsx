import { useCallback } from 'react';
import { useBeforeUnload, useBlocker } from 'react-router';
import { Button, Modal } from '@boticas/ui-web';

export function UnsavedChanges({ dirty }: { dirty: () => boolean }) {
  const blocker = useBlocker(dirty);
  useBeforeUnload(
    useCallback(
      (event) => {
        if (dirty()) {
          event.preventDefault();
          event.returnValue = '';
        }
      },
      [dirty]
    )
  );
  return (
    <Modal
      open={blocker.state === 'blocked'}
      onClose={() => blocker.state === 'blocked' && blocker.reset()}
      title="Cambios sin guardar"
    >
      <p>Si sales ahora, perderás los cambios de este formulario.</p>
      <div className="mt-4 flex flex-wrap justify-end gap-3">
        <Button variant="secondary" onClick={() => blocker.state === 'blocked' && blocker.reset()}>
          Seguir editando
        </Button>
        <Button onClick={() => blocker.state === 'blocked' && blocker.proceed()}>
          Descartar cambios
        </Button>
      </div>
    </Modal>
  );
}
