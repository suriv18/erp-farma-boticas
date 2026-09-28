import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext, type AuthSession } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { NewUserPage } from './NewUserPage';

const authenticatedSession: AuthSession = {
  status: 'authenticated',
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const sampleUsuarioCreado = {
  id: 'user-nuevo',
  tenantId: 'tenant-1',
  documentType: '1',
  documentNumber: '45678912',
  firstNames: null,
  lastNames: null,
  username: null,
  email: 'ada@boticas.pe',
  displayName: null,
  phone: null,
  credentialChangeRequired: false,
  mfaRequired: false,
  status: 'ACTIVO',
  createdAt: '2026-09-27T00:00:00Z',
  updatedAt: null
};

const sampleRolesPage = {
  items: [
    {
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
    }
  ],
  page: 0,
  size: 100,
  totalElements: 1
};

function renderPage(session: AuthSession = authenticatedSession) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([
    { path: '/seguridad/usuarios/nuevo', Component: NewUserPage },
    { path: '/seguridad/usuarios/:userId', Component: () => <h1>Detalle de usuario</h1> }
  ], { initialEntries: ['/seguridad/usuarios/nuevo'] });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={session}>
          <RouterProvider router={router} />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

async function fillRequiredUserFields(user: ReturnType<typeof userEvent.setup>) {
  await screen.findByRole('option', { name: 'DNI' });
  await user.type(screen.getByLabelText('Número de documento'), '45678912');
  await user.type(screen.getByLabelText('Correo'), 'ada@boticas.pe');
}

describe('NewUserPage', () => {
  it('crea el usuario sin rol cuando el toggle de asignar rol esta desactivado', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleUsuarioCreado, { status: 201 });
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(receivedBody).toEqual(
      expect.objectContaining({ tenantId: 'tenant-1', documentType: '1', documentNumber: '45678912', email: 'ada@boticas.pe' })
    );
  });

  it('crea el usuario y asigna el rol cuando el toggle esta activado y el bloque es valido', async () => {
    let roleAssignmentBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', async ({ request }) => {
        roleAssignmentBody = await request.json();
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: 'tenant-1',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(roleAssignmentBody).toEqual(
      expect.objectContaining({ tenantId: 'tenant-1', roleId: 'rol-1', scopeType: 'GLOBAL' })
    );
  });

  it('muestra fallo parcial cuando el usuario se crea pero la asignacion de rol falla, y permite reintentar', async () => {
    let attempts = 0;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json({ detail: 'El rol indicado no existe.' }, { status: 404 });
        }
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: 'tenant-1',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(
      await screen.findByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Detalle de usuario' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Reintentar asignación' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
  });

  it('muestra el error inline y no crea ninguna asignacion cuando falla la creacion del usuario', async () => {
    server.use(
      http.post('*/api/v1/usuarios', () =>
        HttpResponse.json({ detail: 'Ya existe un usuario con ese correo.' }, { status: 409 })
      )
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un usuario con ese correo.');
    expect(screen.queryByRole('heading', { name: 'Detalle de usuario' })).not.toBeInTheDocument();
  });

  it('oculta el bloque de asignacion de rol al hacer click en quitar asignacion de rol', async () => {
    server.use(
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      )
    );

    const { user } = renderPage();
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Quitar asignación de rol' }));

    expect(screen.queryByLabelText('Rol')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Asignar rol ahora')).not.toBeChecked();
  });

  it('usa tenantId vacio en la creacion cuando la sesion no tiene tenantId', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleUsuarioCreado, { status: 201 });
      })
    );

    const { user } = renderPage({ ...authenticatedSession, tenantId: null });
    await fillRequiredUserFields(user);
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(receivedBody).toEqual(expect.objectContaining({ tenantId: '' }));
  });

  it('usa tenantId vacio en la asignacion de rol cuando la sesion no tiene tenantId', async () => {
    let roleAssignmentBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', async ({ request }) => {
        roleAssignmentBody = await request.json();
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: '',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage({ ...authenticatedSession, tenantId: null });
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(roleAssignmentBody).toEqual(expect.objectContaining({ tenantId: '' }));
  });

  it('envia los campos opcionales del usuario cuando se completan', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleUsuarioCreado, { status: 201 });
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.type(screen.getByLabelText('Nombres'), 'Ada');
    await user.type(screen.getByLabelText('Apellidos'), 'Lovelace');
    await user.type(screen.getByLabelText('Username'), 'ada.lovelace');
    await user.type(screen.getByLabelText('Teléfono'), '999888777');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(receivedBody).toEqual(
      expect.objectContaining({
        firstNames: 'Ada',
        lastNames: 'Lovelace',
        username: 'ada.lovelace',
        phone: '999888777'
      })
    );
  });

  it('envia los campos opcionales del ambito cuando el rol se asigna con ambito ESTABLECIMIENTO', async () => {
    let roleAssignmentBody: unknown;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({
          asOf: '2026-09-01T00:00:00Z',
          companies: [
            {
              id: 'company-1',
              legalName: 'Boticas SAC',
              tradeName: 'Boticas',
              status: 'ACTIVE',
              establishments: [
                {
                  id: 'est-1',
                  code: 'EST-01',
                  name: 'Sede Central',
                  status: 'ACTIVE',
                  timeZone: 'America/Lima',
                  warehouses: [],
                  cashRegisters: []
                }
              ]
            }
          ]
        })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', async ({ request }) => {
        roleAssignmentBody = await request.json();
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: 'tenant-1',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'ESTABLECIMIENTO',
            companyId: 'company-1',
            establishmentId: 'est-1',
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.selectOptions(await screen.findByLabelText('Empresa'), 'company-1');
    await user.selectOptions(await screen.findByLabelText('Establecimiento'), 'est-1');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(roleAssignmentBody).toEqual(
      expect.objectContaining({
        scopeType: 'ESTABLECIMIENTO',
        companyId: 'company-1',
        establishmentId: 'est-1'
      })
    );
  });

  it('muestra un mensaje generico cuando falla la creacion del usuario por un error de red', async () => {
    server.use(http.post('*/api/v1/usuarios', () => HttpResponse.error()));

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo crear el usuario.');
  });

  it('muestra un mensaje generico cuando la asignacion de rol falla por un error de red', async () => {
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () => HttpResponse.error())
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(
      await screen.findByText('Usuario creado, pero no se pudo asignar el rol: No se pudo asignar el rol.')
    ).toBeInTheDocument();
  });

  it('no crea el usuario ni asigna el rol cuando la validacion del bloque de rol falla', async () => {
    let userCreationCalled = false;
    let roleAssignmentCalled = false;
    server.use(
      http.post('*/api/v1/usuarios', () => {
        userCreationCalled = true;
        return HttpResponse.json(sampleUsuarioCreado, { status: 201 });
      }),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({
          asOf: '2026-09-01T00:00:00Z',
          companies: [
            {
              id: 'company-1',
              legalName: 'Boticas SAC',
              tradeName: 'Boticas',
              status: 'ACTIVE',
              establishments: [
                {
                  id: 'est-1',
                  code: 'EST-01',
                  name: 'Sede Central',
                  status: 'ACTIVE',
                  timeZone: 'America/Lima',
                  warehouses: [],
                  cashRegisters: []
                }
              ]
            }
          ]
        })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () => {
        roleAssignmentCalled = true;
        return HttpResponse.json({}, { status: 201 });
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(await screen.findByLabelText('Empresa'), 'company-1');
    // scopeType queda en el default 'ESTABLECIMIENTO' sin seleccionar establecimiento: invalido.

    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Selecciona un establecimiento.')).toBeInTheDocument();
    expect(userCreationCalled).toBe(false);
    expect(roleAssignmentCalled).toBe(false);
    expect(screen.queryByRole('heading', { name: 'Detalle de usuario' })).not.toBeInTheDocument();
    expect(
      screen.queryByText('Usuario creado, pero no se pudo asignar el rol: No se pudo asignar el rol.')
    ).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
    expect(userCreationCalled).toBe(true);
    expect(roleAssignmentCalled).toBe(true);
  });

  it('ignora el submit nativo del formulario de rol si aun no se hizo click en crear usuario', async () => {
    let userCreationCalled = false;
    let roleAssignmentCalled = false;
    server.use(
      http.post('*/api/v1/usuarios', () => {
        userCreationCalled = true;
        return HttpResponse.json(sampleUsuarioCreado, { status: 201 });
      }),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () => {
        roleAssignmentCalled = true;
        return HttpResponse.json({}, { status: 201 });
      })
    );

    const { user } = renderPage();
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');

    const roleForm = document.getElementById('new-user-role-assignment-form') as HTMLFormElement;
    roleForm.requestSubmit();

    await Promise.resolve();
    expect(userCreationCalled).toBe(false);
    expect(roleAssignmentCalled).toBe(false);
    expect(screen.queryByRole('heading', { name: 'Detalle de usuario' })).not.toBeInTheDocument();
  });

  it('el enlace ir al detalle desde el fallo parcial navega sin reintentar la asignacion', async () => {
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () =>
        HttpResponse.json({ detail: 'El rol indicado no existe.' }, { status: 404 })
      )
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    await screen.findByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.');
    await user.click(screen.getByRole('link', { name: 'Ir al detalle del usuario' }));

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
  });

  it('invalida la lista de usuarios apenas se crea el usuario, incluso si luego falla la asignacion de rol', async () => {
    const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', () =>
        HttpResponse.json({ detail: 'El rol indicado no existe.' }, { status: 404 })
      )
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    await screen.findByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.');

    expect(invalidateQueriesSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['seguridad', 'usuarios'] })
    );

    invalidateQueriesSpy.mockRestore();
  });

  it('mantiene visible la pantalla de fallo parcial mientras se reintenta la asignacion de rol', async () => {
    let attempts = 0;
    server.use(
      http.post('*/api/v1/usuarios', () => HttpResponse.json(sampleUsuarioCreado, { status: 201 })),
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () =>
        HttpResponse.json({ asOf: '2026-09-01T00:00:00Z', companies: [] })
      ),
      http.post('*/api/v1/usuarios/user-nuevo/role-assignments', async () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json({ detail: 'El rol indicado no existe.' }, { status: 404 });
        }
        await new Promise((resolve) => setTimeout(resolve, 50));
        return HttpResponse.json(
          {
            id: 'assign-1',
            tenantId: 'tenant-1',
            userId: 'user-nuevo',
            roleId: 'rol-1',
            scopeType: 'GLOBAL',
            companyId: null,
            establishmentId: null,
            warehouseId: null,
            terminalId: null,
            validFrom: null,
            validUntil: null,
            status: 'ACTIVO',
            createdBy: 'wilton.sullcaray.r@gmail.com',
            createdAt: '2026-09-27T00:00:00Z'
          },
          { status: 201 }
        );
      })
    );

    const { user } = renderPage();
    await fillRequiredUserFields(user);
    await user.click(screen.getByLabelText('Asignar rol ahora'));
    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }));

    await screen.findByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.');

    await user.click(screen.getByRole('button', { name: 'Reintentar asignación' }));

    expect(
      screen.getByText('Usuario creado, pero no se pudo asignar el rol: El rol indicado no existe.')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentando…' })).toBeDisabled();
    expect(screen.queryByLabelText('Correo')).not.toBeInTheDocument();

    expect(await screen.findByRole('heading', { name: 'Detalle de usuario' })).toBeInTheDocument();
  });
});
