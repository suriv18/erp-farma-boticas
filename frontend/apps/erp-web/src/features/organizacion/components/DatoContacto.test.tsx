import { render, screen } from '@testing-library/react';
import { DatoContacto } from './DatoContacto';

describe('DatoContacto', () => {
  it('muestra el correo como enlace mailto', () => {
    render(<DatoContacto label="Correo" tipo="correo" valor="contacto@boticas.pe" />);

    expect(screen.getByRole('link', { name: 'contacto@boticas.pe' })).toHaveAttribute(
      'href',
      'mailto:contacto@boticas.pe'
    );
  });

  it('muestra el teléfono como enlace tel', () => {
    render(<DatoContacto label="Teléfono" tipo="telefono" valor="014445566" />);

    expect(screen.getByRole('link', { name: '014445566' })).toHaveAttribute(
      'href',
      'tel:014445566'
    );
  });

  it('abre el sitio web en una pestaña nueva de forma segura', () => {
    render(<DatoContacto label="Sitio web" tipo="web" valor="https://boticas.pe" />);

    const link = screen.getByRole('link', { name: 'https://boticas.pe' });
    expect(link).toHaveAttribute('href', 'https://boticas.pe');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('muestra texto plano cuando el valor no es enlazable', () => {
    render(<DatoContacto label="Sitio web" tipo="web" valor="x" />);

    expect(screen.getByText('x')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it.each([null, ''])('muestra un guion cuando el valor es %j', (valor) => {
    render(<DatoContacto label="Correo" tipo="correo" valor={valor} />);

    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
