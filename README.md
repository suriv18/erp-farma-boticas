# ERP Botica

Monorepo de una plataforma integral para una cadena de farmacias en Perú: backend modular en Java/Spring Boot y frontend en React, con documentación de dominio y arquitectura versionada junto al código.

## Estructura del repositorio

```text
erp-botica/
├── service-botica/    Backend Java 25 / Spring Boot 4.1, Gradle multimodulo
├── frontend/           Frontend React 19 / Vite, workspace pnpm
├── docs/               Documentacion de negocio, dominio y arquitectura
├── Dockerfile, docker-compose.yml, .env.example   Entorno local con Docker
└── CLAUDE.md           Guia de contexto del repo para Claude Code
```

## Documentación

La documentación de negocio, dominio, arquitectura, seguridad y estándares de desarrollo vive en [`docs/cadena-farmacias-docs/docs/`](docs/cadena-farmacias-docs/docs/), organizada por área (`01-negocio`, `04-dominio`, `05-arquitectura`, `08-seguridad`, etc.). Las carpetas `docs/arquitectura/`, `docs/sprint/`, `docs/decisiones-adr/` y `docs/db/` contienen antecedentes y borradores legados, conservados solo para trazabilidad histórica — ante cualquier discrepancia, prevalece la documentación en `docs/cadena-farmacias-docs/docs/`.

## Estado actual

Matriz de capacidades. *Implementado* = existe código con pruebas unitarias; *Integrado* = el frontend lo consume contra el backend real; *Validado* = hay prueba de integración HTTP/PostgreSQL (Testcontainers) o verificación en navegador contra el backend real.

| Área | Implementado | Integrado | Validado | Pendiente |
| --- | --- | --- | --- | --- |
| `security` (IAM) | Login local, JWT con refresh rotativo, RBAC por ámbito, sesiones, dispositivos, cambio obligatorio de contraseña | Login, cambio de contraseña, permisos, roles y usuarios | `IamApiIntegrationTest` | Sin pendientes verificados en esta revisión |
| `organizacion` | CRUD de empresas, establecimientos, almacenes, terminales POS y estructura corporativa | Gestión completa | `OrganizacionApiIntegrationTest` + navegador | Reglas marcadas POR_VALIDAR |
| `catalogo` | CRUD de 10 de los ~11 catálogos maestros | Categorías y marcas | Solo unitarias; sin `*ApiIntegrationTest` | `tipo_documento_identidad`; UI de los otros 8 catálogos |
| `inventario` | Lotes, posiciones, movimientos con idempotencia | Posiciones, ingreso, ajustes, lotes | `InventarioApiIntegrationTest`, concurrencia | Capacidades fuera de lo listado (el DDL define más tablas que API) |
| `compras` | Proveedores, órdenes y recepciones con ingreso a inventario | Órdenes y recepción | `ComprasApiIntegrationTest`, concurrencia | Capacidades fuera de lo listado |
| `ventas` | Turno de caja, venta presencial en efectivo (FEFO), anulación con turno abierto | Frontend de caja/POS aún sin lógica | `VentaApiIntegrationTest`, concurrencia | IGV, CPE, receta/retención/dispensación (hoy se rechazan SKUs que las exigen), promociones, clientes, otros medios de pago, devolución |
| Resto de módulos | Scaffold | No | No | Todo |

Alcance de la autorización por ámbito organizacional: las operaciones de `ventas` (venta y turno de caja) validan, además del permiso, que el ámbito asignado al usuario cubra el establecimiento, almacén o terminal del recurso. `organizacion`, `inventario` y `compras` aún validan solo el permiso.

- **Frontend**: React 19 con slices reales integrados contra `packages/api-client` (sin mocks) para auth, seguridad, organización, catálogo (categorías y marcas), inventario y compras. El dashboard sigue usando datos mock; ventas, POS, caja y clientes son placeholders.
- **Docker**: el backend puede levantarse junto con PostgreSQL con un solo comando (`docker compose up`), ver más abajo.

Una pantalla visible, un controlador o un módulo detectado en el código no equivalen a una capacidad terminada.

## Cómo levantar el proyecto

### Backend + base de datos con Docker

```bash
cp .env.example .env
# Editar .env y generar BOTICA_JWT_SECRET:
openssl rand -base64 32

docker compose up --build
```

La API queda disponible en `http://localhost:8080` (`GET /actuator/health` para verificar).

### Backend en local (sin Docker)

Requiere JDK 25 y PostgreSQL disponible.

```powershell
cd service-botica
.\gradlew.bat check
.\gradlew.bat :bootstrap-app:bootRun
```

### Frontend

Requiere Node.js 24.14.1 y pnpm 11.9.0.

```bash
cd frontend
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

La aplicación queda disponible en `http://localhost:3000` y, por defecto, habla con el backend real (`VITE_API_MODE=http`, con proxy de `/api` hacia `http://localhost:8080`). Para trabajar sin backend, usar `VITE_API_MODE=mock` (ver `frontend/.env.example`).

## Verificación local

Backend:

```powershell
cd service-botica
.\gradlew.bat check
```

Frontend:

```bash
cd frontend
pnpm check
```

## Para trabajar con Claude Code

Este repositorio incluye [`CLAUDE.md`](CLAUDE.md) con comandos de build/test y la arquitectura de alto nivel del backend y frontend, pensado como contexto de arranque rápido para agentes de código.
