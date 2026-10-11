import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { SkusPage } from './SkusPage';

function renderPage() {
  server.use(
    http.get('*/api/v1/catalogo/productos-regulados', () =>
      HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })
    )
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path: '/', Component: SkusPage }]);
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

const sample = {
  id: 'sku-1',
  codigoInterno: 'SKU-001',
  descripcionComercial: 'Alcohol 70% 250 ml',
  tipoSku: 'NO_REGULADO',
  estado: 'ACTIVO'
};

function pagina(items: unknown[], extra: Record<string, number> = {}) {
  return { items, page: 0, size: 20, totalElements: items.length, ...extra };
}

describe('SkusPage', () => {
  it('lista los SKU sin enviar tenant y con enlace al detalle', async () => {
    let tenant: string | null = 'pendiente';
    server.use(
      http.get('*/api/v1/catalogo/skus', ({ request }) => {
        tenant = new URL(request.url).searchParams.get('tenantId');
        return HttpResponse.json(pagina([sample]));
      })
    );

    renderPage();

    expect(await screen.findByText('Alcohol 70% 250 ml')).toBeInTheDocument();
    expect(tenant).toBeNull();
    expect(screen.getByRole('link', { name: 'Ver detalle de SKU-001' })).toHaveAttribute(
      'href',
      '/catalogo/skus/sku-1'
    );
  });

  it('informa cuando el listado falla', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => new HttpResponse(null, { status: 500 })));

    renderPage();

    expect(await screen.findByText('No se pudo cargar el listado de SKU.')).toBeInTheDocument();
  });

  it('aplica filtros y pagina en el servidor', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get('*/api/v1/catalogo/skus', ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sample], { totalElements: 120 }));
      })
    );

    const { user } = renderPage();
    await screen.findByText('Alcohol 70% 250 ml');
    await screen.findByRole('option', { name: 'Bayer' });

    const avanzar = async () => {
      await user.click(screen.getByRole('button', { name: 'Siguiente' }));
      await waitFor(() => expect(received.get('page')).toBe('1'));
    };

    await avanzar();
    await user.type(screen.getByLabelText('Buscar SKU'), 'alc');
    await waitFor(() => {
      expect(received.get('q')).toBe('alc');
      expect(received.get('page')).toBe('0');
    });

    await avanzar();
    await user.selectOptions(screen.getByLabelText('Tipo de SKU'), 'REGULADO');
    await waitFor(() => expect(received.get('tipoSku')).toBe('REGULADO'));

    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await waitFor(() => expect(received.get('estado')).toBe('BLOQUEADO'));

    await user.selectOptions(screen.getByLabelText('Categoría'), 'categoria-1');
    await waitFor(() => expect(received.get('categoriaId')).toBe('categoria-1'));

    await user.selectOptions(screen.getByLabelText('Marca'), 'marca-1');
    await waitFor(() => expect(received.get('marcaId')).toBe('marca-1'));
    expect(received.get('page')).toBe('0');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => expect(received.get('size')).toBe('50'));
  });

  it('crea un SKU sin enviar tenant y refresca el listado', async () => {
    let created = false;
    let body: Record<string, unknown> = {};
    server.use(
      http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina(created ? [sample] : []))),
      http.post('*/api/v1/catalogo/skus', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        created = true;
        return HttpResponse.json({ id: 'sku-1' }, { status: 201 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('No se encontraron SKU.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Nuevo SKU' }));
    await screen.findAllByRole('option', { name: 'UND — Unidad' });
    await user.selectOptions(
      screen.getByLabelText('Tipo de SKU', { selector: '#sku-tipoSku' }),
      'NO_REGULADO'
    );
    await user.type(screen.getByLabelText('Código interno'), 'SKU-001');
    await user.type(screen.getByLabelText('Descripción comercial'), 'Alcohol 70% 250 ml');
    await user.selectOptions(screen.getByLabelText('Unidad de venta'), 'UND');
    await user.click(screen.getByRole('button', { name: 'Crear SKU' }));

    expect(await screen.findByText('Alcohol 70% 250 ml')).toBeInTheDocument();
    expect(body).not.toHaveProperty('tenantId');
    expect(body).toMatchObject({
      tipoSku: 'NO_REGULADO',
      codigoInterno: 'SKU-001',
      unidadVentaCodigo: 'UND',
      stockMinimoDefault: 0
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y permite cerrar el dialogo', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([]))),
      http.post('*/api/v1/catalogo/skus', () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Ya existe un SKU con ese código interno.' },
          { status: 409 }
        )
      )
    );

    const { user } = renderPage();
    await screen.findByText('No se encontraron SKU.');

    await user.click(screen.getByRole('button', { name: 'Nuevo SKU' }));
    await screen.findAllByRole('option', { name: 'UND — Unidad' });
    await user.selectOptions(
      screen.getByLabelText('Tipo de SKU', { selector: '#sku-tipoSku' }),
      'NO_REGULADO'
    );
    await user.type(screen.getByLabelText('Código interno'), 'SKU-001');
    await user.type(screen.getByLabelText('Descripción comercial'), 'Alcohol');
    await user.selectOptions(screen.getByLabelText('Unidad de venta'), 'UND');
    await user.click(screen.getByRole('button', { name: 'Crear SKU' }));
    expect(await screen.findByText('Ya existe un SKU con ese código interno.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
