import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Pagination } from './Pagination';

describe('Pagination', () => {
  it('corrige la página cuando el total disminuye, pero espera durante la carga', () => {
    const onPageChange = vi.fn();
    const { rerender } = render(
      <Pagination page={4} size={20} totalElements={25} onPageChange={onPageChange} disabled />
    );
    expect(onPageChange).not.toHaveBeenCalled();
    rerender(<Pagination page={4} size={20} totalElements={25} onPageChange={onPageChange} />);
    expect(onPageChange).toHaveBeenCalledWith(1);
    expect(screen.getByRole('status')).toHaveTextContent('21–25 de 25');
  });
  it('navega con índices de API y muestra páginas humanas', async () => {
    const onPageChange = vi.fn();
    render(<Pagination page={1} size={20} totalElements={45} onPageChange={onPageChange} />);
    expect(screen.getByRole('status')).toHaveTextContent('21–40 de 45');
    expect(screen.getByText('Página 2 de 3')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Anterior' }));
    expect(onPageChange).toHaveBeenLastCalledWith(0);
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(onPageChange).toHaveBeenLastCalledWith(2);
  });
  it('muestra el rango vacío y bloquea ambos límites', () => {
    render(<Pagination page={0} size={20} totalElements={0} onPageChange={vi.fn()} />);
    expect(screen.getByRole('status')).toHaveTextContent('0–0 de 0');
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });
  it('cambia el tamaño y vuelve al inicio, admite tamaños del consumidor', async () => {
    const onSizeChange = vi.fn();
    const onPageChange = vi.fn();
    render(
      <Pagination
        page={2}
        size={5}
        totalElements={100}
        onPageChange={onPageChange}
        onSizeChange={onSizeChange}
      />
    );
    expect(screen.getByRole('combobox')).toHaveValue('5');
    await userEvent.selectOptions(screen.getByRole('combobox'), '50');
    expect(onSizeChange).toHaveBeenCalledWith(50);
    expect(onPageChange).toHaveBeenCalledWith(0);
  });
  it('bloquea los controles mientras no se puede navegar', () => {
    render(
      <Pagination
        page={1}
        size={20}
        totalElements={100}
        onPageChange={vi.fn()}
        onSizeChange={vi.fn()}
        disabled
      />
    );
    expect(screen.getByRole('combobox')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });
});
