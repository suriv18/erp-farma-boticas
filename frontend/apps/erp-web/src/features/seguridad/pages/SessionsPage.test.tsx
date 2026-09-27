import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { SessionsPage } from './SessionsPage';

const authenticatedSession = {
  status: 'authenticated' as const,
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <SessionsPage />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

const sampleSession = {
  id: 'sesion-1',
  userId: 'user-1',
  provider: 'local',
  authMethod: 'PASSWORD',
  channel: 'WEB',
  ipAddress: '127.0.0.1',
  userAgent: 'vitest',
  deviceId: null,
  loginAt: '2026-09-01T00:00:00Z',
  lastUsedAt: null,
  expiresAt: null,
  logoutAt: null,
  revokedAt: null,
  revocationReason: null,
  status: 'ACTIVA'
};

describe('SessionsPage', () => {
  it('lista las sesiones del tenant activo', async () => {
    server.use(http.get('*/api/v1/sesiones', () => HttpResponse.json([sampleSession])));

    renderPage();

    expect(await screen.findByText('WEB')).toBeInTheDocument();
    expect(screen.getByText('127.0.0.1')).toBeInTheDocument();
  });

  it('revoca una sesion activa tras confirmar', async () => {
    let revoked = false;
    server.use(
      http.get('*/api/v1/sesiones', () =>
        HttpResponse.json([revoked ? { ...sampleSession, status: 'REVOCADA' } : sampleSession])
      ),
      http.post('*/api/v1/sesiones/sesion-1/revocacion', () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('ACTIVA')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Revocar' }));
    await user.click(screen.getByRole('button', { name: 'Revocar sesión' }));

    expect(await screen.findByText('REVOCADA')).toBeInTheDocument();
  });

  it('cierra el dialogo de confirmacion al cancelar', async () => {
    server.use(http.get('*/api/v1/sesiones', () => HttpResponse.json([sampleSession])));

    const { user } = renderPage();
    await user.click(await screen.findByRole('button', { name: 'Revocar' }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('button', { name: 'Revocar sesión' })).not.toBeInTheDocument();
  });
});
