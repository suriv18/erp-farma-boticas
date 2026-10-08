import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { Button, buttonClassName } from './Button';

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

describe('buttonClassName', () => {
  it('devuelve las clases del boton primario por defecto', () => {
    const className = buttonClassName();
    expect(className).toContain('bg-primary-600');
    expect(className).toContain('h-11');
  });

  it('devuelve las clases de la variante y tamano solicitados', () => {
    const className = buttonClassName('secondary', 'sm');
    expect(className).toContain('border-neutral-200');
    expect(className).toContain('h-9');
  });

  it('produce las mismas clases base que usa el componente Button', () => {
    render(<a className={buttonClassName()} href="/destino">Ir</a>);
    const link = screen.getByRole('link', { name: 'Ir' });
    expect(link.className).toContain('rounded-xl');
    expect(link.className).toContain('font-semibold');
  });
});
