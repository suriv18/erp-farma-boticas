import type { Page } from '@playwright/test';
import { sampleMiPuesto } from '../../apps/erp-web/src/test/mi-puesto-fixtures';
import { sampleTurno, sampleTurnoCerrado } from '../../apps/erp-web/src/test/ventas-fixtures';
import type { Turno } from '../../apps/erp-web/src/features/caja/api/caja.types';
import { json, login } from './login';

export async function mockCajaApi(page: Page, opciones: { turnoAbierto?: boolean } = {}) {
  let turno: Turno | null = opciones.turnoAbierto ? sampleTurno : null;

  await page.route('**/api/v1/organizacion/mi-puesto', (route) =>
    json(route, 200, sampleMiPuesto)
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
