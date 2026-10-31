import { render, screen } from '@testing-library/react';
import { EstadoLoteBadge } from './EstadoLoteBadge';

describe('EstadoLoteBadge', () => {
  it('muestra el estado del lote', () => {
    render(<EstadoLoteBadge estado="CUARENTENA" />);

    expect(screen.getByText('CUARENTENA')).toBeInTheDocument();
  });
});
