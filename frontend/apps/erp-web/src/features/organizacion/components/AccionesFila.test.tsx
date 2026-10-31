import { screen } from '@testing-library/react';
import { renderRoute } from '../../../test/render-route';
import { AccionesFila } from './AccionesFila';

function renderAcciones(onEditar = vi.fn()) {
  const result = renderRoute(
    '/lista',
    () => (
      <AccionesFila
        nombre="Boticas SAC"
        detalleHref="/organizacion/empresas/empresa-1"
        onEditar={onEditar}
      />
    ),
    '/lista'
  );
  return { onEditar, ...result };
}

describe('AccionesFila', () => {
  it('muestra un enlace accesible al detalle', () => {
    renderAcciones();

    const enlace = screen.getByRole('link', { name: 'Ver detalle de Boticas SAC' });
    expect(enlace).toHaveAttribute('href', '/organizacion/empresas/empresa-1');
    expect(enlace).toHaveAttribute('title', 'Ver detalle de Boticas SAC');
  });

  it('muestra un botón de editar que llama a onEditar sin navegar', async () => {
    const { onEditar, user, router } = renderAcciones();

    await user.click(screen.getByRole('button', { name: 'Editar Boticas SAC' }));

    expect(onEditar).toHaveBeenCalledOnce();
    expect(router.state.location.pathname).toBe('/lista');
  });
});
