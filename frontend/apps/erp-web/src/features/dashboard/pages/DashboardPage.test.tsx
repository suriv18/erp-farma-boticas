import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { DashboardPage } from './DashboardPage';

describe('DashboardPage', () => {
  it('shows the operational summary returned by the API', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    render(
      <QueryClientProvider client={queryClient}>
        <DashboardPage />
      </QueryClientProvider>
    );

    expect(screen.getByRole('heading', { name: 'Buenos días, María' })).toBeInTheDocument();
    expect(await screen.findByText(/8,420\.50/u)).toBeInTheDocument();
  });
});
