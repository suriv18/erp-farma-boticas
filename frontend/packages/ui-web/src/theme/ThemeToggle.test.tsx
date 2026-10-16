import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeToggle } from './ThemeToggle';

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.setAttribute('data-theme', 'light');
  });

  it('inicia mostrando el tema actual del documento', () => {
    document.documentElement.setAttribute('data-theme', 'dark');
    render(<ThemeToggle />);
    expect(screen.getByRole('button', { name: /modo claro/i })).toBeInTheDocument();
  });

  it('alterna a oscuro, actualiza el atributo del documento y persiste en localStorage', async () => {
    const user = userEvent.setup();
    render(<ThemeToggle />);

    await user.click(screen.getByRole('button', { name: /modo oscuro/i }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(screen.getByRole('button', { name: /modo claro/i })).toBeInTheDocument();
  });

  it('alterna de vuelta a claro en un segundo clic', async () => {
    const user = userEvent.setup();
    render(<ThemeToggle />);

    await user.click(screen.getByRole('button', { name: /modo oscuro/i }));
    await user.click(screen.getByRole('button', { name: /modo claro/i }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('theme')).toBe('light');
  });
});
