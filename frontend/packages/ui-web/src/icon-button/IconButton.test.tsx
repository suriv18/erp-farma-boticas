import { render, screen } from '@testing-library/react';
import { Pencil } from 'lucide-react';
import { IconButton, iconButtonClassName } from './IconButton';

describe('IconButton', () => {
  it('renders an accessible button labeled from the label prop', () => {
    render(<IconButton icon={Pencil} label="Editar Paracetamol" />);

    const button = screen.getByRole('button', { name: 'Editar Paracetamol' });
    expect(button).toBeInTheDocument();
    expect(button).toHaveAttribute('title', 'Editar Paracetamol');
  });

  it('renders the icon hidden from assistive tech', () => {
    render(<IconButton icon={Pencil} label="Editar Paracetamol" />);

    const icon = document.querySelector('svg');
    expect(icon).toHaveAttribute('aria-hidden', 'true');
  });

  it('applies the danger tone styling', () => {
    render(<IconButton icon={Pencil} label="Eliminar" tone="danger" />);

    expect(screen.getByRole('button', { name: 'Eliminar' }).className).toContain('danger');
  });

  it('forwards onClick and other button props', async () => {
    const onClick = vi.fn();
    render(<IconButton icon={Pencil} label="Editar" onClick={onClick} disabled />);

    const button = screen.getByRole('button', { name: 'Editar' });
    expect(button).toBeDisabled();
  });
});

describe('iconButtonClassName', () => {
  it('returns the default tone and size classes used to style a non-button element', () => {
    const className = iconButtonClassName();

    expect(className).toContain('size-8');
    expect(className).not.toContain('danger');
  });

  it('returns the danger tone and md size classes when requested', () => {
    const className = iconButtonClassName('danger', 'md');

    expect(className).toContain('danger');
    expect(className).toContain('size-9');
  });
});
