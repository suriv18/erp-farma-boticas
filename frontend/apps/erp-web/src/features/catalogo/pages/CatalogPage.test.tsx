import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { CatalogPage } from './CatalogPage';

function renderPage() {
  const router = createMemoryRouter([{ path: '/', Component: CatalogPage }]);
  return render(<RouterProvider router={router} />);
}

describe('CatalogPage', () => {
  it('enlaza a los submodulos de marcas y categorias', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Marcas' }).closest('a')).toHaveAttribute(
      'href',
      '/catalogo/marcas'
    );
    expect(screen.getByRole('heading', { name: 'Categorías' }).closest('a')).toHaveAttribute(
      'href',
      '/catalogo/categorias'
    );
  });
});
