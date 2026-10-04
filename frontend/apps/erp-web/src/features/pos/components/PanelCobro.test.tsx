import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { formatoMoneda } from '../../../shared/lib/format';
import { PanelCobro } from './PanelCobro';

type Props = Parameters<typeof PanelCobro>[0];

function renderPanel(props: Partial<Props> = {}) {
  const onRecibido = vi.fn();
  const onCobrar = vi.fn();
  render(
    <PanelCobro
      total={12.5}
      recibido="20"
      onRecibido={onRecibido}
      vuelto={7.5}
      puedeCobrar
      isSubmitting={false}
      error={null}
      onCobrar={onCobrar}
      {...props}
    />
  );
  return { onRecibido, onCobrar, user: userEvent.setup() };
}

describe('PanelCobro', () => {
  it('muestra el total y el vuelto formateados', () => {
    renderPanel();

    expect(screen.getByText('Total').nextElementSibling?.textContent).toBe(formatoMoneda(12.5));
    expect(screen.getByText('Vuelto').nextElementSibling?.textContent).toBe(formatoMoneda(7.5));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('muestra un guion cuando el vuelto no alcanza', () => {
    renderPanel({ vuelto: null });

    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('notifica el monto recibido escrito', async () => {
    const { onRecibido, user } = renderPanel({ recibido: '' });

    await user.type(screen.getByLabelText('Monto recibido'), '5');

    expect(onRecibido).toHaveBeenCalledWith('5');
  });

  it('cobra al pulsar Cobrar cuando es posible', async () => {
    const { onCobrar, user } = renderPanel();

    await user.click(screen.getByRole('button', { name: 'Cobrar' }));

    expect(onCobrar).toHaveBeenCalledTimes(1);
  });

  it('deshabilita Cobrar cuando no se puede cobrar', () => {
    renderPanel({ puedeCobrar: false });

    expect(screen.getByRole('button', { name: 'Cobrar' })).toBeDisabled();
  });

  it('muestra Cobrando… y deshabilita el botón mientras envía', () => {
    renderPanel({ isSubmitting: true });

    expect(screen.getByRole('button', { name: 'Cobrando…' })).toBeDisabled();
  });

  it('muestra el error en una alerta', () => {
    renderPanel({ error: 'Stock insuficiente.' });

    expect(screen.getByRole('alert')).toHaveTextContent('Stock insuficiente.');
  });
});
