# ERP Web

Frontend del ERP de boticas construido con React, TypeScript, Vite, Tailwind CSS y pnpm.

## Estado de implementación

La aplicación no es todavía un ERP funcional completo. Estado por feature:

| Feature | Estado | Contra el backend real |
| --- | --- | --- |
| `auth` | Login, renovación de sesión con refresh token, cierre de sesión, cambio obligatorio de contraseña (`/cambiar-contrasena`) y limpieza de la caché de consultas al cambiar de sesión | Sí |
| `seguridad` | Permisos, roles y usuarios con listado, detalle y formularios | Sí |
| `organizacion` | Empresas, establecimientos, almacenes y terminales POS con alta, edición y cambio de estado; usa `/api/v1/estructura-corporativa` | Sí |
| `catalogo` | Categorías y marcas; los otros 8 catálogos expuestos por el backend no tienen UI | Sí (2 de 10) |
| `inventario` | Posiciones con filtros, ingreso con lote nuevo, ajustes con `Idempotency-Key`, detalle y bloqueo de lote | Sí |
| `compras` | Órdenes de compra y recepción | Sí |
| `dashboard` | Resumen con datos mock | No |
| `ventas`, `pos`, `caja`, `clientes` | Placeholders sin lógica | No |

Las pruebas end-to-end de Playwright (`e2e/`) corren con Vite en modo `http` y simulan la API con `page.route`; no ejercitan el backend real. Una pantalla visible no equivale a una capacidad terminada: cada afirmación debe poder contrastarse con rutas, endpoints y pruebas.

El flujo y alcance canónicos están en el [`README` de la documentación](../docs/cadena-farmacias-docs/README.md).

La base validada utiliza React 19.2.8, React Router 8.3.0, Vite 8.2.1 y Tailwind CSS
4.3.3. Los imports retirados de React Router 8 y el uso de `forwardRef` en componentes
nuevos están bloqueados por ESLint para evitar regresiones hacia APIs antiguas.

## Requisitos

- Node.js 24.14.1
- pnpm 11.9.0

## Primer arranque

```bash
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

La aplicación queda disponible en `http://localhost:3000`.

En Windows PowerShell, si la política de ejecución bloquea `pnpm.ps1`, se puede usar
`pnpm.cmd` en los mismos comandos.

## Comandos

```bash
pnpm dev           # servidor de desarrollo
pnpm check         # lint + tipos + pruebas + build
pnpm test          # pruebas unitarias y de integración
pnpm build         # artefacto de producción
pnpm preview       # previsualización del build
pnpm format        # formato automático
pnpm format:check  # valida el formato sin modificar archivos
```

## Organización

```text
frontend/
├── apps/
│   └── erp-web/       # aplicación desplegable
├── packages/
│   ├── api-client/    # cliente HTTP compartido
│   └── ui-web/        # componentes visuales compartidos
└── package.json       # configuración y comandos comunes
```

La aplicación se organiza por funcionalidades dentro de `apps/erp-web/src/features`.
Los paquetes compartidos se mantienen deliberadamente pequeños: solo contienen código
que ya tiene más de un consumidor o que define una frontera técnica estable.

Los módulos `auth`, `dashboard`, `seguridad`, `organizacion`, `catalogo`, `inventario`,
`compras`, `ventas`, `pos`, `caja` y `clientes` se ensamblan como paquetes locales de feature.
Cada uno publica sus rutas desde un `index.ts`; `app/feature-routes.ts` actúa como
composición padre y el router no accede a sus archivos internos.

## Configuración del API

- `VITE_API_MODE` admite `http` (predeterminado) y `mock`. `.env.development` y `.env.production` usan `http`; `.env.example` usa `mock`.
- Modo `mock`: solo en desarrollo, arranca MSW (`src/test/mocks`) y permite ejecutar la interfaz sin backend.
- Modo `http`: la URL base es `/api/v1` (`VITE_API_BASE_URL`) y, en desarrollo, Vite redirige `/api` hacia `http://localhost:8080`.

Todas las variables expuestas al navegador deben comenzar con `VITE_`.
