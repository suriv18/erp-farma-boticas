import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleLote } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { BloqueoDialog } from './BloqueoDialog';

const url = '*/api/v1/inventario/lotes/lote-1/bloqueos';

function renderDialog(onClose = vi.fn()) {
  const Dialog = () => <BloqueoDialog loteId="lote-1" onClose={onClose} />;
  return { onClose, ...renderRoute('/inventario', Dialog, '/inventario') };
}

describe('BloqueoDialog', () => {
  it('exige el motivo antes de enviar', async () => {
    const { user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));

    expect(await screen.findByText('El motivo es obligatorio.')).toBeInTheDocument();
  });

  it('bloquea el lote con el motivo, cierra e invalida inventario', async () => {
    let body: unknown;
    server.use(
      http.post(url, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleLote, estado: 'BLOQUEADO' });
      })
    );
    const { onClose, user, queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await user.type(screen.getByLabelText('Motivo'), 'Control de calidad');
    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(body).toEqual({ motivo: 'Control de calidad' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('muestra el error del backend sin cerrar', async () => {
    server.use(
      http.post(url, () =>
        HttpResponse.json(
          {
            title: 'Conflicto',
            detail: 'Solo un lote habilitado o en cuarentena puede bloquearse.'
          },
          { status: 409 }
        )
      )
    );
    const { onClose, user } = renderDialog();

    await user.type(screen.getByLabelText('Motivo'), 'Control');
    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Solo un lote habilitado o en cuarentena puede bloquearse.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it('no cierra con el botón Cerrar mientras la solicitud está en curso', async () => {
    let liberar = () => undefined as void;
    const liberada = new Promise<void>((resolver) => {
      liberar = resolver;
    });
    server.use(
      http.post(url, async () => {
        await liberada;
        return HttpResponse.json(sampleLote);
      })
    );
    const { onClose, user } = renderDialog();

    await user.type(screen.getByLabelText('Motivo'), 'Control');
    await user.click(screen.getByRole('button', { name: 'Bloquear lote' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).not.toHaveBeenCalled();
    liberar();
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
  });
});
