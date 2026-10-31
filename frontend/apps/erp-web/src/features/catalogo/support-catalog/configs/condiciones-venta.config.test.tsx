import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  condicionVentaColumns,
  condicionVentaFields,
  condicionVentaResolver,
  condicionesVentaApi
} from './condiciones-venta.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Condiciones de venta"
          description="Administra las condiciones de venta del catálogo."
          resourceLabel="condición de venta"
          api={condicionesVentaApi}
          fields={condicionVentaFields}
          resolver={condicionVentaResolver}
          columns={condicionVentaColumns}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            denominacion: item.denominacion,
            requiereReceta: item.requiereReceta,
            requiereRetencion: item.requiereRetencion,
            fuente: item.fuente ?? '',
            versionFuente: item.versionFuente ?? ''
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: 'VL',
  denominacion: 'Venta libre',
  requiereReceta: false,
  requiereRetencion: false,
  fuente: 'DIGEMID',
  versionFuente: '2026',
  vigenteDesde: null,
  vigenteHasta: null,
  estado: 'ACTIVO'
};

describe('condiciones-venta.config', () => {
  it('lista las condiciones de venta y crea una nueva', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/condiciones-venta', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/condiciones-venta', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código'), 'VL');
    await user.type(screen.getByLabelText('Denominación'), 'Venta libre');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() => expect(screen.getByText('Venta libre')).toBeInTheDocument());
  });

  it('cambia el estado de una condicion de venta existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/condiciones-venta', () =>
        HttpResponse.json([{ ...sampleItem, estado: currentEstado }])
      ),
      http.patch('*/api/v1/catalogo/condiciones-venta/VL/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Venta libre');

    await user.click(screen.getByRole('button', { name: 'Desactivar VL' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
