# ERP Web

Frontend del ERP de boticas construido con React, TypeScript, Vite, Tailwind CSS y pnpm.

## Estado de implementación

La aplicación es un scaffold navegable, no un ERP funcional terminado:

- `auth` valida formato, pero acepta cualquier credencial y mantiene sesión solo en memoria;
- `dashboard` consulta un resumen mediante MSW en desarrollo y conserva ventas/usuario/fecha fijos;
- `inventario` presenta fixtures sin búsqueda, filtros ni ajustes conectados;
- `organizacion` consulta mediante MSW el contrato candidato `/api/v1/estructura-corporativa` y
  presenta empresa, establecimiento, almacenes y cajas; el endpoint backend aún no existe;
- catálogo, compras, ventas, POS, caja, clientes y seguridad son placeholders;
- existe un OpenAPI candidato, pero todavía no hay cliente generado ni navegación/RBAC obtenidos
  desde backend.

El flujo y alcance canónicos están en
[`GOV-FAR-001`](../docs/cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md).

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

La aplicación queda disponible en `http://localhost:5173`.

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

- Desarrollo: `.env.development` utiliza MSW para ejecutar la interfaz sin backend.
- Producción: la configuración común utiliza `/api/v1` como URL base.
- Backend local: Vite redirige `/api` (incluido `/api/v1`) hacia `http://localhost:8080` cuando se
  desactiva el modo mock.

Todas las variables expuestas al navegador deben comenzar con `VITE_`.
