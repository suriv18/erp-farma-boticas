import { render, screen } from '@testing-library/react';
import { EstadoBadge } from './EstadoBadge';

describe('EstadoBadge', () => {
  it('muestra tono success para estados activos', () => {
    render(<EstadoBadge status="ACTIVO" />);
    const badge = screen.getByText('ACTIVO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-success-50', 'text-success-700');
  });

  it('muestra tono danger para estados bloqueados o revocados', () => {
    render(<EstadoBadge status="BLOQUEADO" />);
    const badge = screen.getByText('BLOQUEADO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-danger-50', 'text-danger-700');
  });

  it('muestra tono warning para estados pendientes', () => {
    render(<EstadoBadge status="PENDIENTE" />);
    const badge = screen.getByText('PENDIENTE');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-warning-50', 'text-warning-800');
  });

  it('muestra tono neutral para estados no reconocidos', () => {
    render(<EstadoBadge status="DESCONOCIDO" />);
    const badge = screen.getByText('DESCONOCIDO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-neutral-100', 'text-neutral-700');
  });
});
