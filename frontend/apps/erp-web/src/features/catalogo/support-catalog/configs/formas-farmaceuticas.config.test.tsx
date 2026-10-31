import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  formaFarmaceuticaColumns,
  formaFarmaceuticaFields,
  formaFarmaceuticaResolver,
  formasFarmaceuticasApi
} from './formas-farmaceuticas.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Formas farmacéuticas"
          description="Administra las formas farmacéuticas del catálogo."
          resourceLabel="forma farmacéutica"
          api={formasFarmaceuticasApi}
          fields={formaFarmaceuticaFields}
          resolver={formaFarmaceuticaResolver}
          columns={formaFarmaceuticaColumns}
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

const sampleItem = { codigo: 'TAB', denominacion: 'Tableta', fuente: 'DIGEMID', estado: 'ACTIVO' };

describe('formas-farmaceuticas.config', () => {
  it('lista las formas farmaceuticas y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/formas-farmaceuticas', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/formas-farmaceuticas', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'TAB');
    await user.type(screen.getByLabelText('Denominación'), 'Tableta');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Tableta')).toBeInTheDocument());
  });

  it('cambia el estado de una forma farmaceutica existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/formas-farmaceuticas', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/formas-farmaceuticas/TAB/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Tableta');

    await user.click(screen.getByRole('button', { name: 'Desactivar TAB' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
