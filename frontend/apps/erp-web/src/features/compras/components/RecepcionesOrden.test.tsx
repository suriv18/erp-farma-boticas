import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleRecepcion } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { formatoFechaHora } from '../../../shared/lib/format';
import { formatoFecha } from '../lib/formato-compras';
import { RecepcionesOrden } from './RecepcionesOrden';

const recepcionesUrl = '*/api/v1/compras/recepciones';
const texto = (valor: string) => valor.replace(/\s/g, ' ');

function renderTabla() {
  const Pantalla = () => <RecepcionesOrden ordenId="orden-1" />;
  return renderRoute('/x', Pantalla, '/x');
}

describe('RecepcionesOrden', () => {
  it('consulta las recepciones de la orden y muestra una fila por línea recibida', async () => {
    let parametros: URLSearchParams | undefined;
    server.use(
      http.get(recepcionesUrl, ({ request }) => {
        parametros = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleRecepcion]));
      })
    );

    renderTabla();

    const fila = (await screen.findByText('REC-2026-000001')).closest('tr');
    const contenido = texto(fila?.textContent ?? '');
    expect(contenido).toContain(texto(formatoFechaHora('2026-10-08T15:00:00Z')));
    expect(contenido).toContain('L2026-01');
    expect(contenido).toContain(texto(formatoFecha('2028-12-31')));
    expect(parametros?.get('ordenCompraId')).toBe('orden-1');
    expect(parametros?.get('size')).toBe('100');
  });

  it('indica cuando la orden aún no tiene recepciones', async () => {
    server.use(http.get(recepcionesUrl, () => HttpResponse.json(pagina([]))));

    renderTabla();

    expect(await screen.findByText('La orden aún no tiene recepciones.')).toBeInTheDocument();
  });

  it('indica el error de consulta', async () => {
    server.use(
      http.get(recepcionesUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 }))
    );

    renderTabla();

    expect(
      await screen.findByText('No se pudo cargar las recepciones de la orden.')
    ).toBeInTheDocument();
  });
});
