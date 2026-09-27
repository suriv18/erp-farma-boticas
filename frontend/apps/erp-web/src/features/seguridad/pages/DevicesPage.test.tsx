import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { DevicesPage } from './DevicesPage';

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
          <DevicesPage />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

const sampleDevice = {
  id: 'device-1',
  companyId: 'company-1',
  establishmentId: 'establishment-1',
  terminalId: null,
  fingerprintHash: 'abc',
  certificateThumbprint: null,
  agentVersion: '1.0.0',
  status: 'PENDIENTE',
  registeredAt: '2026-09-01T00:00:00Z',
  lastContactAt: null
};

describe('DevicesPage', () => {
  it('lista los dispositivos del tenant activo', async () => {
    server.use(http.get('*/api/v1/dispositivos', () => HttpResponse.json([sampleDevice])));

    renderPage();

    expect(await screen.findByText('1.0.0')).toBeInTheDocument();
    expect(screen.getByText('PENDIENTE')).toBeInTheDocument();
  });

  it('marca un dispositivo pendiente como confiable', async () => {
    let status = 'PENDIENTE';
    server.use(
      http.get('*/api/v1/dispositivos', () => HttpResponse.json([{ ...sampleDevice, status }])),
      http.patch('*/api/v1/dispositivos/device-1/estado', () => {
        status = 'CONFIABLE';
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('PENDIENTE')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Marcar confiable' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar' }));

    expect(await screen.findByText('CONFIABLE')).toBeInTheDocument();
  });

  it('bloquea un dispositivo confiable', async () => {
    let status = 'CONFIABLE';
    server.use(
      http.get('*/api/v1/dispositivos', () => HttpResponse.json([{ ...sampleDevice, status }])),
      http.patch('*/api/v1/dispositivos/device-1/estado', () => {
        status = 'BLOQUEADO';
        return new HttpResponse(null, { status: 204 });
      })
    );

    const { user } = renderPage();
    expect(await screen.findByText('CONFIABLE')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Bloquear' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar' }));

    expect(await screen.findByText('BLOQUEADO')).toBeInTheDocument();
  });

  it('cierra el dialogo de confirmacion al cancelar', async () => {
    server.use(http.get('*/api/v1/dispositivos', () => HttpResponse.json([sampleDevice])));

    const { user } = renderPage();
    await user.click(await screen.findByRole('button', { name: 'Marcar confiable' }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('button', { name: 'Confirmar' })).not.toBeInTheDocument();
  });
});
