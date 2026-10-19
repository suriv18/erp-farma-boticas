import { render, screen } from '@testing-library/react';
import { VencimientoCelda } from './VencimientoCelda';

describe('VencimientoCelda', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 2, 12));
  });

  afterEach(() => vi.useRealTimers());

  it('marca como vencido una fecha pasada', () => {
    render(<VencimientoCelda fecha="2026-10-01" />);

    expect(screen.getByText('2026-10-01')).toBeInTheDocument();
    expect(screen.getByText('Vencido')).toBeInTheDocument();
  });

  it('marca como por vencer una fecha dentro del umbral', () => {
    render(<VencimientoCelda fecha="2026-12-31" />);

    expect(screen.getByText('Por vencer')).toBeInTheDocument();
  });

  it('no marca una fecha vigente', () => {
    render(<VencimientoCelda fecha="2030-01-01" />);

    expect(screen.getByText('2030-01-01')).toBeInTheDocument();
    expect(screen.queryByText('Vencido')).not.toBeInTheDocument();
    expect(screen.queryByText('Por vencer')).not.toBeInTheDocument();
  });
});
