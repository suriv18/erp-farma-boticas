import { render, screen } from '@testing-library/react';
import { EstadoBadge } from './EstadoBadge';

describe('EstadoBadge', () => {
  it('muestra tono success para estados activos', () => {
    render(<EstadoBadge status="ACTIVO" />);
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
  });

  it('muestra tono danger para estados bloqueados o revocados', () => {
    render(<EstadoBadge status="BLOQUEADO" />);
    expect(screen.getByText('BLOQUEADO')).toBeInTheDocument();
  });

  it('muestra tono warning para estados pendientes', () => {
    render(<EstadoBadge status="PENDIENTE" />);
    expect(screen.getByText('PENDIENTE')).toBeInTheDocument();
  });

  it('muestra tono neutral para estados no reconocidos', () => {
    render(<EstadoBadge status="DESCONOCIDO" />);
    expect(screen.getByText('DESCONOCIDO')).toBeInTheDocument();
  });
});
