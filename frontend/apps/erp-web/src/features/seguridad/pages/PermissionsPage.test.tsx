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

const consultarUsuarios = {
  moduleCode: 'SEGURIDAD',
  moduleName: 'Seguridad',
  code: 'seguridad.usuarios.consultar',
  resource: 'USUARIO',
  action: 'CONSULTAR',
  name: 'Consultar usuarios',
  description: null,
  critical: false,
  status: 'ACTIVO'
};

describe('PermissionsPage', () => {
  it('lista los permisos agrupados por modulo', async () => {
    server.use(
      http.get('*/api/v1/permisos', () =>
        HttpResponse.json({ items: [consultarUsuarios], page: 0, size: 20, totalElements: 1 })
      )
    );

    renderPage();

    expect(await screen.findByText('Consultar usuarios')).toBeInTheDocument();
    expect(screen.getByText('seguridad.usuarios.consultar')).toBeInTheDocument();
  });

  it('muestra un mensaje cuando no hay permisos', async () => {
    server.use(
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 }))
    );

    renderPage();

    expect(await screen.findByText('No se encontraron permisos.')).toBeInTheDocument();
  });

  it('filtra por texto de busqueda', async () => {
    server.use(
      http.get('*/api/v1/permisos', ({ request }) => {
        const url = new URL(request.url);
        const search = url.searchParams.get('search');
        if (search === 'roles') {
          return HttpResponse.json({
            items: [
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
            ],
            page: 0,
            size: 20,
            totalElements: 1
          });
        }
        return HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 });
      })
    );

    const { user } = renderPage();
    await user.type(screen.getByLabelText('Buscar permiso'), 'roles');

    expect(await screen.findByText('Consultar roles')).toBeInTheDocument();
  });

  it('pagina en el servidor pidiendo la pagina y el tamano solicitados', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('*/api/v1/permisos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [consultarUsuarios], page: 0, size: 20, totalElements: 45 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('Consultar usuarios')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(
      await screen.findByText((_, element) => element?.textContent === 'Página 2 de 3')
    ).toBeInTheDocument();
    expect(receivedUrl?.searchParams.get('page')).toBe('1');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
  });
});
