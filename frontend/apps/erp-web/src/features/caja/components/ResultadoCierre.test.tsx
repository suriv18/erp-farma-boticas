import { render, screen } from '@testing-library/react';
import { sampleTurnoCerrado } from '../../../test/ventas-fixtures';
import { ResultadoCierre } from './ResultadoCierre';

const valorDe = (etiqueta: string) => screen.getByText(etiqueta).nextElementSibling;

describe('ResultadoCierre', () => {
  it('muestra los totales, la diferencia y la observación del cierre', () => {
    render(<ResultadoCierre turno={sampleTurnoCerrado} />);

    expect(screen.getByText('Turno cerrado')).toBeInTheDocument();
    expect(valorDe('Total del sistema')).toHaveTextContent('S/ 350.00');
    expect(valorDe('Total declarado')).toHaveTextContent('S/ 348.50');
    expect(valorDe('Diferencia')).toHaveTextContent('-S/ 1.50');
    expect(valorDe('Observación')).toHaveTextContent('Faltante de monedas');
  });

  it('muestra cero cuando no hay diferencia', () => {
    render(<ResultadoCierre turno={{ ...sampleTurnoCerrado, diferencia: 0 }} />);

    expect(valorDe('Diferencia')).toHaveTextContent('S/ 0.00');
  });

  it('muestra guiones cuando el turno no trae totales ni observación', () => {
    render(
      <ResultadoCierre
        turno={{
          ...sampleTurnoCerrado,
          totalSistema: null,
          totalDeclarado: null,
          diferencia: null,
          observacionCierre: null
        }}
      />
    );

    expect(valorDe('Total del sistema')).toHaveTextContent('—');
    expect(valorDe('Total declarado')).toHaveTextContent('—');
    expect(valorDe('Diferencia')).toHaveTextContent('—');
    expect(valorDe('Observación')).toHaveTextContent('—');
  });
});
