import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { SkuDetailPage } from './SkuDetailPage';

function renderPage() {
  server.use(
    http.get('*/api/v1/catalogo/productos-regulados', () =>
      HttpResponse.json({
        items: [
          {
            id: 'pr-1',
            denominacion: 'Paracetamol 500 mg',
            condicionVentaCodigo: 'VL',
            estadoRegulatorio: 'VIGENTE'
          }
        ],
        page: 0,
        size: 100,
        totalElements: 1
      })
    )
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/catalogo/skus/:skuId', Component: SkuDetailPage }], {
    initialEntries: ['/catalogo/skus/sku-1']
  });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

function skuApi(overrides: Record<string, unknown> = {}) {
  return {
    id: 'sku-1',
    tenantId: 'tenant-1',
    productoReguladoId: 'pr-1',
    categoriaId: 'categoria-1',
    marcaId: 'marca-desconocida',
    tipoSku: 'REGULADO',
    codigoInterno: 'SKU-001',
    descripcionComercial: 'Paracetamol 500 mg x 100',
    nombreCorto: null,
    presentacionComercial: null,
    unidadVentaCodigo: 'UND',
    contenido: 100,
    unidadContenidoCodigo: null,
    pesoGramos: null,
    altoCm: null,
    anchoCm: null,
    largoCm: null,
    permiteVentaFraccion: false,
    factorFraccion: null,
    unidadFraccionCodigo: null,
    requiereLote: true,
    requiereVencimiento: true,
    afectoIgv: true,
    stockMinimoDefault: 5,
    stockMaximoDefault: null,
    imagenUri: null,
    codigosBarra: [],
    estado: 'ACTIVO',
    createdBy: 'admin',
    createdAt: '2026-01-01T00:00:00Z',
    updatedBy: null,
    updatedAt: null,
    ...overrides
  };
}

describe('SkuDetailPage', () => {
  it('muestra los datos del SKU resolviendo nombres y marcadores', async () => {
    let tenant: string | null = 'pendiente';
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', ({ request }) => {
        tenant = new URL(request.url).searchParams.get('tenantId');
        return HttpResponse.json(skuApi());
      })
    );

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 100' })
    ).toBeInTheDocument();
    expect(tenant).toBeNull();
    expect(await screen.findAllByText('Paracetamol 500 mg')).not.toHaveLength(0);
    expect(screen.getByText('Analgésicos')).toBeInTheDocument();
    expect(screen.getByText('marca-desconocida')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(5);
    expect(screen.getByRole('link', { name: 'Catálogo / SKU' })).toHaveAttribute(
      'href',
      '/catalogo/skus'
    );
  });

  it('muestra marcador cuando el SKU no tiene producto, categoria ni marca', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () =>
        HttpResponse.json(
          skuApi({
            tipoSku: 'NO_REGULADO',
            productoReguladoId: null,
            categoriaId: null,
            marcaId: null
          })
        )
      )
    );

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 100' })
    ).toBeInTheDocument();
    expect(screen.queryByText('Analgésicos')).not.toBeInTheDocument();
  });

  it('muestra el error cuando el SKU no existe', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => new HttpResponse(null, { status: 404 }))
    );

    renderPage();

    expect(
      await screen.findByText('El recurso no existe o no pertenece a tu organización.')
    ).toBeInTheDocument();
  });

  it('edita el SKU sin enviar tenant y refresca los datos', async () => {
    let descripcion = 'Paracetamol 500 mg x 100';
    let body: Record<string, unknown> = {};
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () =>
        HttpResponse.json(skuApi({ descripcionComercial: descripcion }))
      ),
      http.put('*/api/v1/catalogo/skus/sku-1', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        descripcion = body.descripcionComercial as string;
        return HttpResponse.json(skuApi({ descripcionComercial: descripcion }));
      })
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 100' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    const campo = screen.getByLabelText('Descripción comercial');
    await user.clear(campo);
    await user.type(campo, 'Paracetamol 500 mg x 50');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(
      await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 50' })
    ).toBeInTheDocument();
    expect(body).toMatchObject({
      codigoInterno: 'SKU-001',
      stockMinimoDefault: 5
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al editar y permite cerrar el dialogo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi())),
      http.put('*/api/v1/catalogo/skus/sku-1', () => new HttpResponse(null, { status: 403 }))
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 100' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('cambia el estado del SKU enviando solo el estado', async () => {
    let estado = 'ACTIVO';
    let body: unknown;
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi({ estado }))),
      http.patch('*/api/v1/catalogo/skus/sku-1/estado', async ({ request }) => {
        body = await request.json();
        estado = (body as { status: string }).status;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 100' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(
      screen.getByLabelText('Estado', { selector: '#cambiar-estado' }),
      'BLOQUEADO'
    );
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(screen.getByText('BLOQUEADO')).toBeInTheDocument());
    expect(body).toEqual({ status: 'BLOQUEADO' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del cambio de estado y permite cerrar el dialogo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi())),
      http.patch('*/api/v1/catalogo/skus/sku-1/estado', () =>
        HttpResponse.json({ title: 'Inválido', detail: 'Estado no permitido.' }, { status: 400 })
      )
    );

    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Paracetamol 500 mg x 100' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(
      screen.getByLabelText('Estado', { selector: '#cambiar-estado' }),
      'DESCONTINUADO'
    );
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));
    expect(await screen.findByText('Estado no permitido.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
