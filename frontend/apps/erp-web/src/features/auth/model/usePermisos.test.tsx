import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { renderRoute } from '../../../test/render-route';
import { usePermisos } from './usePermisos';

function Probe() {
  const { puede } = usePermisos();
  return <p>{puede('ventas.turnos.') ? 'puede' : 'no puede'}</p>;
}

describe('usePermisos', () => {
  it('evalúa los permisos del perfil autenticado', async () => {
    server.use(
      http.get('*/api/v1/auth/me', () =>
        HttpResponse.json({
          userId: 'user-1',
          tenantId: 'tenant-1',
          displayName: 'Luis',
          username: 'luis',
          email: null,
          assignments: [],
          permissions: ['ventas.turnos.abrir']
        })
      )
    );
    renderRoute('/x', Probe, '/x');

    expect(await screen.findByText('puede')).toBeInTheDocument();
  });

  it('no concede nada mientras el perfil no ha cargado', () => {
    renderRoute('/x', Probe, '/x');

    expect(screen.getByText('no puede')).toBeInTheDocument();
  });
});
