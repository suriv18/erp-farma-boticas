import type { ApiClient } from '@boticas/api-client';
import type { AuthTokenResponse, LoginRequest } from './auth-tokens.types';

export function login(client: ApiClient, request: LoginRequest): Promise<AuthTokenResponse> {
  return client.post<AuthTokenResponse, LoginRequest & { channel: 'WEB' }>('/auth/login', {
    ...request,
    channel: 'WEB'
  });
}

export function refresh(client: ApiClient, refreshToken: string): Promise<AuthTokenResponse> {
  return client.post<AuthTokenResponse, { refreshToken: string }>('/auth/refresh', {
    refreshToken
  });
}

export function logout(client: ApiClient): Promise<void> {
  return client.post<void, Record<string, never>>('/auth/logout', {});
}
