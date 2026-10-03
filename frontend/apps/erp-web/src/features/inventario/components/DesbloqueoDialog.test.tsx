import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleLote } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { DesbloqueoDialog } from './DesbloqueoDialog';

const url = '*/api/v1/inventario/lotes/lote-1/bloqueos';

function renderDialog(onClose = vi.fn()) {
  const Dialog = () => <DesbloqueoDialog loteId="lote-1" onClose={onClose} />;
  return { onClose, ...renderRoute('/inventario', Dialog, '/inventario') };
}

describe('DesbloqueoDialog', () => {
  it('desbloquea el lote, cierra e invalida inventario', async () => {
    server.use(http.delete(url, () => HttpResponse.json(sampleLote)));
    const { onClose, user, queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('muestra el error del backend sin cerrar', async () => {
    server.use(
      http.delete(url, () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Un lote vencido no puede habilitarse.' },
          { status: 409 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Un lote vencido no puede habilitarse.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });
});
