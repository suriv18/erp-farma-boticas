import type { Page } from '@playwright/test';
import {
  sampleEstructura,
  sampleLote,
  samplePosicion,
  sampleSku
} from '../../apps/erp-web/src/test/inventario-fixtures';
import type {
  Lote,
  Posicion
} from '../../apps/erp-web/src/features/inventario/api/inventario.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

export async function mockInventarioApi(page: Page) {
  const lotes = new Map<string, Lote>([
    [sampleLote.id, sampleLote],
    ['lote-2', { ...sampleLote, id: 'lote-2', numeroLote: 'L002' }]
  ]);
  const posiciones: Posicion[] = [
    samplePosicion,
    {
      ...samplePosicion,
      id: 'pos-2',
      almacenId: 'alm-3',
      establecimientoId: 'est-2',
      loteId: 'lote-2',
      numeroLote: 'L002'
    }
  ];

  await page.route('**/api/v1/estructura-corporativa', (route) =>
    json(route, 200, sampleEstructura)
  );
  await page.route(/\/api\/v1\/catalogo\/skus(\?|$)/, (route) =>
    json(route, 200, pagina([sampleSku]))
  );

  await page.route('**/api/v1/inventario/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const [recurso = '', id, accion] = url.pathname.split('/').slice(4);
    const body = request.postData()
      ? (JSON.parse(request.postData() ?? '{}') as Record<string, unknown>)
      : {};

    if (recurso === 'posiciones') {
      const almacenId = url.searchParams.get('almacenId');
      return json(
        route,
        200,
        pagina(posiciones.filter((posicion) => !almacenId || posicion.almacenId === almacenId))
      );
    }

    if (recurso === 'movimientos' && request.method() === 'POST') {
      const loteId = `lote-${posiciones.length + 1}`;
      lotes.set(loteId, { ...sampleLote, id: loteId, numeroLote: String(body['numeroLote']) });
      posiciones.push({
        ...samplePosicion,
        id: `pos-${posiciones.length + 1}`,
        almacenId: String(body['almacenId']),
        loteId,
        numeroLote: String(body['numeroLote']),
        cantidadFisica: Number(body['cantidad']),
        cantidadDisponible: Number(body['cantidad'])
      });
      return json(route, 201, {
        id: 'mov-1',
        posicionId: 'pos-x',
        loteId,
        tipo: body['tipo'],
        naturaleza: 'E',
        cantidad: body['cantidad'],
        stockAnterior: 0,
        stockPosterior: body['cantidad'],
        fechaNegocio: '2026-10-02T10:00:00Z'
      });
    }

    const lote = id ? lotes.get(id) : undefined;
    if (recurso !== 'lotes' || !lote) return json(route, 404, { title: 'Not Found' });

    if (accion === 'bloqueos') {
      const actualizado: Lote =
        request.method() === 'POST'
          ? { ...lote, estado: 'BLOQUEADO', motivoEstado: String(body['motivo']), vendible: false }
          : { ...lote, estado: 'HABILITADO', motivoEstado: null, vendible: true };
      lotes.set(lote.id, actualizado);
      return json(route, 200, actualizado);
    }

    return json(route, 200, lote);
  });
}

export async function abrirInventarioEn(page: Page, path: string) {
  await mockInventarioApi(page);
  await login(page);
  await page.goto(path);
}
