import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  viaAdministracionColumns,
  viaAdministracionFields,
  viaAdministracionResolver,
  viasAdministracionApi
} from './vias-administracion.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Vías de administración"
          description="Administra las vías de administración del catálogo."
          resourceLabel="vía de administración"
          api={viasAdministracionApi}
          fields={viaAdministracionFields}
          resolver={viaAdministracionResolver}
          columns={viaAdministracionColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            fuente: item.fuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = { codigo: 'ORAL', denominacion: 'Vía oral', fuente: 'DIGEMID', estado: 'ACTIVO' };

describe('vias-administracion.config', () => {
  it('lista las vias de administracion y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/vias-administracion', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'ORAL');
    await user.type(screen.getByLabelText('Denominación'), 'Vía oral');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Vía oral')).toBeInTheDocument());
  });

  it('cambia el estado de una via de administracion existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/vias-administracion/ORAL/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.click(screen.getByRole('button', { name: 'Desactivar ORAL' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
