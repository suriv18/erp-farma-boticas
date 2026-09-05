import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { Button } from './Button';

describe('Button', () => {
  it('renders an accessible button', () => {
    render(<Button>Guardar</Button>);
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeInTheDocument();
  });

  it('accepts ref as a prop with React 19', () => {
    const ref = createRef<HTMLButtonElement>();

    render(<Button ref={ref}>Guardar</Button>);

    expect(ref.current).toBe(screen.getByRole('button', { name: 'Guardar' }));
  });
});
