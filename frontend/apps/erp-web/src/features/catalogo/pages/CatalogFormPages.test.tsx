import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { SkuFormPage } from './SkuFormPage';
import { ProductoReguladoFormPage } from './ProductoReguladoFormPage';

describe.each([
  { resource: 'skus', param: 'skuId', Component: SkuFormPage, title: 'Nuevo SKU' },
  {
    resource: 'productos-regulados',
    param: 'productoReguladoId',
    Component: ProductoReguladoFormPage,
    title: 'Nuevo producto regulado'
  }
])('$resource en página completa', ({ resource, param, Component, title }) => {
  function setup(editing = false) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const base = `/catalogo/${resource}`;
    const router = createMemoryRouter(
      [
        { path: `${base}/nuevo`, Component },
        { path: `${base}/:${param}/editar`, Component },
        { path: base, element: <h1>Listado</h1> },
        { path: `${base}/registro`, element: <h1>Detalle</h1> }
      ],
      { initialEntries: [editing ? `${base}/registro/editar` : `${base}/nuevo`] }
    );
    render(
      <QueryClientProvider client={client}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    );
    return { user: userEvent.setup(), router };
  }

  it('abre directamente la creación y cancela hacia el listado', async () => {
    const { user, router } = setup();
    expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(await screen.findByRole('heading', { name: 'Listado' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/catalogo/${resource}`);
  });

  it('permite seguir editando o descartar los cambios antes de salir', async () => {
    const { user } = setup();
    const input = screen.getByLabelText(resource === 'skus' ? 'Código interno' : 'Denominación');
    await user.type(input, 'Cambio pendiente');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(await screen.findByRole('dialog', { name: 'Cambios sin guardar' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Seguir editando' }));
    expect(input).toHaveValue('Cambio pendiente');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));
    await user.click(screen.getByRole('button', { name: 'Descartar cambios' }));
    expect(await screen.findByRole('heading', { name: 'Listado' })).toBeInTheDocument();
  });

  it('impide editar sin datos y permite reintentar la carga', async () => {
    let requests = 0;
    server.use(
      http.get(`*/api/v1/catalogo/${resource}/registro`, () => {
        requests++;
        return new HttpResponse(null, { status: 500 });
      })
    );
    const { user } = setup(true);
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    await screen.findByRole('alert');
    expect(requests).toBe(2);
    await user.click(screen.getByRole('link', { name: 'Volver al detalle' }));
    expect(await screen.findByRole('heading', { name: 'Detalle' })).toBeInTheDocument();
  });
});
