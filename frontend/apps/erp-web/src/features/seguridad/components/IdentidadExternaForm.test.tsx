import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { IdentidadExternaForm } from './IdentidadExternaForm';

describe('IdentidadExternaForm', () => {
  it('exige el nombre del proveedor cuando se elige Otro', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<IdentidadExternaForm onSubmit={onSubmit} onCancel={vi.fn()} submitLabel="Vincular" />);

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'OTRO');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'sub-123');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    expect(await screen.findByText('Indica el nombre del proveedor.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia provider, subject, issuer y emailClaim cuando el proveedor es conocido', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<IdentidadExternaForm onSubmit={onSubmit} onCancel={vi.fn()} submitLabel="Vincular" />);

    await user.selectOptions(screen.getByLabelText('Proveedor'), 'GOOGLE');
    await user.type(screen.getByLabelText('Identificador (subject)'), 'google-oauth2|123');
    await user.type(screen.getByLabelText('Emisor (opcional)'), 'https://accounts.google.com');
    await user.type(screen.getByLabelText('Correo asociado (opcional)'), 'ada@boticas.pe');
    await user.click(screen.getByRole('button', { name: 'Vincular' }));

    expect(onSubmit).toHaveBeenCalledWith({
      provider: 'GOOGLE',
      subject: 'google-oauth2|123',
      issuer: 'https://accounts.google.com',
      emailClaim: 'ada@boticas.pe'
    });
  });
});
