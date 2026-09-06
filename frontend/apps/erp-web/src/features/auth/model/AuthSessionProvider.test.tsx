import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { act } from 'react';
import { apiClient } from '../../../app/api';
import { useAuthSession } from './useAuthSession';
import { AuthSessionProvider } from './AuthSessionProvider';
import { clearRefreshToken, readRefreshToken, saveRefreshToken } from './session-storage';

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
      <button onClick={() => session.authenticate({
        tenantId: '11111111-1111-1111-1111-111111111111',
        email: 'admin@boticas.pe',
        password: 'Boticas2026!',
        remember: false
      })}>
        login
      </button>
      <button onClick={() => session.signOut()}>logout</button>
    </div>
  );
}

describe('AuthSessionProvider', () => {
  it('autentica exitosamente y guarda el refresh token en sessionStorage', async () => {
    server.use(http.post('http://localhost/api/v1/auth/login', () => HttpResponse.json(tokenResponse)));

    render(
      <AuthSessionProvider>
        <Probe />
      </AuthSessionProvider>
    );

    await act(async () => {
      screen.getByRole('button', { name: 'login' }).click();
    });

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));
    expect(readRefreshToken()).toBe('refresh-1');
  });

  it('restaura la sesion al montar si hay un refresh token guardado', async () => {
    saveRefreshToken('refresh-existing');
    server.use(http.post('http://localhost/api/v1/auth/refresh', () => HttpResponse.json(tokenResponse)));

    render(
      <AuthSessionProvider>
        <Probe />
      </AuthSessionProvider>
    );

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));
  });

  it('no autentica al montar si el refresh guardado ya no es valido', async () => {
    saveRefreshToken('refresh-invalid');
    server.use(http.post('http://localhost/api/v1/auth/refresh', () => HttpResponse.json({}, { status: 401 })));

    render(
      <AuthSessionProvider>
        <Probe />
      </AuthSessionProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
      expect(readRefreshToken()).toBeNull();
    });
  });

  it('signOut limpia la sesion y el refresh token', async () => {
    server.use(
      http.post('http://localhost/api/v1/auth/login', () => HttpResponse.json(tokenResponse)),
      http.post('http://localhost/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 }))
    );

    render(
      <AuthSessionProvider>
        <Probe />
      </AuthSessionProvider>
    );

    await act(async () => {
      screen.getByRole('button', { name: 'login' }).click();
    });
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));

    await act(async () => {
      screen.getByRole('button', { name: 'logout' }).click();
    });

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
    expect(readRefreshToken()).toBeNull();
  });
});
