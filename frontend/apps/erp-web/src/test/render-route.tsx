import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentType } from 'react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext, type AuthSession } from '../features/auth/model/auth-session.context';

export const TEST_TENANT = 'tenant-1';

const session: AuthSession = {
  status: 'authenticated',
  authenticated: true,
  accessToken: 'token',
  tenantId: TEST_TENANT,
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

export function renderRoute(path: string, Component: ComponentType, initialEntry: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path, Component }], { initialEntries: [initialEntry] });
  return {
    user: userEvent.setup(),
    queryClient,
    router,
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={session}>
          <RouterProvider router={router} />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}
