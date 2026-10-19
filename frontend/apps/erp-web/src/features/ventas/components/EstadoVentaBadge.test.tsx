import { render, screen } from '@testing-library/react';
import { EstadoVentaBadge } from './EstadoVentaBadge';

describe('EstadoVentaBadge', () => {
  it('muestra el estado de la venta', () => {
    render(<EstadoVentaBadge estado="ANULADA" />);

    expect(screen.getByText('ANULADA')).toBeInTheDocument();
  });
});
