# Dockerizar service-botica — diseño

## Contexto

El backend `service-botica` (Java 25, Spring Boot 4.1, Gradle 9.5.1 multimódulo) no tiene ningún artefacto de contenedorización. Hoy se ejecuta localmente con `./gradlew.bat :bootstrap-app:bootRun` contra un PostgreSQL levantado aparte. Se pide dockerizarlo para poder levantar el entorno completo (app + base de datos) con un solo comando, tanto para desarrollo local como con vista a un uso posterior en CI/producción.

Restricción arquitectónica clave: `bootstrap-app/build.gradle` declara un `sourceSet` extra —
```groovy
sourceSets { main { resources { srcDir rootProject.file('../docs/cadena-farmacias-docs/database') } } }
```
— que incorpora las migraciones Flyway V001-V017 desde `docs/cadena-farmacias-docs/database/migrations/` al classpath de `bootstrap-app`. Esto significa que **el build de Gradle no es autocontenible dentro de `service-botica/`**: necesita ver también `docs/cadena-farmacias-docs/database/`. Por eso el contexto de build de Docker debe ser la raíz del monorepo, no `service-botica/` en solitario (documentado también en `CLAUDE.md`).

## Decisiones (confirmadas con el usuario)

1. **Alcance:** Dockerfile de la app + `docker-compose.yml` que también levanta PostgreSQL — un solo `docker compose up` deja todo el entorno funcionando.
2. **Propósito:** pensado para desarrollo local **y** como base reutilizable para CI/producción más adelante → imagen multi-stage, capas cacheables, usuario no-root, healthcheck.
3. **Contexto de build:** la raíz del repositorio (`erp-botica/`), para que Gradle resuelva el `sourceSet` de `docs/cadena-farmacias-docs/database` sin modificar el build actual.
4. **Secretos:** archivo `.env.example` versionado (plantilla con placeholders e instrucciones) + `.env` real gitignored, leído por `docker-compose.yml` vía `env_file`.

## Diseño

### Estructura de archivos

```
erp-botica/                          (raíz del repo — contexto de build)
├── docker-compose.yml
├── .env.example
├── .dockerignore                    (raíz — filtra frontend/, docs no necesarios, .git, etc. del contexto)
└── service-botica/
    └── Dockerfile
```

`.env` real se agrega al `.gitignore` raíz ya existente (no se versiona).

### Dockerfile (`service-botica/Dockerfile`, build con contexto `.` desde la raíz)

Multi-stage:

**Stage 1 — `build`:**
- Imagen base: `eclipse-temurin:25-jdk` (o `gradle:9.5.1-jdk25` si existe una imagen oficial equivalente al momento de implementar — se decide en el plan según disponibilidad real de tags).
- Copia primero los archivos de configuración de Gradle (`service-botica/gradlew`, `gradle/`, `settings.gradle`, `build.gradle`, `gradle.properties`, `build-logic/`, y los `build.gradle` de cada subproyecto) para maximizar el cacheo de capas de dependencias antes de copiar el código fuente completo.
- Copia el código fuente de `service-botica/` y `docs/cadena-farmacias-docs/database/` (necesario por el `sourceSet`).
- Ejecuta `./gradlew :bootstrap-app:bootJar --no-daemon`.

**Stage 2 — `runtime`:**
- Imagen base: `eclipse-temurin:25-jre` (sin JDK, más liviana).
- Crea un usuario no-root dedicado y cambia a él.
- Copia únicamente `bootstrap-app/build/libs/service-botica.jar` desde el stage `build`.
- `EXPOSE 8080`.
- `HEALTHCHECK` contra `GET /actuator/health` (ya expuesto en `application.yaml`: `management.endpoints.web.exposure.include: health,info,metrics,modulith`).
- `ENTRYPOINT ["java", "-jar", "service-botica.jar"]`.

### `.dockerignore` (raíz)

Excluye del contexto de build lo que no hace falta para compilar `bootstrap-app`: `frontend/`, `graphify-out/`, `.git/`, `docs/` completo **excepto** `docs/cadena-farmacias-docs/database/` (necesario), `service-botica/build/`, `service-botica/*/build/`, `service-botica/.gradle/`, `.idea/`, `*.md` sueltos no necesarios. El detalle exacto de patrones se resuelve en el plan de implementación probando que el build funcione.

### `docker-compose.yml` (raíz)

Dos servicios:

**`postgres`:**
- Imagen `postgres:18` (coincide con la base asumida por la documentación canónica de datos — "PostgreSQL 16+"/18).
- Variables `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` desde `.env`.
- Volumen nombrado para persistir datos entre reinicios.
- `healthcheck` con `pg_isready`.

**`app`:**
- `build: { context: ., dockerfile: service-botica/Dockerfile }`.
- `depends_on: { postgres: { condition: service_healthy } }`.
- `env_file: .env`.
- Puerto `"8080:8080"`.
- Variables de entorno mapeadas a lo que ya consume el backend (`DB_URL` apuntando al hostname del servicio `postgres` dentro de la red de compose, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `BOTICA_JWT_SECRET`, `BOTICA_JWT_ISSUER`, `BOTICA_JWT_AUDIENCE`, TTLs opcionales) — ver `bootstrap-app/src/main/resources/application.yaml` para la lista completa de variables ya soportadas.

### `.env.example`

Plantilla con todas las variables que el backend ya lee (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `BOTICA_JWT_SECRET`, `BOTICA_JWT_ISSUER`, `BOTICA_JWT_AUDIENCE`, `BOTICA_ACCESS_TOKEN_TTL`, `BOTICA_REFRESH_TOKEN_TTL`, `BOTICA_PASSWORD_RESET_TTL`, `BOTICA_MAX_FAILED_ATTEMPTS`, `BOTICA_LOCK_DURATION`), con valores de ejemplo seguros para desarrollo (no productivos) y, específicamente para `BOTICA_JWT_SECRET`, un comentario explicando que debe ser una clave HMAC-256 (≥256 bits) codificada en Base64 — la app falla al arrancar sin uno válido (`SecurityModuleConfiguration`) — junto con el comando para generarlo (p. ej. `openssl rand -base64 32`).

## Fuera de alcance

- No se modifica el `application.yaml` existente ni la lógica del `sourceSet` de Gradle — el Dockerfile se adapta a la estructura actual, no al revés.
- No se agrega hot-reload/dev-mode con volúmenes montados para código fuente (Spring DevTools, bind mounts) — queda para una iteración futura si se pide explícitamente.
- No se dockeriza el frontend en este cambio.
- No se configura un pipeline de CI que use estas imágenes — solo se deja la base reutilizable.

## Verificación

1. `docker compose build` completa sin errores desde la raíz del repo.
2. `docker compose up` levanta `postgres` (healthy) y luego `app`; el log de `app` muestra el arranque de Spring Boot sin errores de Flyway (confirma que el `sourceSet` de migraciones se resolvió correctamente dentro del contenedor).
3. `curl http://localhost:8080/actuator/health` responde `{"status":"UP"}` desde el host.
4. Un login real contra `/api/v1/auth/login` (con un usuario/credencial sembrados manualmente o vía las migraciones) confirma que la app se conecta correctamente a Postgres dentro de la red de compose.
5. `docker compose down && docker compose up` de nuevo confirma persistencia de datos vía el volumen nombrado.
