import { useCallback, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react';
import { apiClient } from '../../../app/api';
import { login as loginRequest, logout as logoutRequest, refresh as refreshRequest } from '../api/auth.api';
import type { LoginCredentials } from '../schemas/login.schema';
import { AuthSessionContext, type AuthSession } from './auth-session.context';
import { clearRefreshToken, readRefreshToken, saveRefreshToken } from './session-storage';

export function AuthSessionProvider({ children }: PropsWithChildren) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const pendingRefresh = useRef<Promise<string | null> | null>(null);

  const performRefresh = useCallback(async (): Promise<string | null> => {
    const storedRefreshToken = readRefreshToken();
    if (!storedRefreshToken) return null;

    try {
      const response = await refreshRequest(apiClient, storedRefreshToken);
      setAccessToken(response.accessToken);
      saveRefreshToken(response.refreshToken);
      return response.accessToken;
    } catch {
      clearRefreshToken();
      setAccessToken(null);
      return null;
    }
  }, []);

  const onUnauthorized = useCallback((): Promise<string | null> => {
    if (!pendingRefresh.current) {
      pendingRefresh.current = performRefresh().finally(() => {
        pendingRefresh.current = null;
      });
    }
    return pendingRefresh.current;
  }, [performRefresh]);

  useEffect(() => {
    apiClient.setAuthHooks({
      getAccessToken: () => accessToken,
      onUnauthorized
    });
  }, [accessToken, onUnauthorized]);

  useEffect(() => {
    void performRefresh();
    // Solo se ejecuta al montar: restaura sesion desde el refresh token persistido, si existe.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const authenticate = useCallback(async (credentials: LoginCredentials) => {
    const response = await loginRequest(apiClient, {
      tenantId: credentials.tenantId,
      login: credentials.email,
      password: credentials.password
    });
    setAccessToken(response.accessToken);
    saveRefreshToken(response.refreshToken);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logoutRequest(apiClient);
    } catch {
      // Logout es best-effort: la sesion local se limpia igual aunque falle la llamada remota.
    }
    setAccessToken(null);
    clearRefreshToken();
  }, []);

  const session = useMemo<AuthSession>(
    () => ({
      authenticated: accessToken !== null,
      accessToken,
      authenticate,
      signOut
    }),
    [accessToken, authenticate, signOut]
  );

  return <AuthSessionContext value={session}>{children}</AuthSessionContext>;
}
