import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { sampleOrden, sampleProveedor, sampleRecepcion } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { formatoImporte } from '../lib/formato-compras';
import type { EstadoOrden } from '../api/ordenes.types';
import { OrdenDetailPage } from './OrdenDetailPage';

const ordenUrl = '*/api/v1/compras/ordenes/orden-1';
const importe = (valor: number) => formatoImporte(valor, 'PEN').replace(/\s/g, ' ');

function mockOrden(estado: EstadoOrden, overrides: object = {}) {
  server.use(
    http.get(ordenUrl, () => HttpResponse.json({ ...sampleOrden, estado, ...overrides })),
    http.get('*/api/v1/compras/proveedores/prov-1', () => HttpResponse.json(sampleProveedor)),
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/compras/recepciones', () => HttpResponse.json(pagina([])))
  );
}

function renderPage() {
  return renderRoute('/compras/ordenes/:ordenId', OrdenDetailPage, '/compras/ordenes/orden-1');
}

const botones = () =>
  ['Aprobar', 'Emitir', 'Anular'].filter((nombre) =>
    screen.queryByRole('button', { name: nombre })
  );

describe('OrdenDetailPage', () => {
  it('muestra el resumen, los totales y las líneas con el proveedor y el destino', async () => {
    mockOrden('BORRADOR');

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Orden OC-2026-000001' })
    ).toBeInTheDocument();
    expect(await screen.findByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByText('Botica Central')).toBeInTheDocument();
    expect(screen.getByText('Borrador')).toBeInTheDocument();
    expect(screen.getByText('CREDITO 30')).toBeInTheDocument();
    expect(screen.getByText('Reposición')).toBeInTheDocument();
    expect(screen.getAllByText(importe(64.9)).length).toBeGreaterThan(0);
    expect(screen.getByText('Paracetamol 500 mg')).toBeInTheDocument();
  });

  it('muestra guiones cuando faltan datos opcionales y el id si el destino no se conoce', async () => {
    mockOrden('BORRADOR', {
      condicionPago: null,
      observacion: null,
      aprobadoAt: null,
      establecimientoDestinoId: 'est-desconocido'
    });

    renderPage();

    expect(await screen.findByText('est-desconocido')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('muestra la fecha de aprobación cuando la orden fue aprobada', async () => {
    mockOrden('APROBADA', { aprobadoAt: '2026-10-04T15:00:00Z' });

    renderPage();

    expect(
      await screen.findByText(new Date('2026-10-04T15:00:00Z').toLocaleString('es-PE'))
    ).toBeInTheDocument();
  });

  it.each([
    ['BORRADOR', ['Aprobar', 'Anular']],
    ['EN_APROBACION', ['Aprobar', 'Anular']],
    ['APROBADA', ['Emitir', 'Anular']],
    ['EMITIDA', ['Anular']],
    ['PARCIALMENTE_RECIBIDA', []],
    ['CANCELADA', []]
  ] as const)('en estado %s ofrece %j', async (estado, esperados) => {
    mockOrden(estado);

    renderPage();
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    expect(botones()).toEqual(esperados);
  });

  it('muestra la carga y el error de consulta', async () => {
    server.use(
      http.get(ordenUrl, () => HttpResponse.json({ title: 'No encontrado' }, { status: 404 }))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it.each([
    ['Aprobar', 'aprobacion', 'BORRADOR'],
    ['Emitir', 'emision', 'APROBADA']
  ] as const)('%s la orden y refresca las consultas', async (boton, ruta, estado) => {
    let llamadas = 0;
    mockOrden(estado);
    server.use(
      http.post(`*/api/v1/compras/ordenes/orden-1/${ruta}`, () => {
        llamadas += 1;
        return HttpResponse.json({ ...sampleOrden, estado: 'EMITIDA' });
      })
    );
    const { user, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: boton }));

    await waitFor(() => expect(llamadas).toBe(1));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error traducido cuando la acción falla', async () => {
    mockOrden('BORRADOR');
    server.use(
      http.post('*/api/v1/compras/ordenes/orden-1/aprobacion', () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_ORDEN_ESTADO_INVALIDO', detail: 'x' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: 'Aprobar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
    );
  });

  it('abre el diálogo de anulación y lo cierra', async () => {
    mockOrden('EMITIDA');
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

    await user.click(screen.getByRole('button', { name: 'Anular' }));
    expect(screen.getByRole('heading', { name: 'Anular orden de compra' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(
      screen.queryByRole('heading', { name: 'Anular orden de compra' })
    ).not.toBeInTheDocument();
  });

  it.each(['EMITIDA', 'PARCIALMENTE_RECIBIDA'] as const)(
    'en estado %s ofrece registrar una recepción',
    async (estado) => {
      mockOrden(estado);

      renderPage();

      expect(await screen.findByRole('link', { name: 'Registrar recepción' })).toHaveAttribute(
        'href',
        '/compras/ordenes/orden-1/recepcion'
      );
    }
  );

  it.each(['BORRADOR', 'APROBADA', 'RECIBIDA', 'CANCELADA'] as const)(
    'en estado %s no ofrece registrar recepciones',
    async (estado) => {
      mockOrden(estado);

      renderPage();
      await screen.findByRole('heading', { name: 'Orden OC-2026-000001' });

      expect(screen.queryByRole('link', { name: 'Registrar recepción' })).not.toBeInTheDocument();
    }
  );

  it('lista las recepciones de la orden con su lote', async () => {
    mockOrden('PARCIALMENTE_RECIBIDA');
    server.use(
      http.get('*/api/v1/compras/recepciones', () => HttpResponse.json(pagina([sampleRecepcion])))
    );

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Recepciones' })).toBeInTheDocument();
    expect(await screen.findByText('REC-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('L2026-01')).toBeInTheDocument();
  });
});
