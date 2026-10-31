# Diseño: Integración frontend del módulo `seguridad`

Fecha: 2026-09-15
Estado: propuesto

## Contexto

El backend `service-botica`/`security` (IAM) está 100% funcional y verificado end-to-end en esta misma sesión: login local con JWT access/refresh, gestión de usuarios, roles con permisos, sesiones, dispositivos e identidades externas (SSO). Todos los endpoints fueron confirmados leyendo el código fuente real de los controllers.

El frontend (`frontend/`, pnpm workspace con `apps/erp-web`) ya tiene:
- Autenticación real conectada (`features/auth/`): login, refresh automático en 401, logout — **no requiere cambios**.
- `@tanstack/react-query` instalado y provisto en el árbol de la app (`app/query-client.ts`, `app/providers.tsx`), con el patrón `queryOptions` ya en uso en `features/organizacion/api/organization.api.ts`.
- `react-hook-form` + `zod` ya en uso para formularios (`features/auth/schemas/login.schema.ts`, `components/LoginForm.tsx`).
- Vitest + Testing Library + MSW ya configurados como workspace de 3 proyectos (`vitest.config.ts` en la raíz), con handlers globales en `apps/erp-web/src/test/mocks/handlers.ts` y patrón de tests co-ubicados (`*.test.tsx` junto al archivo que prueban).
- `features/seguridad/` hoy es un placeholder vacío: solo `index.ts`, `routes.tsx`, `pages/SecurityPage.tsx` (renderiza `ModulePlaceholderPage`).
- **Playwright no existe en el repo** — ni config, ni carpeta `e2e/`, ni dependencia.

El objetivo de este diseño es reemplazar el placeholder de `seguridad` por 6 pantallas CRUD completas conectadas al backend real, y crear la infraestructura Playwright base del proyecto (que luego reutilizará el módulo `catalogo` en una segunda spec).

## Alcance

Cinco rutas/pantallas para las 6 entidades de seguridad (Identidades externas no tiene ruta propia: se gestiona dentro del detalle de Usuario, como sub-recurso), cada una con listado + creación + edición donde el backend lo soporta + cambio de estado + acciones específicas del recurso:

1. **Usuarios** — listado paginado y buscable, alta, cambio de estado, asignación de rol con ámbito organizacional, gestión de identidades externas (vincular/desvincular), provisión de credencial local (fijar contraseña inicial).
2. **Roles** — listado paginado y buscable, alta, cambio de estado, reemplazo del conjunto de permisos (checklist contra el catálogo de Permisos).
3. **Permisos** — listado de solo lectura (catálogo global, sin tenant, sin mutaciones — el backend no expone escritura sobre `/api/v1/permisos`).
4. **Sesiones** — listado (filtrable por usuario), revocación con motivo opcional.
5. **Dispositivos** — listado, registro, cambio de estado (PENDIENTE/CONFIABLE/BLOQUEADO/REVOCADO).
6. **Identidades externas** — se gestionan **dentro** de la pantalla de detalle de un Usuario (no es una ruta propia — el backend las expone como sub-recurso de usuario: `GET/POST/DELETE /usuarios/{userId}/identidades-externas`).

Fuera de alcance explícito: flujo de "olvidé mi contraseña" / recuperación (`/auth/password/forgot`, `/auth/password/reset`) — pertenece a la pantalla de login pública, no a la administración de seguridad, y no fue pedido. Cambio de la propia contraseña del usuario autenticado (`/auth/password/change`) — pertenece a un futuro "mi perfil", fuera de esta spec.

## Arquitectura de la feature

Seguimos el patrón exacto de `features/auth/` (la feature más completa ya existente):

```
apps/erp-web/src/features/seguridad/
├── api/
│   ├── usuarios.api.ts            # queryOptions + mutaciones: crear, listar, cambiar estado, asignar rol, identidades externas, credencial local
│   ├── usuarios.types.ts
│   ├── roles.api.ts                # queryOptions + mutaciones: crear, listar, cambiar estado, reemplazar permisos
│   ├── roles.types.ts
│   ├── permisos.api.ts             # queryOptions: listar (solo lectura)
│   ├── permisos.types.ts
│   ├── sesiones.api.ts             # queryOptions + mutación: listar, revocar
│   ├── sesiones.types.ts
│   ├── dispositivos.api.ts         # queryOptions + mutaciones: listar, registrar, cambiar estado
│   ├── dispositivos.types.ts
│   └── *.api.test.ts               # uno por archivo de api, mismo estilo que auth.api.test.ts
├── components/
│   ├── EstadoBadge.tsx              # pill de estado reutilizable entre las 6 pantallas
│   ├── ConfirmActionDialog.tsx      # diálogo de confirmación reutilizable (cambiar estado, revocar, desvincular)
│   ├── UsuarioForm.tsx + .test.tsx
│   ├── RolForm.tsx + .test.tsx
│   ├── PermisosChecklist.tsx + .test.tsx   # usado dentro de RolForm
│   ├── AsignarRolDialog.tsx + .test.tsx
│   ├── DispositivoForm.tsx + .test.tsx
│   └── IdentidadExternaForm.tsx + .test.tsx
├── pages/
│   ├── UsersPage.tsx + .test.tsx              # ruta index del módulo
│   ├── UserDetailPage.tsx + .test.tsx          # asignaciones de rol + identidades externas + credencial local
│   ├── RolesPage.tsx + .test.tsx
│   ├── PermissionsPage.tsx + .test.tsx
│   ├── SessionsPage.tsx + .test.tsx
│   └── DevicesPage.tsx + .test.tsx
├── schemas/
│   ├── usuario.schema.ts
│   ├── rol.schema.ts
│   ├── dispositivo.schema.ts
│   └── identidad-externa.schema.ts
├── routes.tsx
└── index.ts
```

Cada `api/*.api.ts` expone `queryOptions` (para `useQuery`/`useSuspenseQuery`) y funciones de mutación planas (usadas con `useMutation`), replicando el tipado de `ApiClient` del paquete `@boticas/api-client` (`client.get/post/put/patch/delete`). Los tipos de request/response en `*.types.ts` reflejan **exactamente** los DTOs del backend confirmados (ver tabla de contratos abajo) — sin inventar ni omitir campos.

### Contratos por recurso (resumen; el detalle completo con tipos queda en el código)

| Recurso | Endpoints usados | Paginación | `tenantId` |
|---|---|---|---|
| Usuarios | `POST /api/v1/usuarios`, `GET /api/v1/usuarios?tenantId&search&page&size`, `PATCH /usuarios/{id}/estado?tenantId`, `POST /usuarios/{id}/role-assignments`, `GET/DELETE /usuarios/{id}/asignaciones-rol?tenantId`, `GET/POST/DELETE /usuarios/{id}/identidades-externas?tenantId`, `POST /usuarios/{id}/credencial-local` | Sí (page/size) | body en create/assign; query en list/estado/identidades/asignaciones |
| Roles | `POST /api/v1/roles`, `GET /api/v1/roles?tenantId&search&page&size`, `PATCH /roles/{id}/estado?tenantId`, `PUT /roles/{id}/permissions` | Sí (page/size) | body en create; query en list/estado; no aplica en `permissions` (solo `roleId` de path) |
| Permisos | `GET /api/v1/permisos?search` | No (lista simple) | no aplica (catálogo global) |
| Sesiones | `GET /sesiones?tenantId&userId`, `POST /sesiones/{id}/revocacion?tenantId` | No (lista simple) | query |
| Dispositivos | `GET /dispositivos?tenantId`, `POST /dispositivos`, `PATCH /dispositivos/{id}/estado?tenantId` | No (lista simple) | body en create; query en list/estado |

El `tenantId` activo se resuelve desde el estado de sesión actual (`AuthSessionContext` ya expone `accessToken`; el `tenantId`/`userId` de la respuesta de login **hoy se descartan** en `AuthSessionProvider` — este diseño requiere guardarlos en el contexto de sesión, ya que todas las pantallas de seguridad los necesitan como parámetro obligatorio). Este es el único cambio necesario fuera de `features/seguridad/`.

## Componentes de UI compartidos nuevos

- `EstadoBadge`: pill de color según estado (reutilizado en las 4 pantallas con estado: usuarios, roles, dispositivos, sesiones).
- `ConfirmActionDialog`: diálogo genérico de confirmación con texto de advertencia, usado para cambios de estado destructivos (bloquear dispositivo, revocar sesión, desvincular identidad).
- `PermisosChecklist`: lista de permisos agrupados por módulo (`moduleCode`/`moduleName` de `PermisoResponse`) con checkboxes, usada dentro de `RolForm` para el reemplazo de permisos.

Si `packages/ui-web` ya tiene primitivos de tabla/modal/badge reutilizables, se usan esos como base; si no, se construyen localmente en `features/seguridad/components/` sin promoverlos a `packages/ui-web` todavía (el criterio de "≥2 consumidores reales" de la arquitectura del proyecto se cumplirá naturalmente cuando `catalogo` los necesite en la siguiente spec — momento en que se evaluará la extracción, no antes).

## Testing

### Unitario / integración (Vitest + Testing Library + MSW)

Cada `pages/*.test.tsx` monta la página con `createMemoryRouter` (patrón de `LoginPage.test.tsx`) y usa handlers MSW locales (`server.use(...)`) para simular el backend. Casos cubiertos por pantalla:
- Estado de carga y listado con datos.
- Estado vacío (sin registros).
- Estado de error (backend responde 500 / problem+json).
- Creación exitosa (formulario válido → petición correcta → refetch de la lista).
- Validación de formulario fallida (campos obligatorios vacíos, formatos inválidos según zod).
- Acción de cambio de estado (con confirmación).
- Autorización denegada (403) en al menos un caso por pantalla — verifica que la UI oculta o deshabilita la acción sin el permiso correspondiente, replicando el patrón `deniesIamAdministrationWithoutTheRequiredPermission` ya probado en el backend.

Cada `api/*.api.test.ts` prueba las funciones de fetch/mutación de forma aislada (patrón `auth.api.test.ts`): construye el payload esperado, verifica que la URL/método sean correctos, y que la respuesta se parsea al tipo esperado.

Cada `components/*.test.tsx` prueba el formulario/diálogo de forma aislada de la página que lo usa (validación zod, callbacks `onSubmit`/`onCancel`).

### E2E (Playwright — infraestructura nueva)

Se crea la configuración base del proyecto, reutilizable por `catalogo` después:

- `frontend/playwright.config.ts`: 3 proyectos —
  - `desktop`: viewport 1280×800 (Chromium).
  - `tablet`: viewport 768×1024 (Chromium, `isMobile: false`).
  - `mobile`: viewport 390×844 (WebKit, `isMobile: true`, emulando iPhone).
  - `webServer`: `pnpm --filter erp-web dev`, `url: http://localhost:5173`, `reuseExistingServer: !process.env.CI`.
  - `use.baseURL: http://localhost:5173`.
- `frontend/e2e/seguridad/`: specs por flujo crítico (no por pantalla — un spec puede cruzar varias pantallas si el flujo de negocio lo requiere):
  - `login.spec.ts`: login real contra el backend, credenciales de un usuario de prueba sembrado (ver más abajo).
  - `usuarios-roles.spec.ts`: crear un usuario → asignarle un rol con ámbito ESTABLECIMIENTO → verificar que aparece en el listado con el rol correcto → cambiar su estado a INACTIVO → verificar que el listado lo refleja.
  - `sesiones.spec.ts`: iniciar sesión, verificar que aparece en el listado de sesiones propio, revocarla, verificar que un refresh subsecuente falla.
- Los E2E corren contra el **backend real** vía Docker Compose (`docker compose up -d postgres` + `service-botica` corriendo, tal como se dejó configurado y verificado en esta sesión) — no se mockea la red. El `README.md` de `frontend/` documenta el prerrequisito ("Docker Desktop activo, stack de `service-botica` corriendo en :8080") y un script `pnpm test:e2e` nuevo en el `package.json` raíz de `frontend/`; no se automatiza el arranque del backend desde el propio comando de Playwright, para no acoplar el ciclo de vida de ambos repos.
- Datos de prueba: el spec de login necesita un tenant + usuario con credencial local ya sembrados. Se sembrarán vía un script SQL/HTTP de setup (`e2e/fixtures/seed.ts`, ejecutado en `globalSetup` de Playwright) que llama directamente a los endpoints de creación (`POST /api/v1/usuarios`, `POST /usuarios/{id}/credencial-local`) contra el backend real antes de correr los specs — evita hardcodear SQL de seed y ejercita los mismos endpoints que la app usa.

## Manejo de errores

Todas las mutaciones usan el `ApiError`/`ProblemDetails` ya estandarizado por `@boticas/api-client` (`toApiError`). Los formularios muestran el `detail`/`title` del problem+json en un toast o alerta inline; no se re-implementa parsing de errores nuevo. Los códigos de error de negocio del backend (`CAT_*`, duplicados 409, `TENANT_NOT_FOUND` 404, etc.) se muestran con su mensaje tal cual devuelto por el backend — no se mapean a mensajes de UI custom en esta primera iteración (YAGNI: se ajustará si algún mensaje resulta confuso en uso real).

## Fuera de alcance de esta spec

- Módulo `catalogo` (spec separada, reutiliza la infraestructura Playwright creada aquí).
- Recuperación de contraseña y cambio de la propia contraseña del usuario autenticado.
- Internacionalización / soporte multi-idioma.
- Extracción de componentes compartidos a `packages/ui-web` (se evalúa en la spec de `catalogo`).
