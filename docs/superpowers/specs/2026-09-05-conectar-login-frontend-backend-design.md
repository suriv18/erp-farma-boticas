# Conectar login real del frontend con el backend — diseño

## Contexto

El backend `service-botica` tiene un flujo de autenticación local completo y verificado (`POST /api/v1/auth/login`, `/refresh`, `/logout`), probado manualmente end-to-end contra PostgreSQL 18 real en Docker. El frontend (`apps/erp-web`), en cambio, tiene un login puramente simulado: `AuthSessionProvider.authenticate()` no hace ninguna llamada HTTP, solo marca un booleano en memoria (`frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`). No existe manejo de tokens en ningún lugar del frontend, ni interceptor de 401.

Este cambio conecta el login del frontend al backend real: el formulario envía credenciales reales, el backend responde con `accessToken`/`refreshToken`, y las llamadas posteriores a la API usan ese token, con renovación automática al expirar.

## Decisiones (confirmadas con el usuario, con investigación de prácticas actuales)

1. **Tenant en el login**: se agrega un campo de `tenantId` (UUID) al formulario de login, junto a email/password. No existe hoy un endpoint de resolución "código de organización → tenantId" en el backend, así que pedir el UUID directo es la opción viable sin ampliar el alcance del backend.
2. **Almacenamiento del `accessToken`**: en memoria (estado de React), nunca persistido. Se pierde al recargar la página — esto es lo esperado y correcto según OWASP (los access tokens de vida corta no deben sobrevivir a un reload).
3. **Almacenamiento del `refreshToken`**: en `sessionStorage`. **Nota de seguridad explícita**: la práctica recomendada por OWASP (Session Management Cheat Sheet) es que el refresh token viva en una cookie `HttpOnly`+`Secure`+`SameSite`, inaccesible a JavaScript y por tanto inmune a robo por XSS. El backend actual (`LocalAuthController`) emite el refresh token en el cuerpo JSON de la respuesta, no como cookie — cambiar esto requeriría modificar también el backend (`Set-Cookie` en vez de body), lo cual queda **fuera de alcance de este cambio**. Se usa `sessionStorage` como opción intermedia (no sobrevive cierre del navegador, pero es sensible a XSS) y se documenta como deuda técnica de seguridad a resolver en una iteración futura que sí toque el backend.
4. **Manejo de 401 / expiración**: refresh automático con reintento de la request original, usando un patrón de flag + cola para evitar múltiples llamadas a `/refresh` concurrentes si varias requests fallan a la vez (práctica estándar confirmada por investigación). Si el refresh también falla, se cierra sesión y se redirige a `/login`.
5. **Canal fijo**: `channel: "WEB"` fijo para todos los logins desde `erp-web` (no hay UI de selección de canal; POS es una app distinta según el diseño del sistema).

## Diseño

### `packages/api-client` — hooks de autenticación inyectables

El paquete `api-client` no debe depender de React ni de la feature `auth`. Se le agregan dos hooks opcionales a `ApiClientConfig`:

```ts
// frontend/packages/api-client/src/auth-hooks.ts
export type AuthHooks = {
  getAccessToken: () => string | null;
  onUnauthorized: () => Promise<string | null>; // intenta refresh, devuelve el nuevo access token o null si falla
};
```

`createApiClient` acepta `authHooks?: AuthHooks` en su config. En `request()`:
- Si `authHooks.getAccessToken()` devuelve un token, se adjunta `Authorization: Bearer <token>` al header (si el caller no lo puso ya explícitamente).
- Si la respuesta es `401` y hay `authHooks`, se llama a `onUnauthorized()` una sola vez por request (sin cola global dentro del cliente — la deduplicación de refreshes concurrentes vive en `onUnauthorized` mismo, ver abajo). Si devuelve un token nuevo, se reintenta la request original una vez con ese token. Si devuelve `null`, se relanza el `ApiError` 401 original.

### `features/auth` — estado de sesión real

**Nuevo: `frontend/apps/erp-web/src/features/auth/api/auth.api.ts`**
Funciones puras que llaman al `apiClient` ya existente (importado desde `app/api.ts` vía inyección, no importado directo — ver nota de dependencia circular abajo):
- `login({tenantId, login, password, channel}): Promise<AuthTokenResponse>`
- `refresh(refreshToken): Promise<AuthTokenResponse>`
- `logout(): Promise<void>` (llama con el access token vigente vía el `apiClient` ya autenticado)

`AuthTokenResponse` se tipa según la respuesta real del backend (`AuthTokenResponse.java`): `accessToken`, `refreshToken`, `tokenType`, `accessExpiresAt`, `refreshExpiresAt`, `tenantId`, `userId`, `sessionId`, `passwordChangeRequired`.

**Nuevo: `frontend/apps/erp-web/src/features/auth/model/session-storage.ts`**
Helpers `saveRefreshToken`/`readRefreshToken`/`clearRefreshToken` sobre `sessionStorage`, con manejo de excepciones (Safari en modo privado puede lanzar) — nunca deben romper el flujo si `sessionStorage` no está disponible.

**Modificado: `auth-session.context.ts`**
```ts
export type AuthSession = {
  authenticated: boolean;
  accessToken: string | null;
  authenticate: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => Promise<void>;
};
```

**Modificado: `AuthSessionProvider.tsx`**
- Estado: `accessToken` (string | null) en memoria vía `useState`.
- `authenticate(credentials)`: llama a `auth.api.login(...)`, guarda `accessToken` en memoria y `refreshToken` en `sessionStorage`, propaga `ApiError` si falla (el formulario ya maneja errores vía React Hook Form / mensaje visible).
- Al montar (`useEffect` una vez): si hay `refreshToken` en `sessionStorage`, intenta `auth.api.refresh(...)` en silencio para restaurar sesión tras un reload; si falla, limpia el storage y queda no autenticado (sin mostrar error, es un intento silencioso).
- `signOut()`: llama a `auth.api.logout()` (best-effort, no bloquea el signOut si falla), limpia `accessToken` y `sessionStorage`.
- Expone `getAccessToken` y `onUnauthorized` (implementa la deduplicación de refresh concurrente con una promesa en curso compartida) para pasarlos al `apiClient`.

**Dependencia circular a resolver**: `app/api.ts` crea el `apiClient` a nivel de módulo (fuera de React), pero los `authHooks` dependen del estado de `AuthSessionProvider` (dentro de React). Se resuelve con un patrón de *late binding*: `apiClient` se crea sin `authHooks` fijos, exponiendo un método `setAuthHooks(hooks: AuthHooks)`; `AuthSessionProvider` llama a `apiClient.setAuthHooks(...)` en un `useEffect` al montar, actualizando la referencia interna del cliente cada vez que cambien las funciones (usando `useCallback`/`useRef` para mantener identidad estable donde aplique).

### Formulario de login

**Modificado: `schemas/login.schema.ts`**
```ts
export const loginSchema = z.object({
  tenantId: z.uuid('Ingresa un identificador de organización válido.'),
  email: z.email('Ingresa un correo electrónico válido.'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.'),
  remember: z.boolean()
});
```

**Modificado: `LoginForm.tsx`**: se agrega el campo `tenantId` (input de texto simple, con label "Identificador de organización") antes del campo de correo. Se agrega manejo visible de error cuando `onAuthenticate` lanza `ApiError` (mensaje genérico "Credenciales incorrectas o cuenta bloqueada", sin filtrar detalles del backend).

### Logout real en la UI

**Modificado: `shared/layout/AppShell.tsx`**: el botón de menú de usuario (hoy decorativo, sin `onClick`) se conecta a `useAuthSession().signOut()`.

## Fuera de alcance

- Refresh token vía cookie `HttpOnly` (requiere cambios en el backend — ver nota de seguridad arriba).
- Recuperación de contraseña real, cambio de contraseña, MFA.
- CRUD de usuarios/roles/permisos (la feature `seguridad` sigue siendo placeholder).
- Selector de empresa/sucursal activa.
- Endpoint de resolución de código de organización a `tenantId`.
- Persistir "recordar mi correo" (el checkbox existente queda visual, sin funcionalidad nueva).

## Verificación

1. Con el backend levantado vía `docker compose up` (ver README) y un usuario+credencial sembrados en la base (como se hizo manualmente en la sesión anterior), completar el formulario de login con `tenantId`, email y password reales → debe redirigir al dashboard.
2. Verificar en las DevTools que `accessToken` nunca aparece en `localStorage`/`sessionStorage`, y que `refreshToken` sí aparece en `sessionStorage`.
3. Provocar una expiración de `accessToken` (esperar >10 min, el TTL configurado, o bajar `BOTICA_ACCESS_TOKEN_TTL` temporalmente) y confirmar que una llamada autenticada se renueva sola sin desloguear al usuario.
4. Invalidar el `refreshToken` manualmente (revocar la sesión desde la BD) y confirmar que la siguiente llamada autenticada desloguea y redirige a `/login`.
5. Recargar la página tras un login exitoso y confirmar que la sesión se restaura silenciosamente (sin pedir login de nuevo) mientras el refresh token siga válido.
6. Probar el botón de logout en `AppShell` y confirmar que limpia la sesión y redirige a `/login`.
7. Ejecutar `pnpm check` en `frontend/` (lint + typecheck + tests + build) sin errores.
