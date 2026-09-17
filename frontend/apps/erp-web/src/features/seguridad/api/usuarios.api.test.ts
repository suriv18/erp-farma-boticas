import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  asignarRolUsuario,
  cambiarEstadoUsuario,
  crearUsuario,
  fetchUsuarios,
  revocarAsignacionRol,
  usuarioAsignacionesQuery
} from './usuarios.api';
import { QueryClient } from '@tanstack/react-query';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleUsuario = {
  id: 'user-1',
  tenantId: 'tenant-1',
  documentType: null,
  documentNumber: null,
  firstNames: 'Ada',
  lastNames: 'Lovelace',
  username: null,
  email: 'ada@boticas.pe',
  displayName: 'Ada Lovelace',
  phone: null,
  credentialChangeRequired: false,
  mfaRequired: false,
  status: 'ACTIVO',
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: null
};

describe('usuarios.api', () => {
  it('fetchUsuarios consulta /usuarios con tenantId, search, page y size', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleUsuario], page: 0, size: 20, totalElements: 1 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchUsuarios(client, { tenantId: 'tenant-1', search: 'ada', page: 0, size: 20 });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('search')).toBe('ada');
    expect(result.items).toEqual([sampleUsuario]);
  });

  it('crearUsuario envia el payload y devuelve el usuario creado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleUsuario, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await crearUsuario(client, {
      tenantId: 'tenant-1',
      email: 'ada@boticas.pe',
      displayName: 'Ada Lovelace'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      email: 'ada@boticas.pe',
      displayName: 'Ada Lovelace'
    });
    expect(result).toEqual(sampleUsuario);
  });

  it('cambiarEstadoUsuario envia PATCH con el nuevo estado', async () => {
    let receivedBody: unknown;
    let receivedUrl: URL | undefined;
    server.use(
      http.patch('http://localhost/api/v1/usuarios/user-1/estado', async ({ request }) => {
        receivedBody = await request.json();
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await cambiarEstadoUsuario(client, 'user-1', 'tenant-1', 'INACTIVO');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ status: 'INACTIVO' });
  });

  const sampleAsignacion = {
    id: 'assign-1',
    roleId: 'rol-1',
    roleCode: 'ADMIN_LOCAL',
    roleName: 'Administrador local',
    scopeType: 'ESTABLECIMIENTO',
    companyId: null,
    establishmentId: 'est-1',
    warehouseId: null,
    terminalId: null,
    validFrom: null,
    validUntil: null,
    status: 'ACTIVO',
    createdBy: 'user-admin',
    createdAt: '2026-09-01T00:00:00Z'
  };

  it('usuarioAsignacionesQuery consulta /usuarios/{userId}/asignaciones-rol con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/usuarios/user-1/asignaciones-rol', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json([sampleAsignacion]);
      })
    );

    const queryClient = new QueryClient();
    const result = await queryClient.fetchQuery(usuarioAsignacionesQuery('user-1', 'tenant-1'));

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual([sampleAsignacion]);
  });

  const sampleAsignacionCreada = {
    id: 'assign-1',
    tenantId: 'tenant-1',
    userId: 'user-1',
    roleId: 'rol-1',
    scopeType: 'ESTABLECIMIENTO',
    companyId: null,
    establishmentId: 'est-1',
    warehouseId: null,
    terminalId: null,
    validFrom: null,
    validUntil: null,
    status: 'ACTIVO',
    createdBy: 'admin-1',
    createdAt: '2026-09-01T00:00:00Z'
  };

  it('asignarRolUsuario envia POST con el payload de asignacion', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/usuarios/user-1/role-assignments', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleAsignacionCreada, { status: 201 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await asignarRolUsuario(client, 'user-1', {
      tenantId: 'tenant-1',
      roleId: 'rol-1',
      scopeType: 'ESTABLECIMIENTO',
      establishmentId: 'est-1'
    });

    expect(receivedBody).toEqual({
      tenantId: 'tenant-1',
      roleId: 'rol-1',
      scopeType: 'ESTABLECIMIENTO',
      establishmentId: 'est-1'
    });
    expect(result).toEqual(sampleAsignacionCreada);
  });

  it('revocarAsignacionRol envia DELETE con tenantId como query param', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.delete('http://localhost/api/v1/usuarios/user-1/asignaciones-rol/assign-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await revocarAsignacionRol(client, 'user-1', 'assign-1', 'tenant-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
  });
});
