import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FiltrosVentas } from './FiltrosVentas';

type Props = Parameters<typeof FiltrosVentas>[0];

const filtros: Props['filtros'] = {
  establecimientoId: 'est-2',
  desde: '2026-09-01',
  hasta: '2026-09-30',
  page: 0,
  size: 20
};
const establecimientos = [
  { id: 'est-1', name: 'Botica Centro' },
  { id: 'est-2', name: 'Botica Norte' }
] as Props['establecimientos'];

function renderFiltros() {
  const handlers = { onEstablecimiento: vi.fn(), onDesde: vi.fn(), onHasta: vi.fn() };
  render(<FiltrosVentas establecimientos={establecimientos} filtros={filtros} {...handlers} />);
  return handlers;
}

describe('FiltrosVentas', () => {
  it('lista los establecimientos más Todos y refleja los filtros', () => {
    renderFiltros();

    const select = screen.getByLabelText('Establecimiento');
    expect(
      within(select)
        .getAllByRole('option')
        .map((option) => option.textContent)
    ).toEqual(['Todos', 'Botica Centro', 'Botica Norte']);
    expect(select).toHaveValue('est-2');
    expect(screen.getByLabelText('Desde')).toHaveValue('2026-09-01');
    expect(screen.getByLabelText('Hasta')).toHaveValue('2026-09-30');
  });

  it('notifica el establecimiento elegido', async () => {
    const { onEstablecimiento } = renderFiltros();

    await userEvent.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');

    expect(onEstablecimiento).toHaveBeenCalledWith('est-1');
  });

  it('notifica las fechas escritas', () => {
    const { onDesde, onHasta } = renderFiltros();

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-10-01' } });
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-10-03' } });

    expect(onDesde).toHaveBeenCalledWith('2026-10-01');
    expect(onHasta).toHaveBeenCalledWith('2026-10-03');
  });
});
