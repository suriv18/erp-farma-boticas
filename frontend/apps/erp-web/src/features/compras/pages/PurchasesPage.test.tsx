import { screen } from '@testing-library/react';
import { renderRoute } from '../../../test/render-route';
import { PurchasesPage } from './PurchasesPage';

describe('PurchasesPage', () => {
  it('muestra el hub con el acceso a proveedores', () => {
    renderRoute('/compras', PurchasesPage, '/compras');

    expect(screen.getByRole('heading', { name: 'Compras' })).toBeInTheDocument();
    expect(screen.getByText('Proveedores, órdenes y recepción de mercadería.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Proveedores/ })).toHaveAttribute(
      'href',
      '/compras/proveedores'
    );
  });

  it('muestra también el acceso a las órdenes de compra', () => {
    renderRoute('/compras', PurchasesPage, '/compras');

    expect(screen.getByRole('link', { name: /Órdenes de compra/ })).toHaveAttribute(
      'href',
      '/compras/ordenes'
    );
  });
});
