import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { Mail } from 'lucide-react';
import { Input } from './Input';

describe('Input', () => {
  it('renderiza un input de texto con las clases base', () => {
    render(<Input aria-label="Nombre" />);
    const input = screen.getByLabelText('Nombre');
    expect(input).toBeInTheDocument();
    expect(input.className).toContain('focus:border-primary-600');
    expect(input.className).toContain('rounded-xl');
  });

  it('acepta ref para integrarse con react-hook-form', () => {
    const ref = createRef<HTMLInputElement>();
    render(<Input aria-label="Correo" ref={ref} />);
    expect(ref.current).toBe(screen.getByLabelText('Correo'));
  });

  it('renderiza el icono cuando se provee y ajusta el padding izquierdo', () => {
    render(<Input aria-label="Correo" icon={Mail} />);
    const input = screen.getByLabelText('Correo');
    expect(input.className).toContain('pl-11');
    expect(document.querySelector('svg')).toBeInTheDocument();
  });

  it('no aplica padding izquierdo extra ni renderiza icono cuando no se provee', () => {
    render(<Input aria-label="Sin icono" />);
    const input = screen.getByLabelText('Sin icono');
    expect(input.className).not.toContain('pl-11');
    expect(document.querySelector('svg')).not.toBeInTheDocument();
  });

  it('reenvia props estandar como type, placeholder y aria-invalid', () => {
    render(<Input aria-label="Password" type="password" placeholder="••••" aria-invalid />);
    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('type', 'password');
    expect(input).toHaveAttribute('placeholder', '••••');
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('combina className adicional sin perder las clases base', () => {
    render(<Input aria-label="Custom" className="my-custom-class" />);
    const input = screen.getByLabelText('Custom');
    expect(input.className).toContain('my-custom-class');
    expect(input.className).toContain('rounded-xl');
  });
});
