import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { ForbiddenPage } from './ForbiddenPage';

describe('ForbiddenPage', () => {
  it('explica la falta de acceso y ofrece volver al resumen', () => {
    render(
      <MemoryRouter>
        <ForbiddenPage />
      </MemoryRouter>
    );

    expect(
      screen.getByRole('heading', { name: 'No tienes acceso a esta sección' })
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir al resumen' })).toHaveAttribute(
      'href',
      '/dashboard'
    );
  });
});
