import { render, screen } from '@testing-library/react';
import { TicketCabecera } from './TicketCabecera';

describe('TicketCabecera', () => {
  it('muestra el título, la insignia y el valor destacado', () => {
    render(<TicketCabecera titulo="Cobro" insignia="Efectivo" etiqueta="Total" valor="S/ 12.50" />);

    expect(screen.getByRole('heading', { name: 'Cobro' })).toBeInTheDocument();
    expect(screen.getByText('Efectivo')).toBeInTheDocument();
    expect(screen.getByText('Total').nextElementSibling).toHaveTextContent('S/ 12.50');
  });
});
