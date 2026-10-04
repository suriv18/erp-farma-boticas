import { screen } from '@testing-library/react';
import { renderRoute } from '../../../test/render-route';
import { AvisoTurno } from './AvisoTurno';

describe('AvisoTurno', () => {
  it('avisa que no hay turno y enlaza a Caja', () => {
    renderRoute('/pos', AvisoTurno, '/pos');

    expect(screen.getByText('No hay un turno abierto en esta terminal.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir a Caja' })).toHaveAttribute('href', '/caja');
  });
});
