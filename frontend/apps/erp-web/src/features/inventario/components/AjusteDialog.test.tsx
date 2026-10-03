import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  sampleEstructura,
  sampleMovimiento,
  samplePosicion,
  sampleSku
} from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { AjusteDialog } from './AjusteDialog';

const movimientosUrl = '*/api/v1/inventario/movimientos';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([sampleSku])))
  );
});

function renderDialog(posicion: typeof samplePosicion | null, onClose = vi.fn()) {
  const Dialog = () => <AjusteDialog posicion={posicion} onClose={onClose} />;
  return { onClose, ...renderRoute('/inventario', Dialog, '/inventario') };
}

describe('AjusteDialog', () => {
  it('desde una fila registra el ajuste con Idempotency-Key, cierra e invalida inventario', async () => {
    let body: unknown;
    let key: string | null = null;
    server.use(
      http.post(movimientosUrl, async ({ request }) => {
        body = await request.json();
        key = request.headers.get('Idempotency-Key');
        return HttpResponse.json(sampleMovimiento, { status: 201 });
      })
    );
    const { onClose, user, queryClient } = renderDialog(samplePosicion);
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    expect(screen.getByRole('heading', { name: 'Ajustar stock del lote' })).toBeInTheDocument();
    await user.type(screen.getByLabelText('Cantidad'), '5');
    await user.type(screen.getByLabelText('Motivo'), 'Conteo');
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(body).toEqual({
      almacenId: 'alm-1',
      skuId: 'sku-0001-aaaa',
      tipo: 'AJUSTE_INGRESO',
      cantidad: 5,
      motivo: 'Conteo',
      loteId: 'lote-1'
    });
    expect(key).toMatch(/^[0-9a-f-]{36}$/);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['inventario'] });
  });

  it('sin fila ofrece los almacenes de la estructura corporativa', async () => {
    renderDialog(null);

    expect(
      screen.getByRole('heading', { name: 'Registrar ingreso con lote nuevo' })
    ).toBeInTheDocument();
    expect(
      await screen.findByRole('option', { name: 'Botica Norte — Almacén Norte' })
    ).toBeInTheDocument();
  });

  it('muestra el error del backend y no cierra el diálogo', async () => {
    server.use(
      http.post(movimientosUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'El estado del lote no admite ingresos de stock.' },
          { status: 409 }
        )
      )
    );
    const { onClose, user } = renderDialog(samplePosicion);

    await user.type(screen.getByLabelText('Cantidad'), '5');
    await user.type(screen.getByLabelText('Motivo'), 'Conteo');
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El estado del lote no admite ingresos de stock.'
    );
    expect(onClose).not.toHaveBeenCalled();
  });
});
