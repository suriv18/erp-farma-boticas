# Diseño: gate de cobertura 100% para código nuevo (backend + frontend)

## Contexto

`CLAUDE.md` declara ahora que todo archivo fuente **nuevo** (backend `service-botica/`, frontend `frontend/`) debe alcanzar 100% de cobertura de líneas y ramas en sí mismo, exigido como gate automático en el build. No es un umbral retroactivo: el código ya existente no está obligado a llegar a 100% salvo que se reescriba o se cree de nuevo.

Ni JaCoCo ni Vitest distinguen nativamente "archivo nuevo" de "archivo viejo". El mecanismo elegido es un **baseline de exclusión congelado**: hoy se genera la lista exacta de clases/archivos existentes con cobertura <100%, se excluyen explícitamente del gate, y esa lista no vuelve a crecer — cualquier archivo creado desde este punto en adelante cae automáticamente bajo el gate al 100%, porque no puede estar en una lista que ya quedó fija.

## Estado actual verificado (investigación previa)

**Backend:** JaCoCo no está configurado en ningún `build.gradle` del monorepo. Existe el plugin custom `boticas.quality`, aplicado por todos los subproyectos (`plugins { id 'boticas.quality' }`), que es el lugar natural para centralizar la configuración en vez de tocar cada `build.gradle` de módulo. Huecos de cobertura ya detectados por investigación previa (a confirmar con el reporte real antes de congelar el baseline): `StandardApplicationError` (shared-application) y el módulo `organizacion` (9 archivos main, 1 test).

**Frontend:** Vitest 4.1.10 + `@vitest/coverage-v8` 4.1.10 ya están instalados pero sin usar — `vitest.config.ts` (único archivo de config de test en el workspace, con `projects` para `erp-web`/`ui-web`/`api-client`) tiene `coverage.reporter` pero ningún `coverage.thresholds` ni `coverage.include`/`exclude`. El script `test` es `vitest run` sin `--coverage`. Cobertura actual aproximada: erp-web 101 archivos fuente/28 test, ui-web 13/6, api-client 5/1. No hay CI (`.github/workflows/`) en el repo todavía — el gate protege solo localmente por ahora.

## Componentes

### 1. Backend — JaCoCo en `boticas.quality`

- Ubicar el archivo real del plugin `boticas.quality` (precondición del Step 1 de la tarea de implementación: buscarlo bajo `build-logic/`/`buildSrc/`/`gradle/plugins/` según la convención del repo — la investigación previa lo mencionó como plugin custom pero no confirmó su ruta exacta de archivo).
- Aplicar el plugin `jacoco` dentro de `boticas.quality` (no en cada `build.gradle` de módulo).
- Configurar `jacocoTestReport` (si no existe ya) para que corra tras `test` y genere XML (necesario para derivar el baseline y, luego, para que `jacocoTestCoverageVerification` lo consuma).
- Configurar `jacocoTestCoverageVerification` con reglas `element = 'CLASS'`, una regla `counter = 'LINE', minimum = 1.0` y otra `counter = 'BRANCH', minimum = 1.0`, con `excludes` = la lista de FQCN del baseline (ver Step de generación abajo).
- Enganchar `jacocoTestCoverageVerification` a `check` (`check.dependsOn jacocoTestCoverageVerification` o equivalente, si el plugin de Gradle no lo hace ya automáticamente al configurar la regla).
- El baseline de exclusión se genera ejecutando `jacocoTestReport` en todo `service-botica` primero, inspeccionando el XML/HTML resultante para listar toda clase con cobertura de líneas o ramas <100%, y volcando esa lista literal como `excludes` en la configuración — no se asume la lista de la investigación previa sin confirmarla con el reporte real (puede haber cambiado, o esa investigación pudo no cubrir el 100% de los módulos).

### 2. Frontend — Vitest thresholds en `vitest.config.ts`

- Agregar a cada entrada de `test.projects` (o al bloque `coverage` raíz si Vitest 4 permite un `coverage` compartido entre projects — a confirmar en la documentación de Vitest 4 durante la implementación, ya que la forma exacta de aplicar `coverage` por-project vs global cambió entre versiones de Vitest) un bloque `coverage.thresholds` con `lines: 100, branches: 100, functions: 100, statements: 100`.
- Agregar `coverage.exclude` con: el baseline de exclusión (globs de los archivos existentes con cobertura <100% hoy, derivados de correr `vitest run --coverage` antes de configurar el gate) más las exclusiones estructurales ya identificadas por convención (no necesitan estar en el baseline porque nunca deberían medirse): `**/main.tsx`, `**/src/test/mocks/**`, `**/setup-tests.ts`, `**/test-setup.ts`, `**/*.config.ts`.
- Cambiar el script `test` en `frontend/package.json` (usado por `pnpm check`) para invocar `vitest run --coverage`, de modo que el gate se ejecute en cada `pnpm check`.
- El baseline se genera ejecutando `vitest run --coverage` primero, leyendo el reporte de texto/HTML generado, y listando los archivos con cobertura <100% como entradas de `coverage.exclude`.

### 3. Verificación final

- Backend: `cd service-botica && .\gradlew.bat check --warning-mode all` debe pasar en verde con el gate activo.
- Frontend: `pnpm check` debe pasar en verde con el gate activo.
- No se escriben tests nuevos para código existente en este plan — el baseline absorbe todo el código actual con cobertura <100%; escribir esos tests queda fuera de alcance (podría ser un plan futuro aparte, por decisión explícita del usuario).

## Fuera de alcance

- No se configura CI (no existe `.github/workflows/` en el repo; este gate solo protege builds locales por ahora).
- No se escriben tests nuevos para elevar la cobertura de código ya existente — eso es trabajo aparte, no de este plan.
- No se modifica ningún `build.gradle` de módulo individual salvo que sea estrictamente necesario (la configuración vive centralizada en `boticas.quality`).
- No se toca el plan en curso de `TipoDocumentoIdentidad` (pausado aparte) — este gate, una vez activo, sí aplicará automáticamente a los archivos que ese plan cree de aquí en adelante (Task 3 en adelante), lo cual es intencional.
