import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleOrden } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { AnularOrdenDialog } from './AnularOrdenDialog';

const anularUrl = '*/api/v1/compras/ordenes/orden-1/anulacion';

function renderDialog() {
  const onClose = vi.fn();
  const Pantalla = () => (
    <AnularOrdenDialog ordenId="orden-1" numero="OC-2026-000001" onClose={onClose} />
  );
  return { onClose, ...renderRoute('/x', Pantalla, '/x') };
}

describe('AnularOrdenDialog', () => {
  it('exige el motivo antes de anular', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }));

    expect(await screen.findByText('El motivo es obligatorio.')).toBeInTheDocument();
  });

  it('anula la orden con el motivo, invalida compras y se cierra', async () => {
    let body: unknown;
    server.use(
      http.post(anularUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleOrden, estado: 'CANCELADA' });
      })
    );
    const { user, onClose, queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    expect(screen.getByRole('heading', { name: 'Anular orden de compra' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('Motivo'), 'Error de digitación');
    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }));

    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    expect(body).toEqual({ motivo: 'Error de digitación' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error traducido y no se cierra', async () => {
    server.use(
      http.post(anularUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_ORDEN_ESTADO_INVALIDO', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user, onClose } = renderDialog();

    await user.type(screen.getByLabelText('Motivo'), 'Duplicada');
    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it('cierra con la X', async () => {
    const { onClose } = renderDialog();

    await userEvent.setup().click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
