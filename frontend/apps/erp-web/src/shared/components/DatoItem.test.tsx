import { render, screen } from '@testing-library/react';
import { DatoItem } from './DatoItem';

describe('DatoItem', () => {
  it('muestra la etiqueta y el valor', () => {
    render(
      <dl>
        <DatoItem label="RUC">20123456786</DatoItem>
      </dl>
    );

    expect(screen.getByText('RUC')).toBeInTheDocument();
    expect(screen.getByText('20123456786')).toBeInTheDocument();
  });
});
