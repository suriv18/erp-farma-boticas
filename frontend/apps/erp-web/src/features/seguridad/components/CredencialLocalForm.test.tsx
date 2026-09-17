import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CredencialLocalForm } from './CredencialLocalForm';
import { describe, it, expect, vi } from 'vitest';

describe('CredencialLocalForm', () => {
  it('rechaza una contraseña que no cumple la politica de complejidad', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña'), 'simple123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'simple123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(await screen.findByText('Debe incluir mayúscula, minúscula, número y símbolo.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('rechaza cuando la confirmacion no coincide', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Otra$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia password y requireChange cuando la contraseña es valida', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<CredencialLocalForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Contraseña'), 'Sup3r$eguro123');
    await user.type(screen.getByLabelText('Confirmar contraseña'), 'Sup3r$eguro123');
    await user.click(screen.getByRole('button', { name: 'Fijar contraseña' }));

    expect(onSubmit).toHaveBeenCalledWith({ password: 'Sup3r$eguro123', requireChange: true });
  });
});
