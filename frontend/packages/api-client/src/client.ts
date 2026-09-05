import { ApiError } from './api-error';
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

  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    const callerSignal = init.signal;
    const abortFromCaller = () => controller.abort(callerSignal?.reason);

    if (callerSignal?.aborted) abortFromCaller();
    else callerSignal?.addEventListener('abort', abortFromCaller, { once: true });

    const headers = new Headers(init.headers);
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');

    try {
      const response = await fetch(resolveUrl(config.baseUrl, path), {
        ...init,
        credentials,
        signal: controller.signal,
        headers
      });

      if (!response.ok) {
        const problem = (await response.json().catch(() => undefined)) as
          ProblemDetails | undefined;
        throw new ApiError(
          problem?.detail ?? problem?.title ?? 'La solicitud no pudo completarse.',
          response.status,
          problem
        );
      }

      if (response.status === 204) return undefined as T;
      return (await response.json()) as T;
    } finally {
      clearTimeout(timeout);
      callerSignal?.removeEventListener('abort', abortFromCaller);
    }
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
    delete: <T>(path: string, init?: RequestInit) => request<T>(path, { ...init, method: 'DELETE' })
  };
}
