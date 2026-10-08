import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { useRouteParam } from './use-route-param';

function Probe() {
  return <p>{useRouteParam('empresaId')}</p>;
}

describe('useRouteParam', () => {
  it('lee el parámetro de la ruta activa', () => {
    const router = createMemoryRouter([{ path: '/empresas/:empresaId', Component: Probe }], {
      initialEntries: ['/empresas/abc-123']
    });

    render(<RouterProvider router={router} />);

    expect(screen.getByText('abc-123')).toBeInTheDocument();
  });
});
