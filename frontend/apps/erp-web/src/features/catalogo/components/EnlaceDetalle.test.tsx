import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { EnlaceDetalle } from './EnlaceDetalle';

describe('EnlaceDetalle', () => {
  it('enlaza al detalle con una etiqueta accesible', () => {
    const router = createMemoryRouter([
      {
        path: '/',
        element: <EnlaceDetalle nombre="Paracetamol" href="/catalogo/productos-regulados/pr-1" />
      }
    ]);

    render(<RouterProvider router={router} />);

    expect(screen.getByRole('link', { name: 'Ver detalle de Paracetamol' })).toHaveAttribute(
      'href',
      '/catalogo/productos-regulados/pr-1'
    );
  });
});
