import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import type { ProductoRegulado } from '../api/productos-regulados.types';
import { PrincipiosAsociadosSection } from './PrincipiosAsociadosSection';

const producto = {
  id: 'pr-1',
  principiosActivos: [
    {
      principioActivoId: 'pa-1',
      concentracionTexto: '500 mg',
      cantidad: 500,
      unidadMedidaCodigo: 'MG',
      esPrincipal: true,
      orden: 1
    },
    {
      principioActivoId: 'pa-inactivo',
      concentracionTexto: null,
      cantidad: null,
      unidadMedidaCodigo: null,
      esPrincipal: false,
      orden: 2
    }
  ]
} as ProductoRegulado;

function renderSection(value: ProductoRegulado = producto) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <PrincipiosAsociadosSection producto={value} />
      </QueryClientProvider>
    )
  };
}

function usarPrincipios() {
  server.use(
    http.get('*/api/v1/catalogo/principios-activos', () =>
      HttpResponse.json([{ id: 'pa-1', denominacion: 'Paracetamol' }])
    )
  );
}

describe('PrincipiosAsociadosSection', () => {
  it('lista las asociaciones usando el nombre del principio o su id como respaldo', async () => {
    usarPrincipios();

    renderSection();

    expect(await screen.findByRole('cell', { name: 'Paracetamol' })).toBeInTheDocument();
    expect(screen.getByText('pa-inactivo')).toBeInTheDocument();
    expect(screen.getByText('500 mg')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(3);
    expect(screen.getByText('Sí')).toBeInTheDocument();
  });

  it('muestra el mensaje vacio cuando no hay asociaciones', async () => {
    usarPrincipios();

    renderSection({ ...producto, principiosActivos: [] });

    expect(
      await screen.findByText('El producto no tiene principios activos asociados.')
    ).toBeInTheDocument();
  });

  it('asocia un principio activo y reinicia el formulario', async () => {
    usarPrincipios();
    let body: unknown;
    server.use(
      http.post(
        '*/api/v1/catalogo/productos-regulados/pr-1/principios-activos',
        async ({ request }) => {
          body = await request.json();
          return HttpResponse.json(producto);
        }
      )
    );

    const { user } = renderSection();
    await screen.findByRole('option', { name: 'Paracetamol' });

    await user.selectOptions(screen.getByLabelText('Principio activo'), 'pa-1');
    await user.click(screen.getByRole('button', { name: 'Asociar principio activo' }));

    await waitFor(() =>
      expect(body).toEqual({
        principioActivoId: 'pa-1',
        esPrincipal: true,
        orden: 1
      })
    );
    await waitFor(() => expect(screen.getByLabelText('Principio activo')).toHaveValue(''));
  });

  it('muestra el error del servidor al asociar', async () => {
    usarPrincipios();
    server.use(
      http.post('*/api/v1/catalogo/productos-regulados/pr-1/principios-activos', () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'El principio activo ya está asociado.' },
          { status: 409 }
        )
      )
    );

    const { user } = renderSection();
    await screen.findByRole('option', { name: 'Paracetamol' });

    await user.selectOptions(screen.getByLabelText('Principio activo'), 'pa-1');
    await user.click(screen.getByRole('button', { name: 'Asociar principio activo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El principio activo ya está asociado.'
    );
  });

  it('quita un principio activo asociado', async () => {
    usarPrincipios();
    let deleted = false;
    server.use(
      http.delete('*/api/v1/catalogo/productos-regulados/pr-1/principios-activos/pa-1', () => {
        deleted = true;
        return HttpResponse.json(producto);
      })
    );

    const { user } = renderSection();
    await screen.findByRole('cell', { name: 'Paracetamol' });

    await user.click(screen.getByRole('button', { name: 'Quitar Paracetamol' }));

    await waitFor(() => expect(deleted).toBe(true));
  });

  it('muestra el error cuando quitar falla', async () => {
    usarPrincipios();
    server.use(
      http.delete(
        '*/api/v1/catalogo/productos-regulados/pr-1/principios-activos/pa-1',
        () => new HttpResponse(null, { status: 403 })
      )
    );

    const { user } = renderSection();
    await screen.findByRole('cell', { name: 'Paracetamol' });

    await user.click(screen.getByRole('button', { name: 'Quitar Paracetamol' }));

    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();
  });
});
