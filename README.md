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

- **`service-botica/`**: monolito modular (17 bounded contexts como subproyectos Gradle). El módulo `security` (IAM: login, JWT con refresh y rotación, RBAC por ámbito organizacional, sesiones, dispositivos) es el más completo y tiene endpoints REST, persistencia JPA/JDBC y migraciones Flyway funcionando contra PostgreSQL real. El módulo `organizacion` tiene un primer slice parcial. El resto de los módulos de negocio (ventas, inventario, compras, etc.) siguen siendo scaffolds sin lógica.
- **`frontend/`**: scaffold React navegable, no un ERP funcional. El login es simulado (no llama al backend), el dashboard usa datos mock, y la mayoría de los módulos (catálogo, ventas, POS, caja, clientes, seguridad) son placeholders visuales.
- **Docker**: el backend puede levantarse junto con PostgreSQL con un solo comando (`docker compose up`), ver más abajo.

Una pantalla visible o un módulo detectado en el código no equivalen a una capacidad terminada.

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

La aplicación queda disponible en `http://localhost:5173` (modo mock con MSW, sin necesitar el backend).

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
