import { createApiClient } from '@boticas/api-client';
import { environment } from './environment';

export const apiClient = createApiClient({
  baseUrl: environment.VITE_API_BASE_URL,
  credentials: 'include',
  timeoutMs: 15_000
});
