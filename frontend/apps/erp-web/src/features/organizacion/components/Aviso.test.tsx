import { render, screen } from '@testing-library/react';
import { Aviso } from './Aviso';

describe('Aviso', () => {
  it('muestra el mensaje como estado accesible', () => {
    render(<Aviso>La empresa está suspendida.</Aviso>);

    expect(screen.getByRole('status')).toHaveTextContent('La empresa está suspendida.');
  });
});
