import { render, screen } from '@testing-library/react';
import { EstadoOrdenBadge } from './EstadoOrdenBadge';

describe('EstadoOrdenBadge', () => {
  it('muestra la etiqueta del estado de la orden', () => {
    render(<EstadoOrdenBadge estado="PARCIALMENTE_RECIBIDA" />);

    expect(screen.getByText('Parcialmente recibida')).toBeInTheDocument();
  });
});
