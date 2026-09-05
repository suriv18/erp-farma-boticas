# Dockerizar service-botica Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Levantar el backend `service-botica` (Java 25 / Spring Boot 4.1) junto con PostgreSQL usando `docker compose up`, con una imagen de aplicación multi-stage lista tanto para desarrollo local como como base reutilizable en CI/producción.

**Architecture:** Dockerfile multi-stage en `service-botica/Dockerfile` (stage `build` con JDK 25 que compila `bootstrap-app:bootJar`, stage `runtime` con JRE 25 que solo contiene el jar final), construido con contexto en la raíz del monorepo porque `bootstrap-app/build.gradle` incorpora migraciones Flyway desde `docs/cadena-farmacias-docs/database/` vía un `sourceSet` adicional. `docker-compose.yml` en la raíz orquesta el servicio `app` y un servicio `postgres:18`, con secretos inyectados vía `.env` (gitignored) a partir de una plantilla `.env.example` versionada.

**Tech Stack:** Docker, Docker Compose, Eclipse Temurin 25 (jdk/jre), PostgreSQL 18, Gradle 9.5.1 (wrapper existente).

## Global Constraints

- Contexto de build de Docker = raíz del repositorio (`erp-botica/`), no `service-botica/` en solitario — requerido para que Gradle resuelva `sourceSets.main.resources.srcDir rootProject.file('../docs/cadena-farmacias-docs/database')` (ver `service-botica/bootstrap-app/build.gradle`).
- No modificar `application.yaml`, `build.gradle` ni ningún archivo de configuración existente del backend — el Dockerfile se adapta a la estructura actual.
- `BOTICA_JWT_SECRET` no tiene valor por defecto en el código (`SecurityModuleConfiguration` falla el arranque sin él) y debe ser una clave HMAC-256 (≥256 bits) codificada en Base64.
- Imágenes verificadas como existentes al momento de escribir este plan: `eclipse-temurin:25-jdk`, `eclipse-temurin:25-jre`, `postgres:18`.
- `.env` ya está en `.gitignore` raíz — no se necesita tocar ese archivo.
- No se agrega hot-reload/dev-mode con bind mounts, no se dockeriza el frontend, no se configura pipeline de CI — fuera de alcance de este plan (ver spec).

---

### Task 1: `.dockerignore` en la raíz del repo

**Files:**
- Create: `.dockerignore` (raíz del repo)

**Interfaces:**
- Consumes: nada.
- Produces: el archivo que Docker usa para filtrar el contexto de build en todas las tareas siguientes.

- [ ] **Step 1: Crear el archivo `.dockerignore`**

Ruta: `C:\ambiente-dev\PROYECTOS-2026\erp-botica\.dockerignore`

```
# No se necesita para compilar bootstrap-app
frontend/
graphify-out/
.git/
.github/

# Documentacion no requerida por el build de Gradle
docs/arquitectura/
docs/cadena-farmacias-docs/DOCUMENTACION_COMPLETA.md
docs/cadena-farmacias-docs/README.md
docs/cadena-farmacias-docs/docs/
docs/cadena-farmacias-docs/database/README.md
docs/cadena-farmacias-docs/database/propuestas/
docs/cadena-farmacias-docs/database/cadena_farmacias_postgresql18.sql
docs/cadena-farmacias-documentacion-md.zip
docs/db/
docs/decisiones-adr/
docs/sprint/
docs/superpowers/
docs/README.md

# Artefactos de build ya generados (se regeneran dentro del contenedor)
service-botica/build/
service-botica/**/build/
service-botica/.gradle/
service-botica/**/.gradle/
service-botica/.idea/
service-botica/*.iml

# Editor/OS
.vscode/
.idea/
*.swp
.DS_Store
Thumbs.db

# Secretos locales
.env
.env.local
```

Nota: `docs/cadena-farmacias-docs/database/migrations/` **no** se excluye — es la carpeta que el `sourceSet` de `bootstrap-app` necesita.

- [ ] **Step 2: Verificar que el patrón no excluye lo necesario**

Ejecutar desde la raíz del repo:
```bash
docker build -f service-botica/Dockerfile --target build -t erp-botica-build-context-check . --progress=plain 2>&1 | head -5
```
En este punto el comando fallará porque el `Dockerfile` todavía no existe (se crea en la Task 2) — eso es esperado. El objetivo de este step es solo confirmar que Docker no arroja un error de "no such file or directory" sobre `.dockerignore` en sí. Si Docker reporta `Dockerfile: no such file or directory`, es el resultado correcto para este punto del plan.

- [ ] **Step 3: Commit**

```bash
git add .dockerignore
git commit -m "chore(docker): agregar .dockerignore raiz para el build de service-botica"
```

---

### Task 2: Dockerfile multi-stage de `service-botica`

**Files:**
- Create: `service-botica/Dockerfile`

**Interfaces:**
- Consumes: contexto de build = raíz del repo (definido en Task 1 vía `.dockerignore`); espera encontrar `service-botica/` y `docs/cadena-farmacias-docs/database/` dentro del contexto.
- Produces: imagen Docker que expone el puerto `8080` y ejecuta `service-botica.jar`; usada por `docker-compose.yml` en la Task 4.

- [ ] **Step 1: Crear `service-botica/Dockerfile`**

Ruta: `C:\ambiente-dev\PROYECTOS-2026\erp-botica\service-botica\Dockerfile`

```dockerfile
# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY service-botica/ ./service-botica/
COPY docs/cadena-farmacias-docs/database/ ./docs/cadena-farmacias-docs/database/

WORKDIR /workspace/service-botica
RUN chmod +x gradlew
RUN ./gradlew :bootstrap-app:bootJar --no-daemon

FROM eclipse-temurin:25-jre AS runtime
RUN groupadd --system botica && useradd --system --gid botica --home /app botica
WORKDIR /app

COPY --from=build /workspace/service-botica/bootstrap-app/build/libs/service-botica.jar ./service-botica.jar

RUN chown -R botica:botica /app
USER botica

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "service-botica.jar"]
```

Nota sobre `HEALTHCHECK`: la imagen `eclipse-temurin:25-jre` está basada en Ubuntu y no trae `curl` preinstalado. Se resuelve en el Step 2.

- [ ] **Step 2: Instalar `curl` en el stage `runtime` para el healthcheck**

Editar `service-botica/Dockerfile`, agregando la instalación de `curl` antes de crear el usuario no-root:

```dockerfile
FROM eclipse-temurin:25-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system botica && useradd --system --gid botica --home /app botica
WORKDIR /app
```

(Reemplaza el bloque `RUN groupadd ...` original agregando la línea de `apt-get` antes.)

- [ ] **Step 3: Verificar que la imagen compila**

Desde la raíz del repo:
```bash
docker build -f service-botica/Dockerfile -t service-botica:local .
```
Expected: el build completa las dos etapas (`build` y `runtime`) sin errores, terminando con `Successfully tagged service-botica:local` (o el mensaje equivalente de BuildKit, `naming to docker.io/library/service-botica:local done`). Si Gradle falla por no encontrar `docs/cadena-farmacias-docs/database/migrations`, revisar que `.dockerignore` (Task 1) no la esté excluyendo.

- [ ] **Step 4: Verificar que la imagen arranca en modo standalone (sin Postgres, solo para confirmar que el jar es válido)**

```bash
docker run --rm service-botica:local java -jar service-botica.jar --spring.main.web-application-type=none 2>&1 | head -30
```
Expected: el log muestra el arranque de Spring hasta el intento de conexión a base de datos (fallará ahí porque no hay Postgres disponible en este comando puntual — eso es esperado y correcto; solo se está confirmando que el jar arranca y no hay un error de empaquetado). Si aparece `NoClassDefFoundError` o `Error: Unable to access jarfile`, el build del jar está mal — revisar Step 1-3.

- [ ] **Step 5: Commit**

```bash
git add service-botica/Dockerfile
git commit -m "feat(docker): agregar Dockerfile multi-stage para service-botica"
```

---

### Task 3: Plantilla de variables de entorno `.env.example`

**Files:**
- Create: `.env.example` (raíz del repo)

**Interfaces:**
- Consumes: nada.
- Produces: plantilla que el desarrollador copia a `.env` (Task 5, verificación manual) y que `docker-compose.yml` (Task 4) referencia vía `env_file`.

- [ ] **Step 1: Crear `.env.example`**

Ruta: `C:\ambiente-dev\PROYECTOS-2026\erp-botica\.env.example`

```dotenv
# Copiar este archivo a ".env" y completar los valores.
# ".env" esta en .gitignore y nunca debe commitearse.

# --- PostgreSQL (usado por el servicio "postgres" de docker-compose) ---
POSTGRES_DB=erp_botica
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# --- Conexion de service-botica a PostgreSQL ---
# El host "postgres" es el nombre del servicio dentro de la red de docker-compose.
DB_URL=jdbc:postgresql://postgres:5432/erp_botica
DB_USERNAME=postgres
DB_PASSWORD=postgres

# --- Servidor ---
SERVER_PORT=8080

# --- JWT (obligatorio, sin valor por defecto en el codigo) ---
# Debe ser una clave HMAC-256 (>=256 bits) codificada en Base64.
# Generarla con: openssl rand -base64 32
BOTICA_JWT_SECRET=
BOTICA_JWT_ISSUER=service-botica
BOTICA_JWT_AUDIENCE=service-botica-api
BOTICA_ACCESS_TOKEN_TTL=PT10M
BOTICA_REFRESH_TOKEN_TTL=P7D
BOTICA_PASSWORD_RESET_TTL=PT30M
BOTICA_MAX_FAILED_ATTEMPTS=5
BOTICA_LOCK_DURATION=PT15M
```

- [ ] **Step 2: Commit**

```bash
git add .env.example
git commit -m "docs(docker): agregar plantilla .env.example para el entorno docker-compose"
```

---

### Task 4: `docker-compose.yml`

**Files:**
- Create: `docker-compose.yml` (raíz del repo)

**Interfaces:**
- Consumes: `service-botica/Dockerfile` (Task 2), variables de `.env` (Task 3 define la plantilla; el `.env` real lo crea el desarrollador).
- Produces: comando `docker compose up` que levanta ambos servicios.

- [ ] **Step 1: Crear `docker-compose.yml`**

Ruta: `C:\ambiente-dev\PROYECTOS-2026\erp-botica\docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10

  app:
    build:
      context: .
      dockerfile: service-botica/Dockerfile
    depends_on:
      postgres:
        condition: service_healthy
    env_file:
      - .env
    ports:
      - "${SERVER_PORT:-8080}:8080"

volumes:
  postgres-data:
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "feat(docker): agregar docker-compose.yml con app y postgres"
```

---

### Task 5: Verificación end-to-end del entorno completo

**Files:**
- No se crean ni modifican archivos en esta tarea — es puramente de verificación manual.

**Interfaces:**
- Consumes: todo lo producido en Tasks 1-4.
- Produces: confirmación de que el diseño funciona de punta a punta.

- [ ] **Step 1: Crear el `.env` real local (no se commitea)**

```bash
cp .env.example .env
```
Editar `.env` y reemplazar `BOTICA_JWT_SECRET=` con un valor generado:
```bash
openssl rand -base64 32
```
Pegar el resultado como valor de `BOTICA_JWT_SECRET` en `.env`.

- [ ] **Step 2: Levantar el entorno completo**

Desde la raíz del repo:
```bash
docker compose up --build
```
Expected: el log muestra primero `postgres` alcanzando estado `healthy`, luego `app` construyéndose (reutiliza Task 2 si ya se construyó antes) y arrancando Spring Boot, terminando con una línea tipo `Started ErpBoticaApplication in N seconds` sin excepciones de Flyway ni de conexión a base de datos.

- [ ] **Step 3: Verificar el healthcheck desde el host**

En otra terminal, con el compose corriendo:
```bash
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}` (o un JSON equivalente con `"status":"UP"` como campo raíz).

- [ ] **Step 4: Verificar que el estado de `docker compose ps` reporta el healthcheck de la imagen como `healthy`**

```bash
docker compose ps
```
Expected: el servicio `app` muestra `(healthy)` en la columna de estado una vez transcurrido el `start-period` de 60s definido en el `HEALTHCHECK` del Dockerfile.

- [ ] **Step 5: Verificar persistencia de datos**

```bash
docker compose down
docker compose up -d
docker compose ps
```
Expected: ambos servicios vuelven a `healthy` sin que Flyway reporte migraciones repetidas fallidas (confirma que el volumen `postgres-data` persistió el esquema entre reinicios).

- [ ] **Step 6: Apagar el entorno**

```bash
docker compose down
```

- [ ] **Step 7: Commit final (si hubo algún ajuste durante la verificación)**

Si algún archivo de las Tasks 1-4 requirió un ajuste durante esta verificación, commitearlo:
```bash
git add -A
git commit -m "fix(docker): ajustes de verificacion end-to-end"
```
Si no hubo ajustes, este step no aplica — no crear un commit vacío.

---

## Self-Review (completado por quien escribió el plan)

**Cobertura del spec:**
- Dockerfile multi-stage (build JDK 25 / runtime JRE 25) → Task 2. ✓
- Usuario no-root → Task 2, Step 2. ✓
- Healthcheck vía `/actuator/health` → Task 2, Steps 1-2 (Dockerfile) y Task 5, Steps 3-4 (verificación). ✓
- Contexto de build = raíz del repo → Task 1 (`.dockerignore` raíz) y Task 2 (`COPY service-botica/` + `COPY docs/cadena-farmacias-docs/database/`). ✓
- docker-compose con app + postgres → Task 4. ✓
- Persistencia de Postgres → Task 4 (volumen nombrado) + Task 5 Step 5 (verificación). ✓
- Secretos vía `.env.example` + `.env` gitignored → Task 3 + Task 5 Step 1. ✓
- Instrucción de generación de `BOTICA_JWT_SECRET` → Task 3 (comentario en `.env.example`) y Task 5 Step 1 (comando real). ✓

**Placeholders:** ninguno — cada step tiene contenido completo y ejecutable.

**Consistencia de nombres:** `service-botica.jar` (nombre real definido en `bootstrap-app/build.gradle` vía `archiveFileName`) se usa igual en Task 2 (Dockerfile) y coincide con lo documentado en `CLAUDE.md`. El puerto `8080` es consistente entre `application.yaml` (default `SERVER_PORT:8080`), Dockerfile (`EXPOSE 8080`) y `docker-compose.yml` (`"${SERVER_PORT:-8080}:8080"`).
