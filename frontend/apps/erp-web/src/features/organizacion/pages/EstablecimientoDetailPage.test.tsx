import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import {
  pagina,
  sampleAlmacen,
  sampleEstablecimiento,
  sampleTerminal
} from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EstablecimientoDetailPage } from './EstablecimientoDetailPage';

const detailUrl = '*/api/v1/organizacion/establecimientos/est-1';
const statusUrl = '*/api/v1/organizacion/establecimientos/est-1/estado';

function mockDefaultHandlers() {
  server.use(
    http.get(detailUrl, () => HttpResponse.json(sampleEstablecimiento)),
    http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([sampleAlmacen]))),
    http.get('*/api/v1/organizacion/terminales-pos', () =>
      HttpResponse.json(pagina([sampleTerminal]))
    )
  );
}

function renderPage() {
  return renderRoute(
    '/organizacion/establecimientos/:establecimientoId',
    EstablecimientoDetailPage,
    '/organizacion/establecimientos/est-1'
  );
}

describe('EstablecimientoDetailPage', () => {
  it('muestra los datos del establecimiento, sus almacenes y sus terminales', async () => {
    mockDefaultHandlers();

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Botica Central' })).toBeInTheDocument();
    expect(screen.getByText('EST001')).toBeInTheDocument();
    expect(screen.getByText('0001')).toBeInTheDocument();
    expect(screen.getByText('BOTICA')).toBeInTheDocument();
    expect(screen.getByText('ONLINE')).toBeInTheDocument();
    expect(screen.getByText('Sí')).toBeInTheDocument();
    expect(screen.getAllByText('No').length).toBeGreaterThan(0);
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'Organización / Empresa' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-1'
    );
    expect(await screen.findByText('Almacén Central')).toBeInTheDocument();
    expect(await screen.findByText('Caja 1')).toBeInTheDocument();
  });

  it('muestra las coordenadas cuando existen', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ ...sampleEstablecimiento, latitud: -12.0464, longitud: -77.0428 })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByText('-12.0464')).toBeInTheDocument();
    expect(screen.getByText('-77.0428')).toBeInTheDocument();
  });

  it('muestra el estado de carga inicial', () => {
    mockDefaultHandlers();

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('informa cuando el establecimiento no existe', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json(
          { title: 'Not Found', detail: 'El establecimiento indicado no existe.' },
          { status: 404 }
        )
      )
    );

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('edita el establecimiento sin enviar el código', async () => {
    mockDefaultHandlers();
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(detailUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEstablecimiento, nombre: 'Botica Principal' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    const nombre = screen.getByLabelText('Nombre');
    await user.clear(nombre);
    await user.type(nombre, 'Botica Principal');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.has('tenantId')).toBe(false);
    expect(body).toMatchObject({ nombre: 'Botica Principal', perfilOperacion: 'ONLINE' });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(
      http.put(detailUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'El nombre no es válido.' },
          { status: 400 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El nombre no es válido.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('cambia el estado del establecimiento', async () => {
    mockDefaultHandlers();
    let body: unknown;
    server.use(
      http.patch(statusUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'CLAUSURADO' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'CLAUSURADO');
    expect(screen.getByRole('note')).toHaveTextContent(
      'Mientras esté clausurado, el establecimiento no admite almacenes ni terminales POS nuevos.'
    );
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(body).toEqual({ estado: 'CLAUSURADO' });
  });

  it('muestra el error al cambiar el estado y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(
      http.patch(statusUrl, () => HttpResponse.json({ title: 'Forbidden' }, { status: 403 }))
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'REMODELACION');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No tienes permiso para esta acción.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('muestra correo y teléfono como enlaces', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({
          ...sampleEstablecimiento,
          email: 'botica@boticas.pe',
          telefono: '014445566'
        })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByRole('link', { name: 'botica@boticas.pe' })).toHaveAttribute(
      'href',
      'mailto:botica@boticas.pe'
    );
    expect(screen.getByRole('link', { name: '014445566' })).toHaveAttribute(
      'href',
      'tel:014445566'
    );
  });

  it('nombra el establecimiento en el título del cambio de estado', async () => {
    mockDefaultHandlers();
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));

    expect(
      screen.getByRole('heading', { name: 'Cambiar estado de Botica Central' })
    ).toBeInTheDocument();
  });

  it('avisa y bloquea las altas cuando el establecimiento está clausurado', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'CLAUSURADO' })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByRole('status')).toHaveTextContent(
      'El establecimiento está clausurado; no admite almacenes ni terminales POS nuevos.'
    );
    expect(screen.getByRole('button', { name: 'Nuevo almacén' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Nuevo terminal' })).toBeDisabled();
  });

  it('permite las altas en remodelación y no muestra aviso', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'REMODELACION' })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    await screen.findByRole('heading', { name: 'Botica Central' });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo almacén' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Nuevo terminal' })).toBeEnabled();
  });
});
