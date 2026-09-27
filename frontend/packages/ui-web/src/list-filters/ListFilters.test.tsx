import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ListFilters } from './ListFilters';

it('asocia la etiqueta al buscador y permite filtros adicionales', async () => {
  const onValueChange = vi.fn();
  render(
    <ListFilters
      label="Buscar producto"
      placeholder="Código"
      value=""
      onValueChange={onValueChange}
    >
      <button>Estado</button>
    </ListFilters>
  );
  await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar producto' }), 'a');
  expect(onValueChange).toHaveBeenCalledWith('a');
  expect(screen.getByRole('button', { name: 'Estado' })).toBeInTheDocument();
});
