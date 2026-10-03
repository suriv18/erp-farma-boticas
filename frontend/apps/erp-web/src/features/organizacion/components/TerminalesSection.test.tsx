import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { TerminalesSection } from './TerminalesSection';

const listUrl = '*/api/v1/organizacion/terminales-pos';
const itemUrl = '*/api/v1/organizacion/terminales-pos/term-1';

function renderSection() {
  return renderRoute(
    '/establecimiento',
    () => <TerminalesSection establecimientoId="est-1" />,
    '/establecimiento'
  );
}

describe('TerminalesSection', () => {
  it('lista los terminales del establecimiento con IP y estado', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([
            sampleTerminal,
            {
              ...sampleTerminal,
              id: 'term-2',
              codigo: 'POS002',
              nombre: 'Caja 2',
              ipEquipo: '10.0.0.16',
              estado: 'BLOQUEADO'
            }
          ])
        );
      })
    );

    renderSection();

    expect(await screen.findByText('Caja 1')).toBeInTheDocument();
    expect(screen.getByText('POS001')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText('10.0.0.16')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByText('BLOQUEADO')).toBeInTheDocument();
    expect(received.get('establecimientoId')).toBe('est-1');
    expect(received.get('size')).toBe('100');
  });

  it('muestra carga, lista vacía y error de carga', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    const first = renderSection();
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('Este establecimiento aún no tiene terminales.')
    ).toBeInTheDocument();
    first.unmount();

    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderSection();
    expect(await screen.findByText('No se pudo cargar los terminales.')).toBeInTheDocument();
  });

  it('crea un terminal del establecimiento y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleTerminal] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleTerminal, { status: 201 });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene terminales.');

    await user.click(screen.getByRole('button', { name: 'Nuevo terminal' }));
    await user.type(screen.getByLabelText('Código'), 'POS001');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 1');
    await user.type(screen.getByLabelText('Dirección IP'), '10.0.0.15');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(await screen.findByText('Caja 1')).toBeInTheDocument();
    expect(created).toMatchObject({
      establecimientoId: 'est-1',
      codigo: 'POS001',
      nombre: 'Caja 1',
      ipEquipo: '10.0.0.15'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe un terminal con el código indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene terminales.');

    await user.click(screen.getByRole('button', { name: 'Nuevo terminal' }));
    await user.type(screen.getByLabelText('Código'), 'POS001');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 1');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un terminal con el código indicado.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nuevo terminal' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('edita un terminal precargando sus datos y permite cambiar su estado', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleTerminal]))),
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleTerminal, estado: 'MANTENIMIENTO' });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Caja 1');

    await user.click(screen.getByRole('button', { name: 'Editar Caja 1' }));
    const codigo = screen.getByLabelText('Código');
    expect(codigo).toHaveValue('POS001');
    expect(codigo).toHaveAttribute('readonly');
    await user.selectOptions(screen.getByLabelText('Estado'), 'MANTENIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.has('tenantId')).toBe(false);
    expect(body).toMatchObject({ nombre: 'Caja 1', estado: 'MANTENIMIENTO' });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleTerminal]))),
      http.put(itemUrl, () =>
        HttpResponse.json(
          { title: 'Bad Request', detail: 'El terminal no es válido.' },
          { status: 400 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Caja 1');

    await user.click(screen.getByRole('button', { name: 'Editar Caja 1' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El terminal no es válido.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar Caja 1' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('deshabilita el alta y explica el motivo cuando el establecimiento no admite altas', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    renderRoute(
      '/establecimiento',
      () => (
        <TerminalesSection
          establecimientoId="est-1"
          motivoSinAltas="El establecimiento está suspendido; no admite almacenes ni terminales POS nuevos."
        />
      ),
      '/establecimiento'
    );
    await screen.findByText('Este establecimiento aún no tiene terminales.');

    const boton = screen.getByRole('button', { name: 'Nuevo terminal' });
    expect(boton).toBeDisabled();
    expect(boton).toHaveAttribute(
      'title',
      'El establecimiento está suspendido; no admite almacenes ni terminales POS nuevos.'
    );
  });
});
