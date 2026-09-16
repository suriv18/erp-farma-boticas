import { render, screen } from '@testing-library/react';
import { EstadoBadge } from './EstadoBadge';

describe('EstadoBadge', () => {
  it('muestra tono success para estados activos', () => {
    render(<EstadoBadge status="ACTIVO" />);
    const badge = screen.getByText('ACTIVO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-emerald-50', 'text-emerald-700');
  });

  it('muestra tono danger para estados bloqueados o revocados', () => {
    render(<EstadoBadge status="BLOQUEADO" />);
    const badge = screen.getByText('BLOQUEADO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-rose-50', 'text-rose-700');
  });

  it('muestra tono warning para estados pendientes', () => {
    render(<EstadoBadge status="PENDIENTE" />);
    const badge = screen.getByText('PENDIENTE');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-amber-50', 'text-amber-800');
  });

  it('muestra tono neutral para estados no reconocidos', () => {
    render(<EstadoBadge status="DESCONOCIDO" />);
    const badge = screen.getByText('DESCONOCIDO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-slate-100', 'text-slate-700');
  });
});
