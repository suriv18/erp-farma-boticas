import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../test/mocks/server';
import { SupportCatalogPage } from '../SupportCatalogPage';
import {
  tipoDocumentoIdentidadColumns,
  tipoDocumentoIdentidadFields,
  tipoDocumentoIdentidadResolver,
  tiposDocumentoIdentidadApi
} from './tipos-documento-identidad.config';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SupportCatalogPage
          title="Tipos de documento de identidad"
          description="Administra el catálogo SUNAT de tipos de documento de identidad."
          resourceLabel="tipo de documento"
          api={tiposDocumentoIdentidadApi}
          fields={tipoDocumentoIdentidadFields}
          resolver={tipoDocumentoIdentidadResolver}
          columns={tipoDocumentoIdentidadColumns}
          searchableFields={['codigo', 'sigla', 'denominacion']}
          toRequest={(values) => values}
          toDefaultValues={(item) => ({
            codigo: item.codigo,
            sigla: item.sigla,
            denominacion: item.denominacion,
            max: item.max ?? undefined,
            min: item.min ?? undefined
          })}
        />
      </QueryClientProvider>
    )
  };
}

const sampleItem = {
  codigo: '1',
  sigla: 'DNI',
  denominacion: 'Documento Nacional de Identidad',
  max: 8,
  min: 8,
  estado: 'ACTIVO'
};

function pagina(items: unknown[]) {
  return { items, page: 0, size: 100, totalElements: items.length };
}

describe('tipos-documento-identidad.config', () => {
  it('lista los tipos de documento y crea uno nuevo', async () => {
    let created = false;
    server.use(
      http.get('*/api/v1/catalogo/tipos-documento-identidad', () =>
        HttpResponse.json(pagina(created ? [sampleItem] : []))
      ),
      http.post('*/api/v1/catalogo/tipos-documento-identidad', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ ...sampleItem, ...body }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron registros.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nuevo/ }));
    await user.type(screen.getByLabelText('Código SUNAT'), '1');
    await user.type(screen.getByLabelText('Sigla'), 'DNI');
    await user.type(screen.getByLabelText('Denominación'), 'Documento Nacional de Identidad');
    await user.click(screen.getByRole('button', { name: 'Crear' }));

    await waitFor(() =>
      expect(screen.getByText('Documento Nacional de Identidad')).toBeInTheDocument()
    );
  });

  it('cambia el estado de un tipo de documento existente', async () => {
    let currentEstado = 'ACTIVO';
    server.use(
      http.get('*/api/v1/catalogo/tipos-documento-identidad', () =>
        HttpResponse.json(pagina([{ ...sampleItem, estado: currentEstado }]))
      ),
      http.patch('*/api/v1/catalogo/tipos-documento-identidad/1/estado', async ({ request }) => {
        const body = (await request.json()) as { status: string };
        currentEstado = body.status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByText('Documento Nacional de Identidad');

    await user.click(screen.getByRole('button', { name: 'Desactivar 1' }));

    await waitFor(() => expect(screen.getByText('INACTIVO')).toBeInTheDocument());
  });
});
