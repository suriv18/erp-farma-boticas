import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react';
import { apiClient, refreshApiClient } from '../../../app/api';
import {
  changePassword as changePasswordRequest,
  login as loginRequest,
  logout as logoutRequest,
  refresh as refreshRequest
} from '../api/auth.api';
import type { AuthTokenResponse, ChangePasswordRequest } from '../api/auth-tokens.types';
import type { LoginCredentials } from '../schemas/login.schema';
import { AuthSessionContext, type AuthSession, type AuthSessionStatus } from './auth-session.context';
import { clearRefreshToken, readRefreshToken, saveRefreshToken } from './session-storage';

export function AuthSessionProvider({ children }: PropsWithChildren) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const [passwordChangeRequired, setPasswordChangeRequired] = useState(false);
  const [status, setStatus] = useState<AuthSessionStatus>(() => (readRefreshToken() ? 'loading' : 'unauthenticated'));
  const pendingRefresh = useRef<Promise<string | null> | null>(null);
  const queryClient = useQueryClient();

  const resetPrivateCache = useCallback(async () => {
    await queryClient.cancelQueries();
    queryClient.clear();
  }, [queryClient]);

  const applyTokens = useCallback((response: AuthTokenResponse) => {
    setAccessToken(response.accessToken);
    setTenantId(response.tenantId);
    setUserId(response.userId);
    setPasswordChangeRequired(response.passwordChangeRequired);
    saveRefreshToken(response.refreshToken);
    setStatus('authenticated');
  }, []);

  const clearLocalSession = useCallback(async () => {
    setAccessToken(null);
    setTenantId(null);
    setUserId(null);
    setPasswordChangeRequired(false);
    clearRefreshToken();
    setStatus('unauthenticated');
    await resetPrivateCache();
  }, [resetPrivateCache]);

  const doRefresh = useCallback(async (): Promise<string | null> => {
    const storedRefreshToken = readRefreshToken();
    if (!storedRefreshToken) {
      setStatus('unauthenticated');
      return null;
    }

    try {
      const response = await refreshRequest(refreshApiClient, storedRefreshToken);
      applyTokens(response);
      return response.accessToken;
    } catch {
      await clearLocalSession();
      return null;
    }
  }, [applyTokens, clearLocalSession]);

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

  const authenticate = useCallback(
    async (credentials: LoginCredentials) => {
      const response = await loginRequest(apiClient, {
        login: credentials.email,
        password: credentials.password
      });
      await resetPrivateCache();
      applyTokens(response);
    },
    [applyTokens, resetPrivateCache]
  );

  const changePassword = useCallback(
    async (request: ChangePasswordRequest) => {
      await changePasswordRequest(apiClient, request);
      await clearLocalSession();
    },
    [clearLocalSession]
  );

  const signOut = useCallback(async () => {
    try {
      await logoutRequest(apiClient);
    } catch {
      // Logout es best-effort: la sesion local se limpia igual aunque falle la llamada remota.
    }
    await clearLocalSession();
  }, [clearLocalSession]);

  const session = useMemo<AuthSession>(
    () => ({
      status,
      authenticated: status === 'authenticated',
      accessToken,
      tenantId,
      userId,
      passwordChangeRequired,
      authenticate,
      changePassword,
      signOut
    }),
    [status, accessToken, tenantId, userId, passwordChangeRequired, authenticate, changePassword, signOut]
  );

  return <AuthSessionContext value={session}>{children}</AuthSessionContext>;
}
