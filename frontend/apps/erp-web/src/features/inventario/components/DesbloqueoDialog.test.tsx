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

  it('no cierra con el botón Cerrar mientras la solicitud está en curso', async () => {
    let liberar = () => undefined as void;
    const liberada = new Promise<void>((resolver) => {
      liberar = resolver;
    });
    server.use(
      http.delete(url, async () => {
        await liberada;
        return HttpResponse.json(sampleLote);
      })
    );
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).not.toHaveBeenCalled();
    liberar();
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
  });
});
