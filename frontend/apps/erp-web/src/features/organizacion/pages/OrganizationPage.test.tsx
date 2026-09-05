import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { OrganizationPage } from './OrganizationPage';

describe('OrganizationPage', () => {
  it('renders the authorized corporate structure returned by the API', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    render(
      <QueryClientProvider client={queryClient}>
        <OrganizationPage />
      </QueryClientProvider>
    );

    expect(screen.getByRole('heading', { name: 'Organización' })).toBeInTheDocument();
    expect(await screen.findByText('Boticas Pacífico')).toBeInTheDocument();
    expect(screen.getByText('Botica Miraflores')).toBeInTheDocument();
    expect(screen.getByText(/Zona de cuarentena/u)).toBeInTheDocument();
    expect(screen.getByText('Caja principal')).toBeInTheDocument();
  });
});
