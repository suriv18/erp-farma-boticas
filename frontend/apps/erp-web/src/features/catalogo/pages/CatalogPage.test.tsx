import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { CatalogPage } from './CatalogPage';

function renderPage() {
  const router = createMemoryRouter([{ path: '/', Component: CatalogPage }]);
  return render(<RouterProvider router={router} />);
}

describe('CatalogPage', () => {
  it('combina búsqueda y grupo y permite restablecer ambos filtros', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Organización comercial' }));
    expect(screen.getByRole('button', { name: 'Organización comercial' })).toHaveAttribute(
      'aria-pressed',
      'true'
    );
    expect(screen.getByRole('status')).toHaveTextContent('3 de 12 catálogos');
    expect(screen.queryByRole('heading', { name: 'SKU comerciales' })).not.toBeInTheDocument();
    await user.type(screen.getByRole('searchbox', { name: 'Buscar catálogo' }), 'marcas');
    expect(screen.getByRole('status')).toHaveTextContent('1 de 12 catálogos');
    await user.click(screen.getByRole('button', { name: 'Catálogos de soporte' }));
    expect(
      screen.getByRole('heading', { name: 'No se encontraron catálogos' })
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Ver todos los catálogos' }));
    expect(screen.getByRole('searchbox', { name: 'Buscar catálogo' })).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Todos' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('12 de 12 catálogos');
  });

  it('busca sin distinguir tildes y permite recuperar todos los catálogos', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByRole('searchbox', { name: 'Buscar catálogo' }), '  categorias  ');
    expect(screen.getByRole('heading', { name: 'Categorías' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Marcas' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Limpiar búsqueda' }));
    expect(screen.getByRole('heading', { name: 'Marcas' })).toBeInTheDocument();
  });

  it('orienta al usuario cuando la búsqueda no tiene resultados', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByRole('searchbox', { name: 'Buscar catálogo' }), 'inexistente');
    expect(
      screen.getByRole('heading', { name: 'No se encontraron catálogos' })
    ).toBeInTheDocument();
  });

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

  it.each([
    ['Principios activos', '/catalogo/principios-activos'],
    ['Productos regulados', '/catalogo/productos-regulados'],
    ['SKU comerciales', '/catalogo/skus']
  ])('enlaza a %s', (titulo, href) => {
    renderPage();

    expect(screen.getByRole('heading', { name: titulo }).closest('a')).toHaveAttribute(
      'href',
      href
    );
  });
});
