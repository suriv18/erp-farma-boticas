import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { PermissionsPage } from './PermissionsPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <PermissionsPage />
      </QueryClientProvider>
    )
  };
}

describe('PermissionsPage', () => {
  it('lista los permisos agrupados por modulo', async () => {
    server.use(
      http.get('*/api/v1/permisos', () =>
        HttpResponse.json([
          {
            moduleCode: 'SEGURIDAD',
            moduleName: 'Seguridad',
            code: 'seguridad.usuarios.consultar',
            resource: 'USUARIO',
            action: 'CONSULTAR',
            name: 'Consultar usuarios',
            description: null,
            critical: false,
            status: 'ACTIVO'
          }
        ])
      )
    );

    renderPage();

    expect(await screen.findByText('Consultar usuarios')).toBeInTheDocument();
    expect(screen.getByText('seguridad.usuarios.consultar')).toBeInTheDocument();
  });

  it('muestra un mensaje cuando no hay permisos', async () => {
    server.use(http.get('*/api/v1/permisos', () => HttpResponse.json([])));

    renderPage();

    expect(await screen.findByText('No se encontraron permisos.')).toBeInTheDocument();
  });

  it('filtra por texto de busqueda', async () => {
    server.use(
      http.get('*/api/v1/permisos', ({ request }) => {
        const url = new URL(request.url);
        const search = url.searchParams.get('search');
        if (search === 'roles') {
          return HttpResponse.json([
            {
              moduleCode: 'SEGURIDAD',
              moduleName: 'Seguridad',
              code: 'seguridad.roles.consultar',
              resource: 'ROL',
              action: 'CONSULTAR',
              name: 'Consultar roles',
              description: null,
              critical: false,
              status: 'ACTIVO'
            }
          ]);
        }
        return HttpResponse.json([]);
      })
    );

    const { user } = renderPage();
    await user.type(screen.getByLabelText('Buscar permiso'), 'roles');

    expect(await screen.findByText('Consultar roles')).toBeInTheDocument();
  });
});
