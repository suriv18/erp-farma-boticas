import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import type { Sku } from '../api/skus.types';
import { CodigosBarraSection } from './CodigosBarraSection';

const sku = {
  id: 'sku-1',
  codigosBarra: [
    {
      codigoBarra: '7750001000012',
      tipoCodigo: 'EAN13',
      esPrincipal: true,
      vigenteDesde: '2026-01-01',
      vigenteHasta: null,
      estado: 'ACTIVO'
    },
    {
      codigoBarra: '7750001000029',
      tipoCodigo: null,
      esPrincipal: false,
      vigenteDesde: null,
      vigenteHasta: null,
      estado: 'ACTIVO'
    }
  ]
} as Sku;

function renderSection(value: Sku = sku) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <CodigosBarraSection sku={value} />
      </QueryClientProvider>
    )
  };
}

describe('CodigosBarraSection', () => {
  it('lista los codigos y ofrece marcar como principal solo los que no lo son', () => {
    renderSection();

    expect(screen.getByText('7750001000012')).toBeInTheDocument();
    expect(screen.getByText('Sí')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Marcar 7750001000012 como principal' })
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Marcar 7750001000029 como principal' })
    ).toBeInTheDocument();
  });

  it('muestra el mensaje vacio cuando no hay codigos', () => {
    renderSection({ ...sku, codigosBarra: [] });

    expect(screen.getByText('El SKU no tiene códigos de barras registrados.')).toBeInTheDocument();
  });

  it('agrega un codigo de barras sin tenant y reinicia el formulario', async () => {
    let body: unknown;
    let search = '?';
    server.use(
      http.post('*/api/v1/catalogo/skus/sku-1/codigos-barra', async ({ request }) => {
        search = new URL(request.url).search;
        body = await request.json();
        return HttpResponse.json(sku);
      })
    );

    const { user } = renderSection();

    await user.type(screen.getByLabelText('Código de barras'), '7750009999999');
    await user.type(screen.getByLabelText('Vigente desde'), '2026-02-01');
    await user.click(screen.getByRole('button', { name: 'Agregar código de barras' }));

    await waitFor(() =>
      expect(body).toEqual({
        codigoBarra: '7750009999999',
        tipoCodigo: 'EAN13',
        vigenteDesde: '2026-02-01'
      })
    );
    expect(search).toBe('');
    await waitFor(() => expect(screen.getByLabelText('Código de barras')).toHaveValue(''));
  });

  it('muestra el error del servidor al agregar', async () => {
    server.use(
      http.post('*/api/v1/catalogo/skus/sku-1/codigos-barra', () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Ya existe un SKU con el código de barras indicado.' },
          { status: 409 }
        )
      )
    );

    const { user } = renderSection();
    await user.type(screen.getByLabelText('Código de barras'), '7750001000012');
    await user.click(screen.getByRole('button', { name: 'Agregar código de barras' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un SKU con el código de barras indicado.'
    );
  });

  it('marca un codigo como principal', async () => {
    let llamado = false;
    server.use(
      http.patch('*/api/v1/catalogo/skus/sku-1/codigos-barra/7750001000029/principal', () => {
        llamado = true;
        return HttpResponse.json(sku);
      })
    );

    const { user } = renderSection();
    await user.click(screen.getByRole('button', { name: 'Marcar 7750001000029 como principal' }));

    await waitFor(() => expect(llamado).toBe(true));
  });

  it('elimina un codigo de barras', async () => {
    let eliminado = false;
    server.use(
      http.delete('*/api/v1/catalogo/skus/sku-1/codigos-barra/7750001000029', () => {
        eliminado = true;
        return HttpResponse.json(sku);
      })
    );

    const { user } = renderSection();
    await user.click(screen.getByRole('button', { name: 'Eliminar 7750001000029' }));

    await waitFor(() => expect(eliminado).toBe(true));
  });

  it('muestra el error cuando marcar como principal falla', async () => {
    server.use(
      http.patch(
        '*/api/v1/catalogo/skus/sku-1/codigos-barra/7750001000029/principal',
        () => new HttpResponse(null, { status: 404 })
      )
    );

    const { user } = renderSection();
    await user.click(screen.getByRole('button', { name: 'Marcar 7750001000029 como principal' }));

    expect(
      await screen.findByText('El recurso no existe o no pertenece a tu organización.')
    ).toBeInTheDocument();
  });

  it('muestra el error cuando eliminar falla', async () => {
    server.use(
      http.delete(
        '*/api/v1/catalogo/skus/sku-1/codigos-barra/7750001000029',
        () => new HttpResponse(null, { status: 403 })
      )
    );

    const { user } = renderSection();
    await user.click(screen.getByRole('button', { name: 'Eliminar 7750001000029' }));

    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();
  });
});
