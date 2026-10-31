import { ApiError } from './api-error';
import type { AuthHooks } from './auth-hooks';
import type { ProblemDetails } from './problem-details';

export type ApiClientConfig = {
  baseUrl: string;
  credentials?: RequestCredentials;
  timeoutMs?: number;
};

export type ApiClient = {
  get<T>(path: string, init?: RequestInit): Promise<T>;
  post<TResponse, TBody>(path: string, body: TBody, init?: RequestInit): Promise<TResponse>;
  put<TResponse, TBody>(path: string, body: TBody, init?: RequestInit): Promise<TResponse>;
  patch<TResponse, TBody>(path: string, body: TBody, init?: RequestInit): Promise<TResponse>;
  delete<T>(path: string, init?: RequestInit): Promise<T>;
  setAuthHooks(hooks: AuthHooks | null): void;
};

function resolveUrl(baseUrl: string, path: string): string {
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${normalizedBase}${normalizedPath}`;

  if (/^https?:\/\//u.test(url)) return url;
  if (typeof window !== 'undefined') return new URL(url, window.location.origin).toString();
  return url;
}

export function createApiClient(config: ApiClientConfig): ApiClient {
  const credentials = config.credentials ?? 'include';
  const timeoutMs = config.timeoutMs ?? 15_000;
  let authHooks: AuthHooks | null = null;

  async function performFetch(path: string, init: RequestInit): Promise<Response> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    const callerSignal = init.signal;
    const abortFromCaller = () => controller.abort(callerSignal?.reason);

    if (callerSignal?.aborted) abortFromCaller();
    else callerSignal?.addEventListener('abort', abortFromCaller, { once: true });

    const headers = new Headers(init.headers);
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');

    const accessToken = authHooks?.getAccessToken();
    if (accessToken && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${accessToken}`);
    }

    try {
      return await fetch(resolveUrl(config.baseUrl, path), {
        ...init,
        credentials,
        signal: controller.signal,
        headers
      });
    } finally {
      clearTimeout(timeout);
      callerSignal?.removeEventListener('abort', abortFromCaller);
    }
  }

  async function toApiError(response: Response): Promise<ApiError> {
    const problem = (await response.json().catch(() => undefined)) as ProblemDetails | undefined;
    return new ApiError(
      problem?.detail ?? problem?.title ?? 'La solicitud no pudo completarse.',
      response.status,
      problem
    );
  }

  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    let response = await performFetch(path, init);

    if (response.status === 401 && authHooks) {
      const newToken = await authHooks.onUnauthorized();
      if (newToken) {
        const retryHeaders = new Headers(init.headers);
        retryHeaders.set('Authorization', `Bearer ${newToken}`);
        response = await performFetch(path, { ...init, headers: retryHeaders });
      }
    }

    if (!response.ok) throw await toApiError(response);
    if (response.status === 204) return undefined as T;
    return (await response.json()) as T;
  }

  function sendJson<TResponse, TBody>(
    method: 'POST' | 'PUT' | 'PATCH',
    path: string,
    body: TBody,
    init?: RequestInit
  ): Promise<TResponse> {
    const headers = new Headers(init?.headers);
    if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

    return request<TResponse>(path, {
      ...init,
      method,
      headers,
      body: JSON.stringify(body)
    });
  }

  return {
    get: <T>(path: string, init?: RequestInit) => request<T>(path, init),
    post: <TResponse, TBody>(path: string, body: TBody, init?: RequestInit) =>
      sendJson<TResponse, TBody>('POST', path, body, init),
    put: <TResponse, TBody>(path: string, body: TBody, init?: RequestInit) =>
      sendJson<TResponse, TBody>('PUT', path, body, init),
    patch: <TResponse, TBody>(path: string, body: TBody, init?: RequestInit) =>
      sendJson<TResponse, TBody>('PATCH', path, body, init),
    delete: <T>(path: string, init?: RequestInit) => request<T>(path, { ...init, method: 'DELETE' }),
    setAuthHooks(hooks: AuthHooks | null) {
      authHooks = hooks;
    }
  };
}
