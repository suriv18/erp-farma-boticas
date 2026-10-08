import type { Page } from '@playwright/test';
import { samplePosicion } from '../../apps/erp-web/src/test/inventario-fixtures';
import { sampleSkuVenta, sampleVenta } from '../../apps/erp-web/src/test/ventas-fixtures';
import { mockCajaApi } from './caja-api';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

type CuerpoVenta = {
  lineas: { cantidad: number; precioUnitario: number }[];
  pago: { montoRecibido: number };
};

function ventaDesde({ lineas, pago }: CuerpoVenta) {
  const [linea] = lineas;
  const total = (linea?.cantidad ?? 0) * (linea?.precioUnitario ?? 0);
  return {
    ...sampleVenta,
    total,
    lineas: [{ ...sampleVenta.lineas[0], cantidad: linea?.cantidad, totalLinea: total }],
    pago: {
      ...sampleVenta.pago,
      monto: total,
      montoRecibido: pago.montoRecibido,
      vuelto: pago.montoRecibido - total
    }
  };
}

export async function mockPosApi(page: Page) {
  await mockCajaApi(page, { turnoAbierto: true });
  await page.route(/\/api\/v1\/catalogo\/skus(\?|$)/, (route) =>
    json(route, 200, pagina([sampleSkuVenta]))
  );
  await page.route(/\/api\/v1\/inventario\/posiciones(\?|$)/, (route) =>
    json(route, 200, pagina([samplePosicion]))
  );
  await page.route(/\/api\/v1\/ventas\/ventas(\?|$)/, (route) =>
    json(route, 201, ventaDesde(JSON.parse(route.request().postData() ?? '{}') as CuerpoVenta))
  );
}

export async function abrirPosEn(page: Page, path: string) {
  await mockPosApi(page);
  await login(page);
  await page.addInitScript(() => {
    localStorage.setItem(
      'erp.puesto-trabajo',
      JSON.stringify({ establecimientoId: 'est-1', terminalId: 'term-1', almacenId: 'alm-1' })
    );
  });
  await page.goto(path);
}
