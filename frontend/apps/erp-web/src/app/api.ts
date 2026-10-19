import { createApiClient } from '@boticas/api-client';
import { environment } from './environment';

export const apiClient = createApiClient({
  baseUrl: environment.VITE_API_BASE_URL,
  credentials: 'include',
  timeoutMs: 15_000
});

// Cliente sin auth hooks: la llamada de refresh no debe disparar su propio reintento en 401.
export const refreshApiClient = createApiClient({
  baseUrl: environment.VITE_API_BASE_URL,
  credentials: 'include',
  timeoutMs: 15_000
});
