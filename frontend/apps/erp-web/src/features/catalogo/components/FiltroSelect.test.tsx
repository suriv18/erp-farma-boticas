import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FiltroSelect } from './FiltroSelect';

describe('FiltroSelect', () => {
  it('muestra la opcion vacia y las opciones, y notifica el valor elegido', async () => {
    const onValueChange = vi.fn();
    const user = userEvent.setup();
    render(
      <FiltroSelect
        id="filtro"
        label="Marca"
        value=""
        onValueChange={onValueChange}
        opciones={[
          { value: 'm-1', label: 'Bayer' },
          { value: 'm-2', label: 'Genfar' }
        ]}
        opcionVacia="Todas"
      />
    );

    expect(screen.getByRole('option', { name: 'Todas' })).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Marca'), 'm-2');

    expect(onValueChange).toHaveBeenCalledWith('m-2');
  });
});
