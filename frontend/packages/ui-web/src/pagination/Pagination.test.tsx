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
  it('muestra cero resultados con navegación deshabilitada', () => {
    render(<Pagination page={0} size={20} totalElements={0} onPageChange={vi.fn()} />);
    expect(screen.getByRole('status')).toHaveTextContent('0 resultados');
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Página 1' })).toBeDisabled();
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
  it('permite saltar a los extremos y páginas cercanas sin renderizar todas las páginas', async () => {
    const onPageChange = vi.fn();
    render(<Pagination page={7} size={20} totalElements={700} onPageChange={onPageChange} />);
    expect(screen.getByRole('button', { name: 'Página 8' })).toHaveAttribute(
      'aria-current',
      'page'
    );
    expect(screen.queryByRole('button', { name: 'Página 3' })).not.toBeInTheDocument();
    expect(screen.getAllByText('…')).toHaveLength(2);
    await userEvent.click(screen.getByRole('button', { name: 'Página 35' }));
    expect(onPageChange).toHaveBeenLastCalledWith(34);
    await userEvent.click(screen.getByRole('button', { name: 'Página 1' }));
    expect(onPageChange).toHaveBeenLastCalledWith(0);
    await userEvent.click(screen.getByRole('button', { name: 'Página 8' }));
    expect(onPageChange).toHaveBeenCalledTimes(2);
  });
  it('conserva el selector y el resumen cuando hay una sola página', () => {
    render(
      <Pagination
        page={0}
        size={20}
        totalElements={8}
        itemLabel="marcas"
        onPageChange={vi.fn()}
        onSizeChange={vi.fn()}
      />
    );
    expect(screen.getByRole('status')).toHaveTextContent('1–8 de 8 marcas');
    expect(screen.getByRole('combobox')).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Página 1' })).toHaveAttribute(
      'aria-current',
      'page'
    );
  });
  it('no anuncia cero resultados ni reinicia la página cuando llega un total provisional durante la carga', () => {
    const onPageChange = vi.fn();
    render(
      <Pagination page={3} size={20} totalElements={0} isLoading onPageChange={onPageChange} />
    );
    expect(screen.getByRole('status')).toHaveTextContent('Cargando resultados…');
    expect(screen.getByText('Página 4')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(onPageChange).not.toHaveBeenCalled();
  });
});
