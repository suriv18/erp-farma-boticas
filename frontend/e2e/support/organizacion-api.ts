import type { Page } from '@playwright/test';
import {
  sampleAlmacen,
  sampleEmpresa,
  sampleEstablecimiento,
  sampleTerminal
} from '../../apps/erp-web/src/test/organizacion-fixtures';
import { json, login } from './login';

type Row = Record<string, unknown> & { id: string };

type Collection = {
  template: Row;
  uniqueKey: string;
  duplicateMessage: string;
  stateField: string;
  searchFields: string[];
  parentField?: string;
};

const collections: Record<string, Collection> = {
  empresas: {
    template: sampleEmpresa,
    uniqueKey: 'ruc',
    duplicateMessage: 'Ya existe una empresa con el RUC indicado.',
    stateField: 'estado',
    searchFields: ['ruc', 'razonSocial', 'nombreComercial']
  },
  establecimientos: {
    template: sampleEstablecimiento,
    uniqueKey: 'codigo',
    duplicateMessage: 'Ya existe un establecimiento con el código indicado.',
    stateField: 'estadoOperativo',
    searchFields: ['codigo', 'nombre'],
    parentField: 'empresaId'
  },
  almacenes: {
    template: sampleAlmacen,
    uniqueKey: 'codigo',
    duplicateMessage: 'Ya existe un almacén con el código indicado.',
    stateField: 'activo',
    searchFields: ['codigo', 'nombre'],
    parentField: 'establecimientoId'
  },
  'terminales-pos': {
    template: sampleTerminal,
    uniqueKey: 'codigo',
    duplicateMessage: 'Ya existe un terminal con el código indicado.',
    stateField: 'estado',
    searchFields: ['codigo', 'nombre'],
    parentField: 'establecimientoId'
  }
};

const matchesSearch = (row: Row, fields: string[], search: string) =>
  fields.some((field) =>
    String(row[field] ?? '')
      .toLowerCase()
      .includes(search.toLowerCase())
  );

export async function mockOrganizacionApi(page: Page) {
  const rows: Record<string, Row[]> = Object.fromEntries(
    Object.keys(collections).map((name) => [name, []])
  );

  await page.route('**/api/v1/organizacion/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const [name = '', id, action] = url.pathname.split('/').slice(4);
    const collection = collections[name];
    const items = rows[name];
    if (!collection || !items) return json(route, 404, { title: 'Not Found' });

    const notFound = () => json(route, 404, { title: 'Not Found', detail: 'No existe.' });
    const current = id ? items.find((row) => row.id === id) : undefined;
    const body = request.postData() ? (JSON.parse(request.postData() ?? '{}') as Row) : undefined;

    if (request.method() === 'GET' && !id) {
      const search = url.searchParams.get('search') ?? '';
      const parent = collection.parentField ? url.searchParams.get(collection.parentField) : null;
      const filtered = items
        .filter((row) => !parent || row[collection.parentField ?? ''] === parent)
        .filter((row) => !search || matchesSearch(row, collection.searchFields, search));
      return json(route, 200, {
        items: filtered,
        page: Number(url.searchParams.get('page') ?? 0),
        size: Number(url.searchParams.get('size') ?? 20),
        totalElements: filtered.length
      });
    }
    if (request.method() === 'GET') return current ? json(route, 200, current) : notFound();

    if (request.method() === 'POST' && body) {
      if (items.some((row) => row[collection.uniqueKey] === body[collection.uniqueKey])) {
        return json(route, 409, { title: 'Conflict', detail: collection.duplicateMessage });
      }
      const created: Row = {
        ...collection.template,
        ...body,
        id: crypto.randomUUID(),
        createdAt: new Date().toISOString(),
        updatedAt: null
      };
      items.push(created);
      return json(route, 201, created);
    }

    if (!current) return notFound();
    if (request.method() === 'PATCH' && action === 'estado' && body) {
      current[collection.stateField] = body.estado;
      return json(route, 200, current);
    }
    if (request.method() === 'PUT' && body) {
      Object.assign(current, body, { updatedAt: new Date().toISOString() });
      return json(route, 200, current);
    }
    return json(route, 405, { title: 'Method Not Allowed' });
  });
}

export async function abrirSesionEn(page: Page, path: string) {
  await mockOrganizacionApi(page);
  await login(page);
  await page.goto(path);
}
