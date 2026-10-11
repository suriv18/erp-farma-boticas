import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { Select } from './Select';

describe('Select', () => {
  it('renderiza un select con sus opciones y las clases base', () => {
    render(
      <Select aria-label="Tipo">
        <option value="a">A</option>
        <option value="b">B</option>
      </Select>
    );
    const select = screen.getByLabelText('Tipo');
    expect(select.tagName).toBe('SELECT');
    expect(screen.getAllByRole('option')).toHaveLength(2);
    expect(select.className).toContain('rounded-xl');
    expect(select.className).toContain('focus:border-primary-600');
  });

  it('acepta ref para integrarse con react-hook-form', () => {
    const ref = createRef<HTMLSelectElement>();
    render(<Select aria-label="Perfil" ref={ref} />);
    expect(ref.current).toBe(screen.getByLabelText('Perfil'));
  });

  it('fusiona className adicional con las clases base', () => {
    render(<Select aria-label="Estado" className="w-40" />);
    const select = screen.getByLabelText('Estado');
    expect(select.className).toContain('w-40');
    expect(select.className).toContain('rounded-xl');
  });

  it('reenvía props estándar como disabled y aria-invalid', () => {
    render(<Select aria-label="Moneda" disabled aria-invalid />);
    const select = screen.getByLabelText('Moneda');
    expect(select).toBeDisabled();
    expect(select).toHaveAttribute('aria-invalid', 'true');
  });
});
