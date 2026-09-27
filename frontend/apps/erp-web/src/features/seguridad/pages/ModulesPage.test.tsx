import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { ModulesPage } from './ModulesPage';

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ModulesPage />
    </QueryClientProvider>
  );
}

describe('ModulesPage', () => {
  it('lista los modulos del sistema', async () => {
    server.use(
      http.get('*/api/v1/modulos', () =>
        HttpResponse.json([
          { code: 'SEGURIDAD', name: 'Seguridad', description: 'Modulo IAM', order: 1, active: true }
        ])
      )
    );

    renderPage();

    expect(await screen.findByText('Seguridad')).toBeInTheDocument();
    expect(screen.getByText('SEGURIDAD')).toBeInTheDocument();
  });

  it('muestra un mensaje cuando no hay modulos', async () => {
    server.use(http.get('*/api/v1/modulos', () => HttpResponse.json([])));

    renderPage();

    expect(await screen.findByText('No se encontraron módulos.')).toBeInTheDocument();
  });

  it('muestra el guion y el estado inactivo cuando el modulo no tiene descripcion y esta inactivo', async () => {
    server.use(
      http.get('*/api/v1/modulos', () =>
        HttpResponse.json([{ code: 'CATALOGO', name: 'Catálogo', description: null, order: 2, active: false }])
      )
    );

    renderPage();

    expect(await screen.findByText('INACTIVO')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
