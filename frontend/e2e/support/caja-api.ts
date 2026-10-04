import type { Page } from '@playwright/test';
import { sampleEstructura } from '../../apps/erp-web/src/test/inventario-fixtures';
import { sampleTurno, sampleTurnoCerrado } from '../../apps/erp-web/src/test/ventas-fixtures';
import type { Turno } from '../../apps/erp-web/src/features/caja/api/caja.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

const terminal = {
  id: 'term-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'T01',
  nombre: 'Caja 1',
  serieBoletaDefecto: null,
  serieFacturaDefecto: null,
  numeroSerieEquipo: null,
  hostname: null,
  ipEquipo: null,
  impresoraCodigo: null,
  storeEdgeHabilitado: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export async function mockCajaApi(page: Page) {
  let turno: Turno | null = null;

  await page.route('**/api/v1/estructura-corporativa', (route) =>
    json(route, 200, sampleEstructura)
  );
  await page.route(/\/api\/v1\/organizacion\/terminales-pos(\?|$)/, (route) =>
    json(route, 200, pagina([terminal]))
  );
  await page.route(/\/api\/v1\/ventas\/turnos(\/|\?|$)/, async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const body = request.postData()
      ? (JSON.parse(request.postData() ?? '{}') as Record<string, unknown>)
      : {};
    if (request.method() === 'POST' && pathname.endsWith('/cierre')) {
      const totalDeclarado = Number(body['totalDeclarado']);
      turno = null;
      return json(route, 200, {
        ...sampleTurnoCerrado,
        totalDeclarado,
        diferencia: totalDeclarado - (sampleTurnoCerrado.totalSistema ?? 0),
        observacionCierre: (body['observacion'] as string | undefined) ?? null
      });
    }
    if (request.method() === 'POST') {
      turno = { ...sampleTurno, fondoInicial: Number(body['fondoInicial']) };
      return json(route, 201, turno);
    }
    return turno
      ? json(route, 200, turno)
      : json(route, 404, { title: 'No encontrado', code: 'VEN_TURNO_NO_ENCONTRADO' });
  });
}

export async function abrirCajaEn(page: Page, path: string) {
  await mockCajaApi(page);
  await login(page);
  await page.goto(path);
}
