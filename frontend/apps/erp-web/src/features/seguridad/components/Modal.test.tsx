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
});
