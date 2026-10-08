import type { Page } from '@playwright/test';
import { sampleProveedor } from '../../apps/erp-web/src/test/compras-fixtures';
import type { Proveedor } from '../../apps/erp-web/src/features/compras/api/proveedores.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

export async function mockComprasApi(page: Page) {
  let proveedores: Proveedor[] = [sampleProveedor];

  await page.route(/\/api\/v1\/compras\/proveedores(\?|$)/, async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      const body = JSON.parse(request.postData() ?? '{}') as Partial<Proveedor>;
      const nuevo = {
        ...sampleProveedor,
        ...body,
        id: `prov-${proveedores.length + 1}`,
        estado: 'ACTIVO'
      } as Proveedor;
      proveedores = [...proveedores, nuevo];
      return json(route, 201, nuevo);
    }
    return json(route, 200, pagina(proveedores));
  });

  await page.route(/\/api\/v1\/compras\/proveedores\/[^/?]+(\/estado)?(\?|$)/, async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const cambioEstado = pathname.endsWith('/estado');
    const id = pathname.split('/').at(cambioEstado ? -2 : -1);
    const actual = proveedores.find((proveedor) => proveedor.id === id);
    if (!actual) return json(route, 404, { title: 'No encontrado' });
    if (request.method() === 'GET') return json(route, 200, actual);
    const body = JSON.parse(request.postData() ?? '{}') as Partial<Proveedor>;
    const actualizado = { ...actual, ...body } as Proveedor;
    proveedores = proveedores.map((proveedor) => (proveedor.id === id ? actualizado : proveedor));
    return json(route, 200, actualizado);
  });
}

export async function abrirComprasEn(page: Page, path: string) {
  await mockComprasApi(page);
  await login(page);
  await page.goto(path);
}
