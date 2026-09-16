import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { cambiarEstadoRol, crearRol, fetchRoles, reemplazarPermisosRol } from './roles.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleRol = {
  id: 'rol-1',
  tenantId: 'tenant-1',
  code: 'ADMIN_LOCAL',
  name: 'Administrador local',
  description: null,
  roleType: 'ESTABLECIMIENTO',
  systemRole: false,
  permissionCodes: [],
  status: 'ACTIVO',
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: null
};

describe('roles.api', () => {
  it('fetchRoles consulta /roles con tenantId, search, page y size', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/roles', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleRol], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRoles(client, { tenantId: 'tenant-1', search: 'admin', page: 0, size: 20 });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('search')).toBe('admin');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleRol]);
  });

  it('crearRol envia el payload y devuelve el rol creado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/roles', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleRol, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearRol(client, {
      tenantId: 'tenant-1',
      code: 'ADMIN_LOCAL',
      name: 'Administrador local',
      roleType: 'ESTABLECIMIENTO'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      code: 'ADMIN_LOCAL',
      name: 'Administrador local',
      roleType: 'ESTABLECIMIENTO'
    });
    expect(result).toEqual(sampleRol);
  });

  it('cambiarEstadoRol envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.patch('http://localhost/api/v1/roles/rol-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoRol(client, 'rol-1', 'tenant-1', 'INACTIVO');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });

  it('reemplazarPermisosRol envia PUT con la lista de codigos', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/roles/rol-1/permissions', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleRol, permissionCodes: ['seguridad.usuarios.consultar'] });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await reemplazarPermisosRol(client, 'rol-1', ['seguridad.usuarios.consultar']);

    expect(receivedBody).toEqual({ permissionCodes: ['seguridad.usuarios.consultar'] });
    expect(result.permissionCodes).toEqual(['seguridad.usuarios.consultar']);
  });
});
