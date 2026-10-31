import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { miPuestoHandler } from '../../../test/mi-puesto-fixtures';
import { renderRoute } from '../../../test/render-route';
import { useMiPuesto } from './use-mi-puesto';

function Probe() {
  const { establecimientos, isPending, isError } = useMiPuesto();
  if (isPending) return <p>cargando</p>;
  if (isError) return <p>error</p>;
  return <p>{establecimientos.map(({ nombre }) => nombre).join(',')}</p>;
}

describe('useMiPuesto', () => {
  it('expone los establecimientos del puesto del usuario', async () => {
    server.use(miPuestoHandler());
    renderRoute('/x', Probe, '/x');

    expect(await screen.findByText('Botica Central,Botica Norte')).toBeInTheDocument();
  });

  it('expone el error cuando la consulta falla', async () => {
    server.use(http.get('*/api/v1/organizacion/mi-puesto', () => new HttpResponse(null, { status: 500 })));
    renderRoute('/x', Probe, '/x');

    expect(await screen.findByText('error')).toBeInTheDocument();
  });
});
