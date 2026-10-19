import { render, screen, waitFor } from '@testing-library/react';
import { useState } from 'react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

describe('Modal', () => {
  it('contiene el foco, cierra con Escape y devuelve el foco al disparador', async () => {
    function Example() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <button onClick={() => setOpen(true)}>Abrir</button>
          <Modal open={open} onClose={() => setOpen(false)} title="Edición">
            <input aria-label="Nombre" />
            <button>Guardar</button>
          </Modal>
        </>
      );
    }
    const user = userEvent.setup();
    render(<Example />);
    await user.click(screen.getByRole('button', { name: 'Abrir' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Cerrar' })).toHaveFocus());
    await user.tab({ shift: true });
    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Cerrar' })).toHaveFocus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByRole('button', { name: 'Abrir' })).toHaveFocus());
  });
  it('no renderiza nada cuando open es false', () => {
    render(
      <Modal open={false} onClose={() => {}} title="Título">
        <p>Contenido</p>
      </Modal>
    );
    expect(screen.queryByText('Contenido')).not.toBeInTheDocument();
  });

  it('renderiza el título y el contenido cuando open es true', () => {
    render(
      <Modal open onClose={() => {}} title="Nuevo usuario">
        <p>Formulario</p>
      </Modal>
    );
    expect(screen.getByRole('heading', { name: 'Nuevo usuario' })).toBeInTheDocument();
    expect(screen.getByText('Formulario')).toBeInTheDocument();
  });

  it('llama a onClose al hacer clic en cerrar', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Nuevo usuario">
        <p>Formulario</p>
      </Modal>
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('usa el ancho md por defecto y permite scroll interno', () => {
    render(
      <Modal open onClose={() => {}} title="Tamaño">
        <p>Contenido</p>
      </Modal>
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog.className).toContain('max-w-lg');
    expect(dialog.className).not.toContain('max-w-3xl');
    expect(dialog.className).toContain('overflow-y-auto');
    expect(dialog.className).toContain('max-h-[90vh]');
  });

  it('usa el ancho lg cuando se solicita', () => {
    render(
      <Modal open onClose={() => {}} title="Tamaño grande" size="lg">
        <p>Contenido</p>
      </Modal>
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog.className).toContain('max-w-3xl');
    expect(dialog.className).not.toContain('max-w-lg');
  });
});
