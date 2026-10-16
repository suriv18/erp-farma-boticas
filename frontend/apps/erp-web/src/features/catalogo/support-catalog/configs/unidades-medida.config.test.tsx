import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  unidadMedidaColumns,
  unidadMedidaFields,
  unidadMedidaResolver,
  unidadesMedidaApi
} from './unidades-medida.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Unidades de medida"
          description="Administra las unidades de medida del catálogo."
          resourceLabel="unidad de medida"
          api={unidadesMedidaApi}
          fields={unidadMedidaFields}
          resolver={unidadMedidaResolver}
          columns={unidadMedidaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            simbolo: item.simbolo ?? '',
            permiteDecimal: item.permiteDecimal,
            fuente: item.fuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: 'UND',
  denominacion: 'Unidad',
  simbolo: 'u',
  permiteDecimal: false,
  fuente: 'DIGEMID',
  estado: 'ACTIVO'
};

describe('unidades-medida.config', () => {
  it('lista las unidades de medida y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/unidades-medida', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/unidades-medida', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'UND');
    await user.type(screen.getByLabelText('Denominación'), 'Unidad');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Unidad')).toBeInTheDocument());
  });

  it('cambia el estado de una unidad de medida existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/unidades-medida', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/unidades-medida/UND/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Unidad');

    await user.click(screen.getByRole('button', { name: 'Desactivar UND' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
