import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleLote } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import type { EstadoLote } from '../api/inventario.types';
import { LoteDetailPage } from './LoteDetailPage';

const loteUrl = '*/api/v1/inventario/lotes/lote-1';

function renderPage() {
  return renderRoute('/inventario/lotes/:loteId', LoteDetailPage, '/inventario/lotes/lote-1');
}

describe('LoteDetailPage', () => {
  it('muestra los datos del lote habilitado y permite bloquearlo', async () => {
    server.use(http.get(loteUrl, () => HttpResponse.json(sampleLote)));

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Lote L001' })).toBeInTheDocument();
    expect(screen.getByText('HABILITADO')).toBeInTheDocument();
    expect(screen.getByText('2099-01-01')).toBeInTheDocument();
    expect(screen.getByText('Sí')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Inventario' })).toHaveAttribute('href', '/inventario');
    expect(screen.getByRole('button', { name: 'Bloquear lote' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Desbloquear lote' })).not.toBeInTheDocument();
  });

  it.each<[EstadoLote, string | null]>([
    ['HABILITADO', 'Bloquear lote'],
    ['CUARENTENA', 'Bloquear lote'],
    ['BLOQUEADO', 'Desbloquear lote'],
    ['INMOVILIZADO_RECALL', null],
    ['VENCIDO', null],
    ['BAJA_DESTRUIDO', null]
  ])('en estado %s la acción disponible es %s', async (estado, accion) => {
    server.use(http.get(loteUrl, () => HttpResponse.json({ ...sampleLote, estado })));

    renderPage();

    await screen.findByRole('heading', { name: 'Lote L001' });
    ['Bloquear lote', 'Desbloquear lote'].forEach((nombre) => {
      const boton = screen.queryByRole('button', { name: nombre });
      if (nombre === accion) expect(boton).toBeInTheDocument();
      else expect(boton).not.toBeInTheDocument();
    });
  });

  it('bloquea el lote con motivo desde el diálogo', async () => {
    let body: unknown;
    server.use(
      http.get(loteUrl, () => HttpResponse.json(sampleLote)),
      http.post(`${loteUrl}/bloqueos`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleLote, estado: 'BLOQUEADO' });
      })
    );
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Bloquear lote' }));
    const dialogo = screen.getByRole('dialog');
    await user.type(within(dialogo).getByLabelText('Motivo'), 'Control de calidad');
    await user.click(within(dialogo).getByRole('button', { name: 'Bloquear lote' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(body).toEqual({ motivo: 'Control de calidad' });
  });

  it('muestra el motivo del bloqueo y desbloquea el lote', async () => {
    server.use(
      http.get(loteUrl, () =>
        HttpResponse.json({
          ...sampleLote,
          estado: 'BLOQUEADO',
          motivoEstado: 'Control de calidad',
          bloqueadoAt: '2026-10-02T15:30:00Z',
          vendible: false
        })
      ),
      http.delete(`${loteUrl}/bloqueos`, () => HttpResponse.json(sampleLote))
    );
    const { user } = renderPage();

    expect(await screen.findByText('Control de calidad')).toBeInTheDocument();
    expect(screen.getByText('No')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Desbloquear lote' }));
    const dialogo = screen.getByRole('dialog');
    await user.click(within(dialogo).getByRole('button', { name: 'Desbloquear lote' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('cierra el diálogo sin guardar', async () => {
    server.use(http.get(loteUrl, () => HttpResponse.json(sampleLote)));
    const { user } = renderPage();

    await user.click(await screen.findByRole('button', { name: 'Bloquear lote' }));
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el estado de carga y el error de la consulta', async () => {
    server.use(
      http.get(loteUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 }))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });
});
