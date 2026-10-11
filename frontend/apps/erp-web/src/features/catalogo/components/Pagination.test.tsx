import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Pagination } from './Pagination';

describe('Pagination', () => {
  it('muestra el rango de filas mostradas y el total', () => {
    render(<Pagination page={0} size={20} totalElements={45} onPageChange={vi.fn()} />);

    expect(screen.getByText('1–20 de 45')).toBeInTheDocument();
  });

  it('deshabilita Anterior en la primera pagina y Siguiente en la ultima', () => {
    render(<Pagination page={0} size={20} totalElements={20} onPageChange={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });

  it('llama a onPageChange con la pagina siguiente y anterior', async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    render(<Pagination page={1} size={20} totalElements={100} onPageChange={onPageChange} />);

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(onPageChange).toHaveBeenCalledWith(2);

    await user.click(screen.getByRole('button', { name: 'Anterior' }));
    expect(onPageChange).toHaveBeenCalledWith(0);
  });
});
