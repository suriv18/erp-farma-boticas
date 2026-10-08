import { render, screen } from '@testing-library/react';
import { formatoFechaHora } from '../../../shared/lib/format';
import { sampleTurno } from '../../../test/ventas-fixtures';
import { ResumenTurno } from './ResumenTurno';

describe('ResumenTurno', () => {
  it('muestra estado, fondo, apertura y cajero con el total del sistema pendiente', () => {
    render(<ResumenTurno turno={sampleTurno} />);

    expect(screen.getByText('ABIERTO')).toBeInTheDocument();
    expect(screen.getByText('S/ 100.00')).toBeInTheDocument();
    expect(screen.getByText(formatoFechaHora(sampleTurno.aperturaAt))).toBeInTheDocument();
    expect(screen.getByText('user-1')).toBeInTheDocument();
    expect(screen.getByText('Total del sistema').nextElementSibling).toHaveTextContent('—');
  });

  it('muestra el total del sistema cuando está disponible', () => {
    render(<ResumenTurno turno={{ ...sampleTurno, totalSistema: 350 }} />);

    expect(screen.getByText('Total del sistema').nextElementSibling).toHaveTextContent('S/ 350.00');
  });
});
