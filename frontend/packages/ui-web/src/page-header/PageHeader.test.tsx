import { render, screen } from '@testing-library/react';
import { PageHeader } from './PageHeader';

it('presenta la jerarquía y las acciones del módulo', () => {
  render(
    <PageHeader
      title="Productos"
      context="Catálogo"
      description="Productos disponibles"
      actions={<button>Nuevo producto</button>}
    />
  );
  expect(screen.getByRole('heading', { level: 1, name: 'Productos' })).toBeInTheDocument();
  expect(screen.getByText('Catálogo')).toBeInTheDocument();
  expect(screen.getByText('Productos disponibles')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Nuevo producto' })).toBeInTheDocument();
});
