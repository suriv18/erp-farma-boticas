import { useCallback, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react';
import { apiClient, refreshApiClient } from '../../../app/api';
import { login as loginRequest, logout as logoutRequest, refresh as refreshRequest } from '../api/auth.api';
import type { LoginCredentials } from '../schemas/login.schema';
import { AuthSessionContext, type AuthSession } from './auth-session.context';
import { clearRefreshToken, readRefreshToken, saveRefreshToken } from './session-storage';

export function AuthSessionProvider({ children }: PropsWithChildren) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const pendingRefresh = useRef<Promise<string | null> | null>(null);

  const doRefresh = useCallback(async (): Promise<string | null> => {
    const storedRefreshToken = readRefreshToken();
    if (!storedRefreshToken) return null;

    try {
      const response = await refreshRequest(refreshApiClient, storedRefreshToken);
      setAccessToken(response.accessToken);
      setTenantId(response.tenantId);
      setUserId(response.userId);
      saveRefreshToken(response.refreshToken);
      return response.accessToken;
    } catch {
      clearRefreshToken();
      setAccessToken(null);
      setTenantId(null);
      setUserId(null);
      return null;
    }
  }, []);

  const performRefresh = useCallback((): Promise<string | null> => {
    if (!pendingRefresh.current) {
      pendingRefresh.current = doRefresh().finally(() => {
        pendingRefresh.current = null;
      });
    }
    return pendingRefresh.current;
  }, [doRefresh]);

  const onUnauthorized = useCallback((): Promise<string | null> => {
    return performRefresh();
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
      login: credentials.email,
      password: credentials.password
    });
    setAccessToken(response.accessToken);
    setTenantId(response.tenantId);
    setUserId(response.userId);
    saveRefreshToken(response.refreshToken);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await logoutRequest(apiClient);
    } catch {
      // Logout es best-effort: la sesion local se limpia igual aunque falle la llamada remota.
    }
    setAccessToken(null);
    setTenantId(null);
    setUserId(null);
    clearRefreshToken();
  }, []);

  const session = useMemo<AuthSession>(
    () => ({
      authenticated: accessToken !== null,
      accessToken,
      tenantId,
      userId,
      authenticate,
      signOut
    }),
    [accessToken, tenantId, userId, authenticate, signOut]
  );

  return <AuthSessionContext value={session}>{children}</AuthSessionContext>;
}
