import { zodResolver } from '@hookform/resolvers/zod';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { z } from 'zod';
import { server } from '../../../test/mocks/server';
import { createSupportCatalogApi } from './support-catalog.api';
import { SupportCatalogPage } from './SupportCatalogPage';
import type { FieldDef, SupportCatalogItem } from './support-catalog.types';

type SampleItem = SupportCatalogItem & { denominacion: string };
type SampleRequest = { codigo: string; denominacion: string };

const api = createSupportCatalogApi<SampleItem, SampleRequest>('vias-administracion');

const fields: FieldDef[] = [
  { name: 'codigo', label: 'Código', type: 'text' },
  { name: 'denominacion', label: 'Denominación', type: 'text' }
];

const schema = z.object({
  codigo: z.string().min(1, 'Requerido'),
  denominacion: z.string().min(1, 'Requerido')
});
const resolver = zodResolver(schema);

function renderPage(pageSize = 20) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage<SampleItem, SampleRequest>
          title="Vías de administración"
          description="Administra las vías de administración del catálogo."
          resourceLabel="vía"
          api={api}
          fields={fields}
          resolver={resolver}
          columns={[
            { header: 'Código', cell: (row: SampleItem) => row.codigo },
            { header: 'Denominación', cell: (row: SampleItem) => row.denominacion }
          ]}
          searchableFields={['codigo', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({ codigo: item.codigo, denominacion: item.denominacion })}
          pageSize={pageSize}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem: SampleItem = { codigo: 'ORAL', denominacion: 'Vía oral', estado: 'ACTIVO' };

describe('SupportCatalogPage', () => {
  it('lista los items del catalogo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () => HttpResponse.json([sampleItem]))
    );

    renderPage();

    expect(await screen.findByText('Vía oral')).toBeInTheDocument();
  });

  it('filtra en cliente por texto de busqueda', async () => {
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([
          sampleItem,
          { codigo: 'IV', denominacion: 'Vía intravenosa', estado: 'ACTIVO' }
        ])
      )
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');
    expect(screen.getByText('Vía intravenosa')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Buscar/), 'intraven');

    await waitFor(() => expect(screen.queryByText('Vía oral')).not.toBeInTheDocument());
    expect(screen.getByText('Vía intravenosa')).toBeInTheDocument();
  });

  it('pagina en cliente cuando hay mas items que el tamano de pagina', async () => {
    const items: SampleItem[] = Array.from({ length: 3 }, (_, index) => ({
      codigo: `COD-${index}`,
      denominacion: `Vía ${index}`,
      estado: 'ACTIVO'
    }));
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () => HttpResponse.json(items))
    );

    renderPage(2);

    await screen.findByText('Vía 0');
    expect(screen.getByText('Vía 1')).toBeInTheDocument();
    expect(screen.queryByText('Vía 2')).not.toBeInTheDocument();
  });

  it('crea un item nuevo y refresca el listado', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json(created ? [sampleItem] : [])
      ),
      http.post('*/api/v1/catalogo/vias-administracion', () => {
        created = true;
        return HttpResponse.json(sampleItem, { status: 201 });
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

  it('edita un item existente', async () => {
    let currentDenominacion = 'Vía oral';
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', () =>
        HttpResponse.json([{ ...sampleItem, denominacion: currentDenominacion }])
      ),
      http.put('*/api/v1/catalogo/vias-administracion/ORAL', async ({ request }) => {
        const body = (await request.json()) as SampleRequest;
        currentDenominacion = body.denominacion;
        return HttpResponse.json({ ...sampleItem, denominacion: currentDenominacion });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.click(screen.getByRole('button', { name: 'Editar ORAL' }));
    const denominacionInput = screen.getByLabelText('Denominación');
    await user.clear(denominacionInput);
    await user.type(denominacionInput, 'Vía oral estricta');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(screen.getByText('Vía oral estricta')).toBeInTheDocument());
  });

  it('cambia el estado de un item activo a inactivo', async () => {
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

  it('filtra por estado contra el backend', async () => {
    let receivedEstado: string | null = null;
    server.use(
      http.get('*/api/v1/catalogo/vias-administracion', ({ request }) => {
        receivedEstado = new URL(request.url).searchParams.get('estado');
        return HttpResponse.json(receivedEstado === 'INACTIVO' ? [] : [sampleItem]);
      })
    );

    const { user } = renderPage();
    await screen.findByText('Vía oral');

    await user.selectOptions(screen.getByLabelText('Estado'), 'INACTIVO');

    await waitFor(() => expect(receivedEstado).toBe('INACTIVO'));
    expect(screen.getByText('No se encontraron registros.')).toBeInTheDocument();
  });
});
