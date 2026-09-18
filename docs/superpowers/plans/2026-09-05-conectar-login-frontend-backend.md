# Conectar login real del frontend con el backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el login simulado del frontend (`erp-web`) por una integración real contra `POST /api/v1/auth/login` del backend, con renovación automática de sesión vía `/auth/refresh` en cada 401, y logout real.

**Architecture:** El paquete `@boticas/api-client` gana hooks de autenticación inyectables (`getAccessToken`, `onUnauthorized`) sin depender de React. La feature `auth` de `erp-web` implementa esos hooks sobre un estado de sesión real (`accessToken` en memoria, `refreshToken` en `sessionStorage`), y los registra en el `apiClient` singleton vía un método `setAuthHooks` de *late binding* para resolver la dependencia circular entre el módulo `app/api.ts` (creado fuera de React) y el contexto de React que conoce el estado de sesión.

**Tech Stack:** React 19, TypeScript, Zod, React Hook Form, MSW (tests), Vitest, Testing Library.

## Global Constraints

- El backend expone `POST /api/v1/auth/login` con body `{tenantId, login, password, channel}` y responde `AuthTokenResponse`: `accessToken`, `refreshToken`, `tokenType`, `accessExpiresAt`, `refreshExpiresAt`, `tenantId`, `userId`, `sessionId`, `passwordChangeRequired` (ver `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/response/AuthTokenResponse.java`).
- `channel` es fijo `"WEB"` para `erp-web` — no hay UI de selección de canal.
- El `accessToken` se guarda **solo en memoria** (nunca en `localStorage`/`sessionStorage`).
- El `refreshToken` se guarda en `sessionStorage` — **decisión de seguridad documentada como deuda técnica**: OWASP recomienda cookie `HttpOnly`+`Secure`, pero eso requeriría cambios en el backend (`LocalAuthController` emite el refresh token en el body JSON, no como cookie), fuera de alcance de este plan.
- `@boticas/api-client` no debe importar React ni depender de la feature `auth` — los hooks de autenticación son un tipo genérico inyectado vía config.
- No se toca el backend en este plan.
- No se implementa: recuperación de contraseña real, CRUD de usuarios/roles, selector de empresa/sucursal, endpoint de resolución de código de organización a tenantId.

---

### Task 1: Hooks de autenticación inyectables en `api-client`

**Files:**
- Create: `frontend/packages/api-client/src/auth-hooks.ts`
- Modify: `frontend/packages/api-client/src/client.ts`
- Modify: `frontend/packages/api-client/src/index.ts`
- Test: `frontend/packages/api-client/src/client.test.ts`

**Interfaces:**
- Produces: `AuthHooks` type y el nuevo comportamiento de `ApiClient` — expone `setAuthHooks(hooks: AuthHooks | null): void`, usado por la Task 4.

- [ ] **Step 1: Escribir el test que falla — adjunta `Authorization` cuando hay hooks**

Editar `frontend/packages/api-client/src/client.test.ts`, agregando dentro del `describe('createApiClient', ...)`:

```ts
describe('createApiClient with auth hooks', () => {
  it('adjunta el header Authorization cuando getAccessToken devuelve un token', async () => {
    let receivedAuthHeader: string | null = null;
    server.use(
      http.get('http://localhost/api/v1/protected', ({ request }) => {
        receivedAuthHeader = request.headers.get('authorization');
        return HttpResponse.json({ ok: true });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    client.setAuthHooks({
      getAccessToken: () => 'token-123',
      onUnauthorized: async () => null
    });

    await client.get('/protected');
    expect(receivedAuthHeader).toBe('Bearer token-123');
  });

  it('reintenta la request una vez cuando recibe 401 y onUnauthorized devuelve un nuevo token', async () => {
    let attempt = 0;
    server.use(
      http.get('http://localhost/api/v1/protected', ({ request }) => {
        attempt += 1;
        const auth = request.headers.get('authorization');
        if (attempt === 1) return HttpResponse.json({}, { status: 401 });
        if (auth === 'Bearer refreshed-token') return HttpResponse.json({ ok: true });
        return HttpResponse.json({}, { status: 401 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    client.setAuthHooks({
      getAccessToken: () => 'expired-token',
      onUnauthorized: async () => 'refreshed-token'
    });

    await expect(client.get('/protected')).resolves.toEqual({ ok: true });
    expect(attempt).toBe(2);
  });

  it('relanza el 401 original cuando onUnauthorized no puede renovar el token', async () => {
    server.use(
      http.get('http://localhost/api/v1/protected', () => HttpResponse.json({}, { status: 401 }))
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    client.setAuthHooks({
      getAccessToken: () => 'expired-token',
      onUnauthorized: async () => null
    });

    await expect(client.get('/protected')).rejects.toMatchObject({ status: 401 });
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/api-client test -- client.test.ts`
Expected: FAIL — `client.setAuthHooks is not a function`.

- [ ] **Step 3: Crear `auth-hooks.ts`**

```ts
// frontend/packages/api-client/src/auth-hooks.ts
export type AuthHooks = {
  getAccessToken: () => string | null;
  onUnauthorized: () => Promise<string | null>;
};
```

- [ ] **Step 4: Implementar `setAuthHooks` y la lógica de Authorization/retry en `client.ts`**

Reemplazar el contenido completo de `frontend/packages/api-client/src/client.ts` por:

```ts
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
```

Nota: `performFetch` ya no lanza `ApiError` internamente (antes lo hacía en el primer intento) — ahora `request()` decide si reintentar tras un 401 antes de convertir la respuesta final en error. Esto es un cambio de comportamiento interno intencional; el contrato público (`ApiError` en fallos no-401 o 401 sin recuperación) se mantiene igual.

- [ ] **Step 5: Exportar `AuthHooks` desde el índice del paquete**

Editar `frontend/packages/api-client/src/index.ts`, agregar junto a las demás exportaciones:

```ts
export type { AuthHooks } from './auth-hooks';
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/api-client test -- client.test.ts`
Expected: PASS — los 3 tests nuevos y los 2 existentes (`returns parsed JSON...`, `sends JSON commands...`) en verde.

- [ ] **Step 7: Typecheck del paquete**

Run: `pnpm --filter @boticas/api-client typecheck`
Expected: sin errores.

- [ ] **Step 8: Commit**

```bash
git add frontend/packages/api-client/src/auth-hooks.ts frontend/packages/api-client/src/client.ts frontend/packages/api-client/src/index.ts frontend/packages/api-client/src/client.test.ts
git commit -m "feat(api-client): agregar hooks de autenticacion con retry en 401"
```

---

### Task 2: Cliente de API de autenticación en `erp-web`

**Files:**
- Create: `frontend/apps/erp-web/src/features/auth/api/auth-tokens.types.ts`
- Create: `frontend/apps/erp-web/src/features/auth/api/auth.api.ts`
- Test: `frontend/apps/erp-web/src/features/auth/api/auth.api.test.ts`

**Interfaces:**
- Consumes: `apiClient` desde `frontend/apps/erp-web/src/app/api.ts` (import directo — este archivo NO se importa desde `app/api.ts`, evitando el ciclo; ver Task 4).
- Produces: `AuthTokenResponse` type, `login()`, `refresh()`, `logout()` — consumidos por la Task 3.

- [ ] **Step 1: Escribir el test que falla**

Crear `frontend/apps/erp-web/src/features/auth/api/auth.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { login, logout, refresh } from './auth.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleResponse = {
  accessToken: 'access-123',
  refreshToken: 'refresh-123',
  tokenType: 'Bearer',
  accessExpiresAt: '2026-09-05T10:10:00Z',
  refreshExpiresAt: '2026-09-12T10:00:00Z',
  tenantId: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  sessionId: '33333333-3333-3333-3333-333333333333',
  passwordChangeRequired: false
};

describe('auth.api', () => {
  it('login envia tenantId, login, password y channel WEB, y devuelve el token', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/auth/login', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleResponse);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await login(client, {
      tenantId: '11111111-1111-1111-1111-111111111111',
      login: 'admin@boticas.pe',
      password: 'Boticas2026!'
    });

    expect(receivedBody).toEqual({
      tenantId: '11111111-1111-1111-1111-111111111111',
      login: 'admin@boticas.pe',
      password: 'Boticas2026!',
      channel: 'WEB'
    });
    expect(result).toEqual(sampleResponse);
  });

  it('refresh envia el refreshToken y devuelve el nuevo token', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/auth/refresh', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleResponse);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await refresh(client, 'refresh-123');

    expect(receivedBody).toEqual({ refreshToken: 'refresh-123' });
    expect(result).toEqual(sampleResponse);
  });

  it('logout llama al endpoint sin body', async () => {
    let called = false;
    server.use(
      http.post('http://localhost/api/v1/auth/logout', () => {
        called = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    await logout(client);

    expect(called).toBe(true);
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- auth.api.test.ts`
Expected: FAIL — no se puede resolver el módulo `./auth.api`.

- [ ] **Step 3: Crear los tipos**

Crear `frontend/apps/erp-web/src/features/auth/api/auth-tokens.types.ts`:

```ts
export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessExpiresAt: string;
  refreshExpiresAt: string;
  tenantId: string;
  userId: string;
  sessionId: string;
  passwordChangeRequired: boolean;
};

export type LoginRequest = {
  tenantId: string;
  login: string;
  password: string;
};
```

- [ ] **Step 4: Crear `auth.api.ts`**

Crear `frontend/apps/erp-web/src/features/auth/api/auth.api.ts`:

```ts
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
```

Nota: el backend define `POST /auth/logout` sin body esperado; se envía `{}` porque `sendJson` siempre serializa un body — esto es inofensivo (`LocalAuthController.logout` no lee el body, solo el `Authorization` header vía `@AuthenticationPrincipal Jwt`).

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- auth.api.test.ts`
Expected: PASS — 3 tests en verde.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/api/
git commit -m "feat(auth): agregar cliente de API para login, refresh y logout"
```

---

### Task 3: Persistencia del refresh token en `sessionStorage`

**Files:**
- Create: `frontend/apps/erp-web/src/features/auth/model/session-storage.ts`
- Test: `frontend/apps/erp-web/src/features/auth/model/session-storage.test.ts`

**Interfaces:**
- Produces: `saveRefreshToken(token: string)`, `readRefreshToken(): string | null`, `clearRefreshToken()` — consumidos por la Task 4.

- [ ] **Step 1: Escribir el test que falla**

Crear `frontend/apps/erp-web/src/features/auth/model/session-storage.test.ts`:

```ts
import { clearRefreshToken, readRefreshToken, saveRefreshToken } from './session-storage';

describe('session-storage', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('guarda y lee el refresh token', () => {
    saveRefreshToken('token-abc');
    expect(readRefreshToken()).toBe('token-abc');
  });

  it('devuelve null cuando no hay token guardado', () => {
    expect(readRefreshToken()).toBeNull();
  });

  it('limpia el token guardado', () => {
    saveRefreshToken('token-abc');
    clearRefreshToken();
    expect(readRefreshToken()).toBeNull();
  });

  it('no lanza si sessionStorage.setItem falla', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });
    expect(() => saveRefreshToken('token-abc')).not.toThrow();
    spy.mockRestore();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- session-storage.test.ts`
Expected: FAIL — no se puede resolver el módulo `./session-storage`.

- [ ] **Step 3: Implementar `session-storage.ts`**

Crear `frontend/apps/erp-web/src/features/auth/model/session-storage.ts`:

```ts
const REFRESH_TOKEN_KEY = 'erp-botica.refreshToken';

export function saveRefreshToken(token: string): void {
  try {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
  } catch {
    // sessionStorage puede no estar disponible (modo privado, cuota excedida).
    // La sesion seguira funcionando en memoria durante esta pestana.
  }
}

export function readRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function clearRefreshToken(): void {
  try {
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  } catch {
    // Ignorado: si no se pudo leer/escribir, tampoco hay nada que limpiar de forma fiable.
  }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- session-storage.test.ts`
Expected: PASS — 4 tests en verde.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/model/session-storage.ts frontend/apps/erp-web/src/features/auth/model/session-storage.test.ts
git commit -m "feat(auth): agregar persistencia del refresh token en sessionStorage"
```

---

### Task 4: `AuthSessionProvider` real + registro de hooks en `apiClient`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/model/auth-session.context.ts`
- Modify: `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`
- Modify: `frontend/apps/erp-web/src/features/auth/schemas/login.schema.ts`
- Test: `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx`

**Interfaces:**
- Consumes: `login`, `refresh`, `logout` de `../api/auth.api` (Task 2); `saveRefreshToken`, `readRefreshToken`, `clearRefreshToken` de `./session-storage` (Task 3); `apiClient` de `../../../app/api` (import directo, ver nota abajo).
- Produces: `AuthSession` con forma `{ authenticated: boolean; accessToken: string | null; authenticate: (credentials: LoginCredentials) => Promise<void>; signOut: () => Promise<void>; }` — consumido por `LoginPage.tsx`, `RequireAuthentication.tsx`, `AppShell.tsx` (Task 5).

Nota sobre la dependencia circular: `AuthSessionProvider` importa `apiClient` desde `app/api.ts` para llamarle `setAuthHooks`. `app/api.ts` NO importa nada de `features/auth` (solo crea el cliente sin hooks iniciales). Por eso no hay ciclo: la dependencia va en un solo sentido, de `features/auth` hacia `app/api.ts`, igual que ya hace implícitamente el resto de features que usan `apiClient`.

- [ ] **Step 1: Actualizar el schema de login con el campo `tenantId`**

Reemplazar el contenido de `frontend/apps/erp-web/src/features/auth/schemas/login.schema.ts`:

```ts
import { z } from 'zod';

export const loginSchema = z.object({
  tenantId: z.uuid('Ingresa un identificador de organización válido.'),
  email: z.email('Ingresa un correo electrónico válido.'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.'),
  remember: z.boolean()
});

export type LoginCredentials = z.infer<typeof loginSchema>;
```

- [ ] **Step 2: Escribir el test que falla — login real y restauración de sesión**

Crear `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx`:

```tsx
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

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('false'));
    expect(readRefreshToken()).toBeNull();
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
```

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- AuthSessionProvider.test.tsx`
Expected: FAIL — `session.accessToken` es `undefined` / `authenticate` no llama a ningún endpoint (aún es el mock en memoria), o error de tipos si TypeScript ya rechaza `tenantId` en `authenticate`.

- [ ] **Step 4: Actualizar `auth-session.context.ts`**

Reemplazar el contenido de `frontend/apps/erp-web/src/features/auth/model/auth-session.context.ts`:

```ts
import { createContext } from 'react';
import type { LoginCredentials } from '../schemas/login.schema';

export type AuthSession = {
  authenticated: boolean;
  accessToken: string | null;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => Promise<void>;
};

export const AuthSessionContext = createContext<AuthSession | null>(null);
```

- [ ] **Step 5: Implementar `AuthSessionProvider.tsx`**

Reemplazar el contenido de `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`:

```tsx
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
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- AuthSessionProvider.test.tsx`
Expected: PASS — 4 tests en verde.

- [ ] **Step 7: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 8: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/model/auth-session.context.ts frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx frontend/apps/erp-web/src/features/auth/schemas/login.schema.ts
git commit -m "feat(auth): conectar AuthSessionProvider al backend real con refresh automatico"
```

---

### Task 5: Formulario de login con campo de tenant + logout real en `AppShell`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx`
- Modify: `frontend/apps/erp-web/src/shared/layout/AppShell.tsx`
- Modify: `frontend/apps/erp-web/src/test/mocks/handlers.ts`

**Interfaces:**
- Consumes: `AuthSession` (Task 4), `useAuthSession` (ya existente).

- [ ] **Step 1: Agregar el handler MSW de login para los tests de componentes**

Editar `frontend/apps/erp-web/src/test/mocks/handlers.ts`, agregar al array `handlers`:

```ts
  http.post('*/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { login: string; password: string };
    if (body.login === 'admin@boticas.pe' && body.password === 'Boticas2026!') {
      return HttpResponse.json({
        accessToken: 'access-test-token',
        refreshToken: 'refresh-test-token',
        tokenType: 'Bearer',
        accessExpiresAt: '2026-09-05T10:10:00Z',
        refreshExpiresAt: '2026-09-12T10:00:00Z',
        tenantId: '11111111-1111-1111-1111-111111111111',
        userId: '22222222-2222-2222-2222-222222222222',
        sessionId: '33333333-3333-3333-3333-333333333333',
        passwordChangeRequired: false
      });
    }
    return HttpResponse.json(
      { title: 'Credenciales invalidas', status: 401, detail: 'Usuario o contraseña incorrectos.' },
      { status: 401 }
    );
  })
```

- [ ] **Step 2: Escribir el test que falla — actualizar `LoginPage.test.tsx` con el campo tenant y el caso de error**

Reemplazar el contenido de `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { RequireAuthentication } from '../components/RequireAuthentication';
import { AuthSessionProvider } from '../model/AuthSessionProvider';
import { LoginPage } from './LoginPage';

function renderLogin(initialEntry = '/login') {
  const router = createMemoryRouter(
    [
      { path: '/login', Component: LoginPage },
      {
        Component: RequireAuthentication,
        children: [{ path: '/dashboard', Component: () => <h1>Resumen operativo</h1> }]
      }
    ],
    { initialEntries: [initialEntry] }
  );

  return {
    user: userEvent.setup(),
    ...render(
      <AuthSessionProvider>
        <RouterProvider router={router} />
      </AuthSessionProvider>
    )
  };
}

async function fillValidCredentials(user: ReturnType<typeof userEvent.setup>) {
  await user.type(
    screen.getByLabelText('Identificador de organización'),
    '11111111-1111-1111-1111-111111111111'
  );
  await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
  await user.type(screen.getByLabelText('Contraseña'), 'Boticas2026!');
}

describe('LoginPage', () => {
  it('muestra validaciones accesibles y permite visualizar la contraseña', async () => {
    const { user } = renderLogin();

    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(
      await screen.findByText('Ingresa un identificador de organización válido.')
    ).toBeInTheDocument();
    expect(screen.getByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(screen.getByText('La contraseña debe tener al menos 8 caracteres.')).toBeInTheDocument();

    const password = screen.getByLabelText('Contraseña');
    expect(password).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Mostrar contraseña' }));
    expect(password).toHaveAttribute('type', 'text');
  });

  it('navega al dashboard cuando las credenciales son válidas', async () => {
    const { user } = renderLogin();

    await fillValidCredentials(user);
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(await screen.findByRole('heading', { name: 'Resumen operativo' })).toBeInTheDocument();
  });

  it('muestra un mensaje de error cuando el backend rechaza las credenciales', async () => {
    const { user } = renderLogin();

    await user.type(
      screen.getByLabelText('Identificador de organización'),
      '11111111-1111-1111-1111-111111111111'
    );
    await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
    await user.type(screen.getByLabelText('Contraseña'), 'ContrasenaIncorrecta1!');
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(
      await screen.findByText('Credenciales incorrectas o cuenta bloqueada.')
    ).toBeInTheDocument();
  });

  it('redirige al login cuando se intenta abrir una ruta privada sin sesión', async () => {
    renderLogin('/dashboard');

    expect(await screen.findByRole('heading', { name: 'Ingresa a tu cuenta' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: FAIL — no existe el campo con label "Identificador de organización", y no hay mensaje de error visible tras credenciales inválidas.

- [ ] **Step 4: Agregar el campo `tenantId` y el manejo de error a `LoginForm.tsx`**

En `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`:

Cambiar la firma del componente y agregar estado de error:

```tsx
type LoginFormProps = {
  onAuthenticate: (credentials: LoginCredentials) => Promise<void>;
};

export function LoginForm({ onAuthenticate }: LoginFormProps) {
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [recoveryVisible, setRecoveryVisible] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register
  } = useForm<LoginCredentials>({
    defaultValues: { tenantId: '', email: '', password: '', remember: false },
    mode: 'onTouched',
    resolver: zodResolver(loginSchema)
  });

  const submitLogin = handleSubmit(async (credentials) => {
    setSubmitError(null);
    try {
      await onAuthenticate(credentials);
    } catch {
      setSubmitError('Credenciales incorrectas o cuenta bloqueada.');
    }
  });
```

Agregar el campo `tenantId` como primer campo del formulario (antes del bloque de `email`), siguiendo el mismo patrón visual que los campos existentes:

```tsx
      <div>
        <label htmlFor="tenantId" className="text-sm font-semibold text-slate-700">
          Identificador de organización
        </label>
        <div className="relative mt-2">
          <input
            id="tenantId"
            type="text"
            autoComplete="off"
            placeholder="UUID de tu organización"
            aria-describedby={errors.tenantId ? 'tenantId-error' : undefined}
            aria-invalid={Boolean(errors.tenantId)}
            className="h-12 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-900 shadow-sm transition outline-none placeholder:text-slate-400 hover:border-slate-300 focus:border-teal-600 focus:ring-4 focus:ring-teal-100 aria-invalid:border-rose-400 aria-invalid:focus:ring-rose-100"
            {...register('tenantId')}
          />
        </div>
        {errors.tenantId ? (
          <p id="tenantId-error" role="alert" className="mt-1.5 text-xs font-medium text-rose-600">
            {errors.tenantId.message}
          </p>
        ) : null}
      </div>

```

Agregar el bloque de error de submit justo antes del `<Button type="submit" ...>`:

```tsx
      {submitError ? (
        <p role="alert" className="rounded-xl bg-rose-50 px-3.5 py-3 text-xs font-medium text-rose-700">
          {submitError}
        </p>
      ) : null}

```

Y cambiar el `onSubmit` del `<form>` para usar `submitLogin` (ya definido arriba, sin cambios en esa línea).

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — 4 tests en verde.

- [ ] **Step 6: Conectar el botón de logout en `AppShell.tsx`**

En `frontend/apps/erp-web/src/shared/layout/AppShell.tsx`:

Agregar el import:
```tsx
import { useNavigate } from 'react-router';
import { useAuthSession } from '../../features/auth';
```

Dentro de `export function AppShell()`, antes del `return`:
```tsx
  const { signOut } = useAuthSession();
  const navigate = useNavigate();

  async function handleSignOut() {
    await signOut();
    await navigate('/login', { replace: true });
  }
```

Cambiar el botón de usuario (el que hoy no tiene `onClick`) para envolver solo la acción de cerrar sesión con un botón explícito adicional, sin quitar el bloque de perfil existente — agregar debajo del `<button className="flex items-center gap-3 ...">` (dentro del mismo `div.ml-auto`):

```tsx
            <Button variant="ghost" size="sm" onClick={() => void handleSignOut()}>
              Cerrar sesión
            </Button>
```

- [ ] **Step 7: Verificar `useAuthSession` está exportado desde el índice de la feature**

Confirmar que `frontend/apps/erp-web/src/features/auth/index.ts` ya exporta `useAuthSession` (ya lo hace desde antes, sin cambios necesarios).

- [ ] **Step 8: Ejecutar toda la suite de `erp-web` y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test`
Expected: PASS — todos los tests, incluidos los de `AppShell` si existen, y los de `dashboard`/`organizacion` que no deben verse afectados.

- [ ] **Step 9: Typecheck y build completos**

Run: `pnpm --filter @boticas/erp-web typecheck`
Run: `pnpm --filter @boticas/erp-web build`
Expected: sin errores en ambos.

- [ ] **Step 10: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx frontend/apps/erp-web/src/shared/layout/AppShell.tsx frontend/apps/erp-web/src/test/mocks/handlers.ts
git commit -m "feat(auth): agregar campo de tenant al login y conectar logout real en AppShell"
```

---

### Task 6: Verificación manual end-to-end contra el backend real

**Files:** ninguno — verificación manual, sin cambios de código salvo que se detecte un ajuste necesario (documentar cualquier fix en un commit separado si aplica).

**Interfaces:**
- Consumes: todo lo producido en Tasks 1-5, y el backend Docker (`docker compose up`, ver `README.md` de la raíz).

- [ ] **Step 1: Levantar el backend**

Desde la raíz del repo:
```bash
docker compose up --build -d
```
Expected: `docker compose ps` muestra `app` y `postgres` en `healthy`.

- [ ] **Step 2: Sembrar un tenant, usuario y credencial de prueba**

Seguir el mismo procedimiento usado en la verificación manual anterior de este proyecto (insertar en `sch_farmacia.tenant`, `sch_seguridad.usuario`, generar hash `pbkdf2@SpringSecurity_v5_8` y guardarlo en `sch_seguridad.credencial_local`). Anotar el `tenantId` (UUID) y el email/username creados.

- [ ] **Step 3: Levantar el frontend**

```bash
cd frontend
pnpm dev
```
Confirmar en `frontend/apps/erp-web/.env.development` (o variable de entorno) que `VITE_API_MODE` no está en `mock` para esta prueba — debe apuntar al backend real vía el proxy `/api` de Vite (`http://localhost:8080`, ya configurado en `vite.config.ts`).

- [ ] **Step 4: Probar el login real en el navegador**

Abrir `http://localhost:5173/login`, completar el `tenantId`, email y password sembrados en el Step 2, enviar el formulario.
Expected: redirección a `/dashboard`. En DevTools → Application → Session Storage, confirmar que existe la clave `erp-botica.refreshToken`. Confirmar que NO existe ningún token en `localStorage`.

- [ ] **Step 5: Probar la restauración de sesión tras recargar**

Recargar la página (F5) estando en `/dashboard`.
Expected: la sesión se mantiene (no redirige a `/login`) — el `AuthSessionProvider` restaura la sesión automáticamente vía `/auth/refresh` al montar.

- [ ] **Step 6: Probar el logout**

Click en "Cerrar sesión" en el `AppShell`.
Expected: redirección a `/login`, y `sessionStorage` ya no tiene la clave `erp-botica.refreshToken`.

- [ ] **Step 7: Apagar el backend**

```bash
docker compose down
```

- [ ] **Step 8: Commit final (solo si el Step 4-6 reveló necesidad de ajuste)**

Si algún paso de esta verificación requirió un cambio de código no cubierto por las Tasks 1-5, aplicarlo y commitear con un mensaje descriptivo del ajuste. Si todo funcionó según lo esperado, este step no aplica — no crear un commit vacío.

---

## Self-Review

**Cobertura del spec:**
- Campo `tenantId` en el formulario → Task 5, Step 4. ✓
- `accessToken` solo en memoria → Task 4, `useState` en `AuthSessionProvider`, nunca persistido. ✓
- `refreshToken` en `sessionStorage` con nota de deuda técnica → Task 3 + referencia en Global Constraints. ✓
- Hooks de autenticación inyectables sin acoplar `api-client` a React → Task 1. ✓
- Refresh automático con reintento en 401 → Task 1 (`client.ts`) + Task 4 (`onUnauthorized`/`performRefresh` con deduplicación vía `pendingRefresh` ref). ✓
- Logout real conectado a la UI → Task 5, Step 6. ✓
- Restauración de sesión al montar → Task 4, Step 5 (`useEffect` con `performRefresh` al montar). ✓
- Verificación end-to-end contra backend real → Task 6. ✓

**Placeholders:** ninguno — cada step tiene código completo.

**Consistencia de nombres:** `AuthHooks` (Task 1) se importa igual en Task 4. `login`/`refresh`/`logout` de `auth.api.ts` (Task 2) se re-exportan con alias (`loginRequest`, etc.) en `AuthSessionProvider.tsx` para no chocar con la función `login` del propio provider — verificado que los nombres coinciden entre la definición (Task 2) y el uso (Task 4). `AuthSession` type consistente entre Task 4 (definición) y su uso implícito en Task 5 (`useAuthSession().signOut`).
