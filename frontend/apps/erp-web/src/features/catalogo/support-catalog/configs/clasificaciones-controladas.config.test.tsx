import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  clasificacionControladaColumns,
  clasificacionControladaFields,
  clasificacionControladaResolver,
  clasificacionesControladasApi
} from './clasificaciones-controladas.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Clasificaciones controladas"
          description="Administra las clasificaciones controladas del catálogo."
          resourceLabel="clasificación controlada"
          api={clasificacionesControladasApi}
          fields={clasificacionControladaFields}
          resolver={clasificacionControladaResolver}
          columns={clasificacionControladaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            normaFuente: item.normaFuente ?? '',
            requiereRecetaEspecial: item.requiereRecetaEspecial,
            retieneReceta: item.retieneReceta,
            vigenciaRecetaDias: item.vigenciaRecetaDias ?? undefined
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: 'IIA',
  denominacion: 'Lista II-A',
  normaFuente: 'DS 023-2001-SA',
  requiereRecetaEspecial: true,
  retieneReceta: true,
  vigenciaRecetaDias: 30,
  estado: 'ACTIVO'
};

describe('clasificaciones-controladas.config', () => {
  it('lista las clasificaciones controladas y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/clasificaciones-controladas', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/clasificaciones-controladas', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'IIA');
    await user.type(screen.getByLabelText('Denominación'), 'Lista II-A');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Lista II-A')).toBeInTheDocument());
  });

  it('cambia el estado de una clasificacion controlada existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/clasificaciones-controladas', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/clasificaciones-controladas/IIA/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Lista II-A');

    await user.click(screen.getByRole('button', { name: 'Desactivar IIA' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
