import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { AccionesPosicion } from './AccionesPosicion';

describe('AccionesPosicion', () => {
  it('enlaza al detalle del lote y dispara el ajuste', async () => {
    const onAjustar = vi.fn();
    render(
      <MemoryRouter>
        <AccionesPosicion loteId="lote-1" numeroLote="L001" onAjustar={onAjustar} />
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: 'Ver detalle del lote L001' })).toHaveAttribute(
      'href',
      '/inventario/lotes/lote-1'
    );
    await userEvent.click(screen.getByRole('button', { name: 'Ajustar stock del lote L001' }));

    expect(onAjustar).toHaveBeenCalledTimes(1);
  });
});
