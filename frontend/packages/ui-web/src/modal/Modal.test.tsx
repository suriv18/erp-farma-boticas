import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

describe('Modal', () => {
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
