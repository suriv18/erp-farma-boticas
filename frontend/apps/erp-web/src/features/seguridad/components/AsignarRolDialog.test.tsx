import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { AsignarRolDialog } from './AsignarRolDialog';

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

const sampleStructure = {
  asOf: '2026-09-01T00:00:00Z',
  companies: [
    {
      id: 'company-1',
      legalName: 'Boticas SAC',
      tradeName: 'Boticas',
      status: 'ACTIVE' as const,
      establishments: [
        {
          id: 'est-1',
          code: 'EST-01',
          name: 'Sede Central',
          status: 'ACTIVE' as const,
          timeZone: 'America/Lima',
          warehouses: [{ id: 'wh-1', code: 'WH-01', name: 'Almacén central', status: 'ACTIVE' as const }],
          cashRegisters: [{ id: 'cr-1', code: 'CR-01', name: 'Caja 1', status: 'ACTIVE' as const }]
        }
      ]
    }
  ]
};

function renderDialog(onSubmit = vi.fn(), onCancel = vi.fn(), errorMessage?: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    onSubmit,
    onCancel,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AsignarRolDialog
          open
          tenantId="tenant-1"
          onSubmit={onSubmit}
          onCancel={onCancel}
          errorMessage={errorMessage}
        />
      </QueryClientProvider>
    )
  };
}

describe('AsignarRolDialog', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/roles', () => HttpResponse.json(sampleRolesPage)),
      http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleStructure))
    );
  });

  it('exige seleccionar un establecimiento cuando el ambito es ESTABLECIMIENTO', async () => {
    const { user, onSubmit } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    expect(await screen.findByText('Selecciona un establecimiento.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envia el payload completo cuando el ambito ESTABLECIMIENTO tiene establecimiento seleccionado', async () => {
    const { user, onSubmit } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.selectOptions(await screen.findByLabelText('Empresa'), 'company-1');
    await user.selectOptions(await screen.findByLabelText('Establecimiento'), 'est-1');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ roleId: 'rol-1', scopeType: 'ESTABLECIMIENTO', establishmentId: 'est-1' })
      )
    );
  });

  it('no exige seleccion adicional cuando el ambito es GLOBAL', async () => {
    const { user, onSubmit } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Rol'), 'rol-1');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.click(screen.getByRole('button', { name: 'Asignar rol' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ roleId: 'rol-1', scopeType: 'GLOBAL' }))
    );
  });

  it('resetea empresa y establecimiento seleccionados al cambiar el tipo de ambito', async () => {
    const { user } = renderDialog();

    await screen.findByText('Administrador local');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');
    await user.selectOptions(await screen.findByLabelText('Empresa'), 'company-1');
    await user.selectOptions(await screen.findByLabelText('Establecimiento'), 'est-1');

    expect(screen.getByLabelText('Establecimiento')).toHaveValue('est-1');

    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'GLOBAL');
    await user.selectOptions(screen.getByLabelText('Tipo de ámbito'), 'ESTABLECIMIENTO');

    expect(await screen.findByLabelText('Empresa')).toHaveValue('');
    expect(screen.getByLabelText('Establecimiento')).toHaveValue('');
  });

  it('muestra el mensaje de error dentro del dialogo cuando errorMessage esta presente', async () => {
    renderDialog(vi.fn(), vi.fn(), 'No se pudo asignar el rol.');

    await screen.findByText('Administrador local');
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByRole('alert')).toHaveTextContent('No se pudo asignar el rol.');
  });

  it('no renderiza ningun mensaje de error cuando errorMessage no esta presente', async () => {
    renderDialog();

    await screen.findByText('Administrador local');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
