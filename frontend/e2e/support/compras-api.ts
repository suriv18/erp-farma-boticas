import type { Page } from '@playwright/test';
import {
  sampleOrden,
  sampleOrdenResumen,
  sampleProveedor,
  sampleRecepcion
} from '../../apps/erp-web/src/test/compras-fixtures';
import { sampleSkuVenta } from '../../apps/erp-web/src/test/ventas-fixtures';
import { sampleEstructura } from '../../apps/erp-web/src/test/inventario-fixtures';
import type { Orden } from '../../apps/erp-web/src/features/compras/api/ordenes.types';
import type {
  Recepcion,
  RegistrarRecepcionPayload
} from '../../apps/erp-web/src/features/compras/api/recepciones.types';
import type { Proveedor } from '../../apps/erp-web/src/features/compras/api/proveedores.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

export type ComprasApiCapturas = { recepciones: RegistrarRecepcionPayload[] };

const recepcionDesdePayload = (payload: RegistrarRecepcionPayload, numero: number): Recepcion => ({
  ...sampleRecepcion,
  numero: `REC-2026-${String(numero).padStart(6, '0')}`,
  ordenCompraId: payload.ordenCompraId,
  almacenId: payload.almacenId,
  documentoProveedorTipo: payload.documentoProveedorTipo,
  documentoProveedorSerie: payload.documentoProveedorSerie ?? null,
  documentoProveedorNumero: payload.documentoProveedorNumero ?? null,
  lineas: payload.items.map((item, indice) => ({
    ...sampleRecepcion.lineas[0]!,
    id: `rec-linea-${indice + 1}`,
    numeroLinea: indice + 1,
    numeroLineaOrden: item.numeroLineaOrden,
    numeroLote: item.numeroLote,
    fechaVencimiento: item.fechaVencimiento,
    cantidadRecibida: item.cantidadRecibida,
    cantidadAceptada: item.cantidadRecibida - item.cantidadRechazada,
    cantidadRechazada: item.cantidadRechazada,
    costoUnitario: item.costoUnitario ?? null
  }))
});

export async function mockComprasApi(page: Page): Promise<ComprasApiCapturas> {
  const capturas: ComprasApiCapturas = { recepciones: [] };
  let proveedores: Proveedor[] = [sampleProveedor];
  let orden: Orden = sampleOrden;
  let recepciones: Recepcion[] = [];

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

  await page.route('**/api/v1/estructura-corporativa', (route) =>
    json(route, 200, sampleEstructura)
  );
  await page.route(/\/api\/v1\/compras\/ordenes(\?|$)/, (route) =>
    route.request().method() === 'POST'
      ? json(route, 201, sampleOrden)
      : json(
          route,
          200,
          pagina([{ ...sampleOrdenResumen, estado: orden.estado, total: orden.total }])
        )
  );
  await page.route(/\/api\/v1\/catalogo\/skus(\?|$)/, (route) =>
    json(route, 200, pagina([sampleSkuVenta]))
  );
  await page.route(/\/api\/v1\/catalogo\/skus\/sku-0001-aaaa$/, (route) =>
    json(route, 200, { id: 'sku-0001-aaaa', afectoIgv: true })
  );
  await page.route(
    /\/api\/v1\/compras\/ordenes\/orden-1(\/(aprobacion|emision|anulacion))?(\?|$)/,
    (route) => {
      const { pathname } = new URL(route.request().url());
      if (pathname.endsWith('/aprobacion')) orden = { ...orden, estado: 'APROBADA' };
      if (pathname.endsWith('/emision')) orden = { ...orden, estado: 'EMITIDA' };
      if (pathname.endsWith('/anulacion')) orden = { ...orden, estado: 'CANCELADA' };
      return json(route, 200, orden);
    }
  );

  await page.route(/\/api\/v1\/compras\/recepciones(\?|$)/, (route) => {
    const request = route.request();
    if (request.method() !== 'POST') return json(route, 200, pagina(recepciones));
    if (!request.headers()['idempotency-key']) {
      return json(route, 400, { title: 'Falta Idempotency-Key' });
    }
    const payload = JSON.parse(request.postData() ?? '{}') as RegistrarRecepcionPayload;
    capturas.recepciones.push(payload);
    const recepcion = recepcionDesdePayload(payload, recepciones.length + 1);
    recepciones = [...recepciones, recepcion];
    orden = {
      ...orden,
      estado: 'PARCIALMENTE_RECIBIDA',
      lineas: orden.lineas.map((linea) => {
        const recibida = recepcion.lineas
          .filter(({ numeroLineaOrden }) => numeroLineaOrden === linea.numeroLinea)
          .reduce(
            (total, { cantidadAceptada }) => total + cantidadAceptada,
            linea.cantidadRecibida
          );
        return {
          ...linea,
          cantidadRecibida: recibida,
          cantidadPendiente: Math.max(0, linea.cantidad - recibida)
        };
      })
    };
    return json(route, 201, recepcion);
  });

  return capturas;
}

export async function abrirComprasEn(page: Page, path: string): Promise<ComprasApiCapturas> {
  const capturas = await mockComprasApi(page);
  await login(page);
  await page.goto(path);
  return capturas;
}
