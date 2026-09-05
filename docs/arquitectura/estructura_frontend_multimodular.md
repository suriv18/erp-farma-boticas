# Estructura frontend modular y pragmática

> **Estado: LEGADO / REFERENCIA DE SCAFFOLD.** El flujo vigente y la diferencia entre UI preparada
> y capacidad implementada están en
> [`GOV-FAR-001`](../cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md).

> Propuesta simplificada para ERP Boticas. El objetivo es organizar el frontend sin
> trasladar innecesariamente al cliente la complejidad arquitectónica del backend.

## 1. Decisión arquitectónica

Se utilizará un **workspace liviano con pnpm** y una estructura modular por features.
El `package.json` raíz actuará como padre del frontend, centralizando versiones, scripts y
configuración. No existirá un módulo `app-core` ni una variante `core/web/native` por cada
dominio.

La arquitectura inicial tendrá:

- Una aplicación web para ERP y POS.
- Un paquete para componentes visuales web reutilizables.
- Un paquete para el cliente HTTP y los contratos de API.
- Features organizadas dentro de la aplicación.

Ecommerce y aplicaciones móviles se incorporarán cuando su sprint comience y exista una
decisión concreta de despliegue. No se crearán proyectos vacíos por anticipado.

## 2. Por qué se simplifica

La propuesta anterior anticipaba aproximadamente 50 paquetes y seis aplicaciones. Eso
introducía demasiados `package.json`, configuraciones, dependencias, APIs públicas y pasos
de compilación antes de entregar funcionalidad.

El roadmap actual concentra el ERP durante los primeros sprints, agrega el POS en el sprint
5 y recién trabaja ecommerce y experiencia móvil en el sprint 8. La estructura debe seguir
esa evolución real.

Además, un monorepo facilita compartir código, pero también aumenta la complejidad del
tooling. Por eso se adopta pnpm workspaces con el menor número posible de proyectos.
Turborepo será opcional y se agregará únicamente cuando el tiempo de CI o la cantidad de
aplicaciones justifiquen cache y ejecución incremental.

## 3. Principios

1. Crear una aplicación solamente cuando necesite despliegue independiente.
2. Crear un paquete solamente cuando tenga dos consumidores reales o sea una frontera técnica estable.
3. Organizar el código de negocio por feature, no por carpetas globales gigantes.
4. Mantener juntas las piezas que cambian juntas.
5. Separar UI, acceso a datos y modelo solo hasta el nivel necesario.
6. No replicar agregados, repositorios ni capas completas del backend.
7. El backend continúa siendo la autoridad de permisos, stock, precios y transacciones.
8. Evitar stores globales con copias del estado remoto.
9. Extraer reutilización después de comprobarla, no antes.
10. Favorecer imports directos y APIs pequeñas sobre abstracciones genéricas.

## 4. Estructura inicial

```text
frontend/
├── package.json                         # Padre privado del workspace
├── pnpm-workspace.yaml
├── pnpm-lock.yaml
├── .node-version                        # Misma versión local y CI
├── .gitignore
├── tsconfig.base.json
├── eslint.config.js
├── prettier.config.js
├── apps/
│   └── erp-web/
│       ├── package.json
│       ├── vite.config.ts
│       ├── tsconfig.json
│       ├── tsconfig.app.json
│       ├── tsconfig.node.json
│       ├── index.html
│       ├── .env                         # Valores públicos comunes
│       ├── .env.development             # Valores no secretos para arranque local
│       ├── .env.production              # Valores públicos del artefacto
│       ├── .env.example                 # Contrato de variables
│       └── src/
│           ├── main.tsx
│           ├── app/
│           ├── features/
│           ├── shared/
│           ├── test/
│           └── assets/
└── packages/
    ├── ui-web/
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── src/
    └── api-client/
        ├── package.json
        ├── tsconfig.json
        ├── openapi/
        └── src/
```

La estructura inicial tiene **tres workspaces**: una aplicación y dos paquetes. El padre
no se publica ni contiene pantallas.

## 5. Responsabilidad del padre

El padre centraliza:

- Versión de Node y pnpm.
- Instalación y lockfile.
- Scripts comunes.
- TypeScript, ESLint y Prettier.
- Catálogo de versiones, si resulta útil.

No contiene:

- Componentes React.
- Providers de aplicación.
- Reglas de negocio.
- Estado global.
- Adaptadores web o móviles.

### `package.json`

```json
{
  "name": "@boticas/frontend-parent",
  "private": true,
  "version": "0.0.0",
  "packageManager": "pnpm@<version-fijada>",
  "engines": {
    "node": "<version-LTS-fijada>"
  },
  "scripts": {
    "dev": "pnpm --filter @boticas/erp-web dev",
    "build": "pnpm --filter @boticas/erp-web build",
    "lint": "pnpm -r --if-present lint",
    "typecheck": "pnpm -r --if-present typecheck",
    "test": "pnpm -r --if-present test",
    "check": "pnpm lint && pnpm typecheck && pnpm test && pnpm build",
    "why:react": "pnpm --recursive why react"
  }
}
```

### `pnpm-workspace.yaml`

```yaml
packages:
  - apps/*
  - packages/*
```

Las dependencias internas se declaran con `workspace:*` para garantizar que pnpm utilice
el paquete local.

`<version-fijada>` no permanece en el proyecto real: durante el scaffold se reemplaza por
la versión exacta probada y se confirma en `pnpm-lock.yaml`. Local y CI utilizan esa misma
versión. La instalación siempre se ejecuta desde la raíz del workspace.

## 6. Aplicación `erp-web`

ERP y POS comienzan como una sola aplicación porque comparten autenticación, usuarios,
permisos, catálogo, clientes, inventario y ciclo de entrega.

```text
apps/erp-web/src/
├── main.tsx
├── app/
│   ├── providers.tsx
│   ├── router.tsx
│   ├── query-client.ts
│   ├── environment.ts
│   ├── error-boundary.tsx
│   └── layouts/
│       ├── admin-layout.tsx
│       ├── pos-layout.tsx
│       └── public-layout.tsx
├── features/
│   ├── auth/
│   ├── seguridad/
│   ├── organizacion/
│   ├── catalogo/
│   ├── inventario/
│   ├── compras/
│   ├── ventas/
│   ├── pos/
│   ├── caja/
│   └── clientes/
├── shared/
│   ├── components/
│   ├── hooks/
│   ├── utils/
│   └── constants/
└── assets/
```

`app/` contiene composición transversal de esa aplicación. `features/` contiene
funcionalidad del negocio. `shared/` solo contiene piezas técnicas locales que realmente
son usadas por varias features.

## 7. Estructura de una feature

Se utiliza una plantilla corta:

```text
features/inventario/
├── index.ts
├── routes.tsx
├── pages/
│   ├── stock-list.page.tsx
│   └── stock-detail.page.tsx
├── components/
│   ├── stock-table.tsx
│   └── expiration-badge.tsx
├── api/
│   ├── inventory.api.ts
│   ├── inventory.queries.ts
│   └── inventory.mutations.ts
├── schemas/
│   └── adjustment.schema.ts
├── model/                              # Opcional
│   ├── inventory.types.ts
│   └── inventory.mappers.ts
└── tests/
```

Reglas:

- `pages`: composición de la pantalla.
- `components`: UI específica de la feature.
- `api`: endpoints, query options y mutations.
- `schemas`: validación de formularios y entradas.
- `model`: se crea solo cuando existen tipos, mappers o reglas de interacción relevantes.
- `index.ts`: API pública para otras partes de la aplicación.

Una feature CRUD sencilla puede omitir `model`, `schemas` o `tests` específicos si no los
necesita. No se crean carpetas vacías para completar una plantilla.

## 8. Dependencias permitidas

```text
app
  -> features
  -> shared local
  -> @boticas/ui-web
  -> @boticas/api-client

features
  -> shared local
  -> @boticas/ui-web
  -> @boticas/api-client

ui-web
  -> no depende de features ni de api-client

api-client
  -> no depende de React, features ni ui-web
```

Una feature no importa archivos internos de otra feature. Cuando sea necesaria una
composición entre ventas, clientes y catálogo, se realiza en una página coordinadora o a
través de sus exports públicos.

```ts
// Evitar
import { ProductPicker } from '../catalogo/components/internal/ProductPicker';

// Permitido
import { ProductPicker } from '../catalogo';
```

No se exige convertir cada feature en paquete para conseguir este límite. ESLint puede
restringir imports internos dentro de la misma aplicación.

## 9. Paquetes compartidos iniciales

### 9.1. `@boticas/ui-web`

Contiene componentes visuales reutilizables:

```text
packages/ui-web/src/
├── button/
├── dialog/
├── form-field/
├── data-table/
├── feedback/
├── layout/
├── theme/
└── index.ts
```

No contiene `ProductTable`, `SaleForm` ni reglas de inventario. Esos elementos pertenecen
a sus features.

Los tokens de diseño permanecen dentro de `ui-web`. Se extraerán a `design-tokens` cuando
exista una aplicación nativa que realmente los consuma.

`ui-web` se consume como paquete interno Just-in-Time: exporta TypeScript/TSX ESM y Vite lo
compila junto con la aplicación. React y React DOM son `peerDependencies`, nunca
`dependencies`, para evitar una segunda copia de React.

```json
{
  "name": "@boticas/ui-web",
  "private": true,
  "type": "module",
  "exports": {
    ".": "./src/index.ts",
    "./styles.css": "./src/styles.css"
  },
  "peerDependencies": {
    "react": ">=19 <20",
    "react-dom": ">=19 <20"
  },
  "scripts": {
    "lint": "eslint src",
    "typecheck": "tsc --noEmit",
    "test": "vitest run"
  }
}
```

El manifiesto real también declara en `devDependencies` React, React DOM y sus tipos para
poder validar el paquete aisladamente; todas esas versiones deben coincidir con la
aplicación y quedar fijadas por el lockfile.

### 9.2. `@boticas/api-client`

Centraliza la frontera HTTP:

```text
packages/api-client/src/
├── client.ts
├── config.ts
├── api-error.ts
├── problem-details.ts
├── generated/                           # Código OpenAPI
└── index.ts
```

Responsabilidades:

- URL base y headers técnicos.
- Timeout y cancelación.
- Correlation ID.
- Traducción de Problem Details a `ApiError`.
- Tipos o cliente generados desde OpenAPI.
- Renovación de sesión coordinada con la aplicación.

No contiene hooks de inventario o ventas. Los hooks de TanStack Query permanecen dentro de
la feature que conoce su semántica.

También se consume Just-in-Time y exporta ESM:

```json
{
  "name": "@boticas/api-client",
  "private": true,
  "type": "module",
  "exports": {
    ".": "./src/index.ts"
  },
  "scripts": {
    "generate": "openapi-generator-cli generate -c openapi/generator-config.yaml",
    "lint": "eslint src",
    "typecheck": "tsc --noEmit",
    "test": "vitest run"
  }
}
```

El esquema OpenAPI, la configuración del generador y el código generado se versionan. De
este modo un clon limpio no necesita que el backend esté encendido para compilar. CI vuelve
a generar el cliente y falla si detecta diferencias no confirmadas.

## 10. Aplicación pragmática de los patrones

Los patrones solicitados para el backend no se reproducen mecánicamente en React.

| Patrón | Aplicación en el frontend |
|---|---|
| DDD | Features y nombres alineados al dominio |
| Clean Architecture | Separar pantalla, acceso a datos y modelo cuando la complejidad lo requiera |
| Ports and Adapters | Solo para hardware o integraciones realmente sustituibles |
| CQRS | Diferenciar queries y mutations, sin command bus |
| Result Pattern | Normalizar errores en el límite HTTP; no envolver cada función |

### DDD

Se comparte el lenguaje: producto, lote, stock, compra, venta, cliente y caja. El frontend
no duplica agregados ni invariantes del backend. Una validación local mejora experiencia,
pero la decisión final permanece en el servidor.

### Ports and Adapters

Se justifica un puerto para:

- Impresora de comprobantes.
- Lector de código de barras.
- Cámara o almacenamiento seguro nativo.
- Proveedor externo que deba sustituirse.

No se crea un puerto y un adaptador para cada llamada HTTP normal.

### CQRS

```text
features/catalogo/api/
├── catalog.queries.ts
└── catalog.mutations.ts
```

Las queries leen y se almacenan en cache. Las mutations expresan cambios e invalidan las
queries relacionadas. Esto es suficiente para el frontend; no se necesita mediator ni bus
de commands.

### Result y errores

El cliente HTTP interpreta RFC 9457 y lanza un `ApiError` tipado. TanStack Query utiliza
esa excepción para activar `isError`. Los formularios traducen errores de validación del
backend a sus campos.

Un tipo `Result<T, E>` se utilizará solamente en procesos imperativos donde el consumidor
deba manejar explícitamente ambos resultados. No será el retorno obligatorio de hooks,
componentes o utilidades.

## 11. Estado

```text
Estado remoto        -> TanStack Query
Estado de URL        -> React Router
Estado de formulario -> React Hook Form
Validación            -> Zod
Estado local          -> useState / useReducer
Sesión                -> provider pequeño en app/auth
```

No se introduce Zustand o Redux inicialmente. Solo se agregará un store cuando exista
estado de cliente compartido que no sea remoto, de URL, formulario o sesión.

React recomienda mantener una representación mínima del estado y evitar información
redundante o duplicada. Por eso no se copian respuestas de TanStack Query a otro store.

Las claves de queries deben incluir empresa y sucursal cuando los datos dependan de ese
alcance:

```ts
export const stockQueryKey = (
  tenantId: string,
  branchId: string,
  filters: StockFilters
) => ['stock', tenantId, branchId, filters] as const;
```

Al cambiar de empresa, sucursal o sesión se cancelan solicitudes y se elimina la cache
privada del alcance anterior.

## 12. Routing

React Router administra:

- URL y navegación.
- Layout administrativo y layout POS.
- Protección visual de rutas.
- Lazy loading por feature.
- Estados de error por ruta.

TanStack Query continúa siendo propietario del estado remoto. Los loaders pueden precargar
la misma query, pero no deben duplicar la implementación HTTP.

Los guards mejoran la experiencia, pero no proporcionan seguridad. El backend debe validar
todos los permisos.

## 13. Autenticación y configuración

- La feature `auth` pertenece inicialmente a `erp-web`.
- No se crea un paquete `auth-core`, `auth-web` y `auth-native`.
- Las credenciales de sesión no se almacenan en `localStorage`.
- Se prefieren cookies `HttpOnly`, `Secure` y `SameSite` o una sesión mediante BFF.
- Si se usan cookies, el backend implementa protección CSRF.
- Las variables `VITE_*` son públicas y nunca contienen secretos.
- La configuración pública se valida al iniciar la aplicación.

Si ecommerce necesita autenticación diferente, tendrá su propia feature. Se extraerá un
paquete común solo si ambas implementaciones comparten comportamiento real y estable.

## 14. Pruebas

La estrategia se mantiene sencilla:

```text
Funciones, schemas y mappers -> Vitest
Componentes y páginas        -> Testing Library
Integración HTTP              -> MSW
Flujos críticos               -> Playwright
```

Prioridades:

- Login y permisos visuales.
- Catálogo e inventario.
- Apertura, venta y cierre de caja.
- Errores 401, 403, 409 y validaciones.
- Cambio de empresa o sucursal.
- Reintentos e idempotencia de operaciones críticas.

Los helpers de pruebas se mantienen dentro de `erp-web/src/test` hasta que otra aplicación
necesite exactamente los mismos providers y mocks. Recién entonces se evalúa un paquete de
testing.

## 15. Evolución de las aplicaciones

### Etapa 1: sprints 0 a 4

```text
apps/erp-web
packages/ui-web
packages/api-client
```

Se implementan seguridad, organización, catálogo, inventario y compras.

### Etapa 2: sprints 5 a 7

POS se implementa como features y un layout dentro de `erp-web`:

```text
erp-web/src/features/
├── pos/
├── caja/
└── comprobantes/
```

POS se convierte en `apps/pos-web` solamente si aparece al menos una de estas condiciones:

- Despliegue y versionado independientes.
- Requisitos offline/PWA que interfieren con ERP.
- Hardware o seguridad con configuración diferente.
- Bundle y rendimiento claramente perjudicados.
- Equipo con ciclo de entrega independiente.

### Etapa 3: sprint 8

Ecommerce sí puede convertirse en aplicación separada porque es pública, tiene navegación,
autenticación, SEO y despliegue distintos:

```text
apps/
├── erp-web/
└── ecommerce-web/
```

Ecommerce reutiliza `ui-web` y `api-client` solo donde el comportamiento sea compatible.
Sus páginas, carrito, checkout y componentes comerciales permanecen en su aplicación.

### Etapa 4: móvil

Primero se decide entre responsive/PWA y Expo. No se crea un workspace móvil hasta tomar
esa decisión.

Si se elige Expo:

```text
apps/
├── erp-web/
├── ecommerce-web/
└── cliente-mobile/
```

Expo advierte que los monorepos añaden complejidad de resolución y módulos nativos. Se
mantendrá una sola versión compatible de React, React Native y Expo. `ui-native` y
`design-tokens` se extraerán únicamente si tienen consumidores reales.

## 16. Cuándo crear un paquete nuevo

Un módulo se extrae a `packages/*` cuando cumple todos estos criterios:

1. Tiene dos o más consumidores reales.
2. Conserva el mismo significado en esos consumidores.
3. Su API pública es pequeña y estable.
4. Puede probarse sin iniciar una aplicación específica.
5. No obliga a instalar dependencias innecesarias.
6. Reduce más mantenimiento del que introduce.

Excepciones aceptables desde el inicio:

- `ui-web`, porque representa el design system acordado.
- `api-client`, porque representa una frontera técnica común y generada.

No se crea un paquete `shared` global. Ese nombre suele convertirse en un depósito sin
propietario. El código compartido debe tener un propósito concreto y un nombre concreto.

## 17. Cuándo agregar Turborepo

pnpm workspaces es suficiente inicialmente. Se incorpora Turborepo si:

- Existen varias aplicaciones construidas en CI.
- Los builds y tests consumen un tiempo relevante.
- Se necesita ejecutar solo proyectos afectados.
- La cache local o remota produce una mejora medible.

Agregar Turborepo no cambia la estructura `apps/*` y `packages/*`; solamente coordina las
tareas ya existentes.

## 18. Antipatrones que deben evitarse

- Un paquete por cada carpeta o feature.
- Variantes `core/web/native` creadas por anticipado.
- Copiar la arquitectura del backend dentro de React.
- Carpetas vacías para cumplir una plantilla.
- `shared`, `common` o `utils` globales sin propietario.
- Hooks genéricos que esconden reglas de negocio.
- Un store global con sesión, inventario, ventas y clientes.
- Duplicar estado remoto en Zustand o Redux.
- Acceder a archivos internos de otra feature.
- Usar DTO de API directamente en todos los formularios.
- Crear microfrontends sin despliegue independiente.
- Crear una aplicación móvil antes de decidir PWA versus Expo.

## 19. Secuencia recomendada

1. Crear el padre pnpm y `apps/erp-web`.
2. Crear `ui-web` con los componentes mínimos del layout y formularios.
3. Crear `api-client` con configuración, OpenAPI y manejo uniforme de errores.
4. Implementar `auth`, `seguridad` y `organizacion` como features locales.
5. Implementar catálogo e inventario y ajustar la plantilla de feature con experiencia real.
6. Añadir compras, ventas, POS y caja en la misma aplicación.
7. Medir si POS necesita separación antes de crear `pos-web`.
8. Crear ecommerce en su sprint, no antes.
9. Decidir PWA o Expo con requisitos móviles concretos.
10. Extraer nuevos paquetes solamente al descubrir duplicación estable.

## 20. Criterios de aceptación

- La estructura inicial no supera una aplicación y dos paquetes compartidos.
- Una feature puede entenderse sin recorrer múltiples workspaces.
- No existen ciclos ni imports a internals de otra feature.
- UI y HTTP permanecen desacoplados de las reglas específicas de negocio.
- El estado remoto tiene una única fuente de verdad.
- El padre centraliza scripts y configuración sin contener código de aplicación.
- POS puede evolucionar dentro del ERP y separarse sin reescribir sus features.
- Ecommerce y móvil no generan costo de mantenimiento antes de su sprint.
- Cada paquete adicional documenta consumidores y razón de extracción.

## 21. Contrato de instalación y ejecución

La estructura se considera válida solamente si un clon limpio puede instalar, validar,
compilar y arrancar sin configuraciones ocultas en la computadora de un desarrollador.

### 21.1. Manifiesto de `erp-web`

El nombre debe coincidir con el filtro utilizado por el padre y cada dependencia importada
debe declararse en este workspace. No se depende del hoisting de otra aplicación.

```json
{
  "name": "@boticas/erp-web",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "lint": "eslint src",
    "typecheck": "tsc -b --pretty false",
    "test": "vitest run"
  },
  "dependencies": {
    "@boticas/api-client": "workspace:*",
    "@boticas/ui-web": "workspace:*"
  }
}
```

El fragmento muestra las dependencias internas. El manifiesto real declara además React,
React DOM, React Router, TanStack Query, React Hook Form, Zod y cualquier otra librería que
importe la aplicación. Vite, TypeScript, Vitest, ESLint y sus plugins se declaran como
`devDependencies` del workspace que los ejecuta o del padre cuando su uso sea realmente
global.

### 21.2. Una sola estrategia para paquetes internos

`ui-web` y `api-client` son paquetes Just-in-Time:

- Exportan archivos TypeScript ESM desde `src` mediante `exports`.
- No generan `dist` y no tienen script `build`.
- Vite compila sus fuentes como parte de `erp-web`.
- Cada paquete sí tiene `lint`, `typecheck` y `test`.
- No utilizan aliases TypeScript que el consumidor no pueda resolver.

No se mezcla esta estrategia con imports hacia un `dist` inexistente. Si en el futuro un
paquete se convierte en compilado, deberá generar JavaScript, declaraciones y source maps
antes del build de sus consumidores.

### 21.3. TypeScript compatible con Vite

La configuración web utiliza resolución de módulos para bundler y no emite JavaScript;
Vite es responsable del bundle:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noEmit": true,
    "isolatedModules": true,
    "noUncheckedIndexedAccess": true,
    "skipLibCheck": true
  }
}
```

`tsconfig.app.json` incluye `src`; `tsconfig.node.json` incluye `vite.config.ts`. El
`tsconfig.json` de la aplicación referencia ambos. No se utilizan `paths` para importar
workspaces: se usan sus nombres `@boticas/*` y el protocolo `workspace:*`.

### 21.4. Evitar React duplicado

React advierte que dos copias o versiones incompatibles provocan errores de Hooks. Para
evitarlo:

1. `erp-web` declara React y React DOM como dependencias.
2. `ui-web` los declara como `peerDependencies` y como `devDependencies` para sus pruebas.
3. Se mantiene una única versión acordada en el lockfile.
4. No se usa `npm link`; se usa `workspace:*`.
5. Vite agrega una defensa de resolución:

```ts
resolve: {
  dedupe: ['react', 'react-dom']
}
```

El comando `pnpm why:react` debe mostrar un árbol compatible antes de aceptar una
actualización de React.

### 21.5. Vite, puerto y proxy local

```ts
import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    resolve: {
      dedupe: ['react', 'react-dom']
    },
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.DEV_API_TARGET ?? 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    preview: {
      port: 4173,
      strictPort: true
    }
  };
});
```

`strictPort` evita que Vite cambie silenciosamente de puerto y rompa callbacks o CORS. El
proxy permite que el navegador consuma `/api` bajo el mismo origen durante desarrollo. No
se configura `server.cors: true` ni `allowedHosts: true`.

El `api-client` no lee `import.meta.env`; recibe `baseUrl`, credenciales y timeout desde la
composition root de la aplicación. Así permanece ejecutable fuera de Vite y en pruebas.

### 21.6. Variables y modo sin backend

Los archivos versionados contienen únicamente valores públicos y seguros:

```dotenv
# .env
VITE_API_BASE_URL=/api

# .env.development
VITE_API_MODE=mock
DEV_API_TARGET=http://localhost:8080

# .env.production
VITE_API_MODE=http
```

`VITE_API_MODE=mock` permite iniciar la interfaz con MSW aunque el backend todavía no esté
disponible. Para integración local se cambia a `http`. `environment.ts` valida las
variables con Zod y muestra un error de configuración claro durante el bootstrap.

MSW se declara como dependencia de desarrollo de `erp-web`. Sus handlers viven en
`src/test/mocks` y se cargan dinámicamente solo cuando el ambiente validado indica `mock`;
el bundle de producción utiliza siempre el adaptador HTTP.

Todo valor `VITE_*` termina en el bundle y se considera público. Tokens, passwords y API
keys privadas no se colocan en archivos Vite.

### 21.7. OpenAPI reproducible

Para que el proyecto compile desde un clon limpio:

- El contrato OpenAPI usado por el generador está versionado.
- La versión y configuración del generador están fijadas.
- El código generado está versionado inicialmente.
- `pnpm --filter @boticas/api-client generate` produce siempre las mismas rutas.
- CI regenera y verifica que no exista drift.

No se ejecuta la generación automáticamente en cada `dev`, porque eso haría depender el
arranque de una red, del backend o de una herramienta externa no disponible.

### 21.8. React Router en producción

El servidor que publica `erp-web/dist` debe:

- Servir `index.html` como fallback para rutas de la SPA que no sean archivos.
- Enviar `/api/*` al backend y no al fallback HTML.
- Servir assets con hash usando cache de larga duración.
- Servir `index.html` con revalidación para no retener releases anteriores.
- Configurar `base` de Vite si la aplicación se publica bajo un subpath.

Sin el fallback, abrir o refrescar `/inventario/stock` produciría un 404 aunque la ruta
funcione desde la navegación interna. `vite preview` solo valida el artefacto localmente;
no es un servidor de producción.

## 22. Verificación desde un clon limpio

Secuencia obligatoria:

```text
1. Verificar la versión de Node declarada en .node-version
2. Verificar la versión exacta de pnpm declarada en packageManager
3. Ejecutar pnpm install --frozen-lockfile
4. Ejecutar pnpm check
5. Ejecutar pnpm dev
6. Abrir http://localhost:5173
7. Comprobar modo mock sin backend
8. Comprobar modo http con backend y proxy /api
9. Ejecutar pnpm build
10. Ejecutar pnpm --filter @boticas/erp-web preview
11. Validar una ruta profunda y una recarga del navegador
```

Quality gates mínimos de CI:

- Instalación con lockfile inmutable.
- Cero dependencias workspace no declaradas.
- Una sola versión compatible de React y React DOM.
- Lint, typecheck y tests de los tres workspaces.
- Al menos un smoke test real en cada workspace desde el scaffold; no se oculta su ausencia con `passWithNoTests`.
- Generación OpenAPI sin diferencias.
- Build de producción de `erp-web`.
- Smoke test del artefacto y navegación directa a una ruta.
- Prueba de error de configuración cuando falta una variable requerida.

No es posible garantizar ausencia absoluta de fallos futuros solo con un árbol de carpetas.
Este contrato sí elimina los problemas estructurales previsibles y convierte el arranque
limpio en una condición verificable del proyecto.

## 23. Referencias oficiales

- [pnpm Workspaces](https://pnpm.io/workspaces)
- [Turborepo: Structuring a repository](https://turborepo.dev/docs/crafting-your-repository/structuring-a-repository)
- [Turborepo: Internal packages](https://turborepo.dev/docs/core-concepts/internal-packages)
- [Vite: Monorepos and linked dependencies](https://vite.dev/guide/dep-pre-bundling)
- [Vite: Server options and proxy](https://vite.dev/config/server-options)
- [Vite: Environment variables and modes](https://vite.dev/guide/env-and-mode)
- [Vite: Deploying a static site](https://vite.dev/guide/static-deploy)
- [TypeScript: moduleResolution](https://www.typescriptlang.org/tsconfig/moduleResolution)
- [React: Thinking in React](https://react.dev/learn/thinking-in-react)
- [React: Choosing the state structure](https://react.dev/learn/choosing-the-state-structure)
- [React: Invalid Hook Call warning](https://react.dev/warnings/invalid-hook-call-warning)
- [React Router: Picking a mode](https://reactrouter.com/start/modes)
- [TanStack Query: Query keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys)
- [Expo: Work with monorepos](https://docs.expo.dev/guides/monorepos/)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [IETF RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/info/rfc9457/)
