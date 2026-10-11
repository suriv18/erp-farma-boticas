import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { act } from 'react';
import { apiClient } from '../../../app/api';
import { useAuthSession } from './useAuthSession';
import { AuthSessionProvider } from './AuthSessionProvider';
import { readRefreshToken, saveRefreshToken } from './session-storage';

const server = setupServer();
beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  sessionStorage.clear();
  apiClient.setAuthHooks(null);
});
afterAll(() => server.close());

const tokenResponse = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  tokenType: 'Bearer',
  accessExpiresAt: '2026-09-05T10:10:00Z',
  refreshExpiresAt: '2026-09-12T10:00:00Z',
  tenantId: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  sessionId: '33333333-3333-3333-3333-333333333333',
  passwordChangeRequired: false
};

function Probe() {
  const session = useAuthSession();
  return (
    <div>
      <span data-testid="authenticated">{String(session.authenticated)}</span>
      <span data-testid="tenantId">{session.tenantId}</span>
      <span data-testid="userId">{session.userId}</span>
      <button onClick={() => {
        void session.authenticate({
          email: 'admin@boticas.pe',
          password: 'Boticas2026!',
          remember: false
        });
      }}>
        login
      </button>
      <button onClick={() => {
        void session.signOut();
      }}>logout</button>
    </div>
  );
}

function renderProvider(queryClient = new QueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionProvider>
        <Probe />
      </AuthSessionProvider>
    </QueryClientProvider>
  );
}

describe('AuthSessionProvider', () => {
  it('autentica exitosamente y guarda el refresh token en sessionStorage', async () => {
    server.use(http.post('http://localhost/api/v1/auth/login', () => HttpResponse.json(tokenResponse)));

    renderProvider();

    act(() => {
      screen.getByRole('button', { name: 'login' }).click();
    });

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));
    expect(readRefreshToken()).toBe('refresh-1');
  });

  it('expone tenantId y userId tras autenticar', async () => {
    server.use(
      http.post('http://localhost/api/v1/auth/login', () =>
        HttpResponse.json({
          accessToken: 'access-1',
          refreshToken: 'refresh-1',
          tokenType: 'Bearer',
          accessExpiresAt: '2026-09-05T10:10:00Z',
          refreshExpiresAt: '2026-09-12T10:00:00Z',
          tenantId: 'tenant-abc',
          userId: 'user-xyz',
          sessionId: 'session-1',
          passwordChangeRequired: false
        })
      )
    );

    renderProvider();

    act(() => {
      screen.getByRole('button', { name: 'login' }).click();
    });

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));
    expect(screen.getByTestId('tenantId')).toHaveTextContent('tenant-abc');
    expect(screen.getByTestId('userId')).toHaveTextContent('user-xyz');
  });

  it('restaura la sesion al montar si hay un refresh token guardado', async () => {
    saveRefreshToken('refresh-existing');
    server.use(http.post('http://localhost/api/v1/auth/refresh', () => HttpResponse.json(tokenResponse)));

    renderProvider();

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));
  });

  it('no autentica al montar si el refresh guardado ya no es valido', async () => {
    saveRefreshToken('refresh-invalid');
    server.use(http.post('http://localhost/api/v1/auth/refresh', () => HttpResponse.json({}, { status: 401 })));
    const queryClient = new QueryClient();
    queryClient.setQueryData(['ventas', 'lista'], 'datos-privados');

    renderProvider(queryClient);

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
      expect(readRefreshToken()).toBeNull();
    });
    await waitFor(() => expect(queryClient.getQueryData(['ventas', 'lista'])).toBeUndefined());
  });

  it('signOut limpia la sesion y el refresh token', async () => {
    server.use(
      http.post('http://localhost/api/v1/auth/login', () => HttpResponse.json(tokenResponse)),
      http.post('http://localhost/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 }))
    );

    renderProvider();

    act(() => {
      screen.getByRole('button', { name: 'login' }).click();
    });
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));

    act(() => {
      screen.getByRole('button', { name: 'logout' }).click();
    });

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
    expect(readRefreshToken()).toBeNull();
  });

  it('signOut elimina la cache de consultas privadas', async () => {
    server.use(
      http.post('http://localhost/api/v1/auth/login', () => HttpResponse.json(tokenResponse)),
      http.post('http://localhost/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 }))
    );
    const queryClient = new QueryClient();

    renderProvider(queryClient);

    act(() => {
      screen.getByRole('button', { name: 'login' }).click();
    });
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));
    queryClient.setQueryData(['ventas', 'lista'], 'datos-cuenta-1');

    act(() => {
      screen.getByRole('button', { name: 'logout' }).click();
    });

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
    await waitFor(() => expect(queryClient.getQueryData(['ventas', 'lista'])).toBeUndefined());
  });

  it('un nuevo login no hereda la cache de la sesion anterior ni acepta respuestas pendientes', async () => {
    server.use(http.post('http://localhost/api/v1/auth/login', () => HttpResponse.json(tokenResponse)));
    const queryClient = new QueryClient();
    queryClient.setQueryData(['ventas', 'lista'], 'datos-cuenta-1');
    let release: (value: string) => void = () => undefined;
    const pending = queryClient.fetchQuery({
      queryKey: ['ventas', 'detalle', 'v1'],
      queryFn: () => new Promise<string>((resolve) => { release = resolve; })
    });
    pending.catch(() => undefined);

    renderProvider(queryClient);

    act(() => {
      screen.getByRole('button', { name: 'login' }).click();
    });
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));

    release('respuesta-tardia-cuenta-1');
    expect(queryClient.getQueryData(['ventas', 'lista'])).toBeUndefined();
    expect(queryClient.getQueryData(['ventas', 'detalle', 'v1'])).toBeUndefined();
  });
});
