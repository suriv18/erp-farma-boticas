# Gate de cobertura 100% (código nuevo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Activar un gate de cobertura de tests al 100% (líneas y ramas) que aplica automáticamente a toda clase/archivo **nuevo** creado desde ahora en adelante, en backend (`service-botica/`, JaCoCo) y frontend (`frontend/`, Vitest), sin exigir retroactivamente 100% al código ya existente.

**Architecture:** Mecanismo de baseline de exclusión congelado. Se genera hoy, a partir de un reporte de cobertura real, la lista exacta de clases/archivos existentes con cobertura <100%; esa lista se excluye explícitamente del gate y nunca vuelve a crecer. Backend: `jacocoTestCoverageVerification` (ya se aplica el plugin `jacoco`, falta la regla de verificación) leyendo un archivo `jacoco-baseline.txt` por módulo. Frontend: `coverage.thresholds` + `coverage.exclude` en `vitest.config.ts`.

**Tech Stack:** Gradle 9.5.1 + JaCoCo 0.8.14 (ya aplicado vía `boticas.quality.gradle`, plugin `jacoco` + `jacocoTestReport` ya generando XML/HTML y enganchado a `check`). Vitest 4.1.10 + `@vitest/coverage-v8` 4.1.10 (ya instalados, sin usar).

## Global Constraints

- No se escriben tests nuevos para código existente en este plan — el baseline absorbe todo el código actual con cobertura <100% tal como está hoy. Escribir esos tests es trabajo aparte, fuera de alcance.
- El baseline ya fue generado y verificado antes de escribir este plan, corriendo `cd service-botica && ./gradlew.bat check --warning-mode all` (BUILD SUCCESSFUL) y parseando los 7 `jacocoTestReport.xml` resultantes (`bootstrap-app`, `modules/catalogo`, `modules/organizacion`, `modules/security`, `shared-kernel`, `shared-web`, `testing/architecture-tests`). Los 15 módulos restantes (`shared-application`, `shared-persistence`, `modules/farmacia`, `modules/inventario`, `modules/compras`, `modules/ventas`, `modules/clientes`, `modules/finanzas`, `modules/rrhh`, `modules/crm`, `modules/cmr`, `modules/app-channel`, `modules/logistica`, `modules/pagos`, `modules/notificaciones`, `modules/integraciones`, `testing/test-fixtures`) no tienen ningún test (`src/test` vacío o inexistente) y por tanto JaCoCo no genera `jacocoTestReport.xml` para ellos (`jacocoTestReport SKIPPED`) — no necesitan baseline propio; si en el futuro ganan tests parciales, su primer `jacocoTestReport` real definirá si necesitan uno.
- Los 6 archivos `jacoco-baseline.txt` (uno por módulo con reporte no vacío) ya están generados en disco, en la raíz de cada módulo (`service-botica/bootstrap-app/jacoco-baseline.txt`, `service-botica/modules/catalogo/jacoco-baseline.txt`, `service-botica/modules/organizacion/jacoco-baseline.txt`, `service-botica/modules/security/jacoco-baseline.txt`, `service-botica/shared-kernel/jacoco-baseline.txt`, `service-botica/shared-web/jacoco-baseline.txt`), un FQCN por línea, orden alfabético, `\n` como terminador (sin BOM). `testing/architecture-tests` no tiene archivo de baseline porque su reporte ya está al 100% (0 clases con huecos) — no debe crearse uno vacío.
- No se modifica ningún `build.gradle` de módulo individual — toda la configuración de JaCoCo vive centralizada en `service-botica/build-logic/src/main/groovy/boticas.quality.gradle`, aplicado por todos los subproyectos vía `plugins { id 'boticas.quality' }`.
- No hay CI (`.github/workflows/`) en el repo — este gate protege solo builds locales por ahora; no se crea configuración de CI en este plan.
- Verificación final obligatoria: `cd service-botica && ./gradlew.bat check --warning-mode all` debe terminar en BUILD SUCCESSFUL con el gate activo, y `cd frontend && pnpm check` debe terminar en verde con el gate activo.

---

### Task 1: Backend — activar `jacocoTestCoverageVerification` con baseline por módulo

**Files:**
- Modify: `service-botica/build-logic/src/main/groovy/boticas.quality.gradle`
- Already created (verify present, do not regenerate): `service-botica/bootstrap-app/jacoco-baseline.txt`, `service-botica/modules/catalogo/jacoco-baseline.txt`, `service-botica/modules/organizacion/jacoco-baseline.txt`, `service-botica/modules/security/jacoco-baseline.txt`, `service-botica/shared-kernel/jacoco-baseline.txt`, `service-botica/shared-web/jacoco-baseline.txt`

**Interfaces:**
- Consumes: el plugin `jacoco` de Gradle (ya aplicado en este mismo archivo), la task `jacocoTestReport` (ya configurada, genera `${project.buildDir}/reports/jacoco/test/jacocoTestReport.xml`).
- Produces: la task `jacocoTestCoverageVerification`, con dos reglas (`element = 'CLASS'`, `counter = 'LINE'`/`'BRANCH'`, `minimum = 1.0`), `excludes` poblado dinámicamente leyendo `${project.projectDir}/jacoco-baseline.txt` si existe. Enganchada a `check`. Usada por Task 3 (verificación final).

- [ ] **Step 1: Verificar que los 6 archivos de baseline existen y tienen el conteo esperado**

Ejecutar desde la raíz del repo:

```bash
wc -l service-botica/bootstrap-app/jacoco-baseline.txt service-botica/modules/catalogo/jacoco-baseline.txt service-botica/modules/organizacion/jacoco-baseline.txt service-botica/modules/security/jacoco-baseline.txt service-botica/shared-kernel/jacoco-baseline.txt service-botica/shared-web/jacoco-baseline.txt
```

Esperado (conteos exactos, generados y verificados antes de escribir este plan a partir de un build real):
```
    1 service-botica/bootstrap-app/jacoco-baseline.txt
  157 service-botica/modules/catalogo/jacoco-baseline.txt
    1 service-botica/modules/organizacion/jacoco-baseline.txt
   85 service-botica/modules/security/jacoco-baseline.txt
    4 service-botica/shared-kernel/jacoco-baseline.txt
    2 service-botica/shared-web/jacoco-baseline.txt
  250 total
```

Si algún archivo falta o el conteo difiere sustancialmente, DETENERSE y reportar `NEEDS_CONTEXT` — no regenerar el baseline sin más indicación, porque una regeneración en este punto podría estar corriendo contra un estado de código distinto al que existía cuando se diseñó este plan (por ejemplo, si Task 3+ del plan de `TipoDocumentoIdentidad` ya avanzó en paralelo y agregó código nuevo real que no debería entrar al baseline).

- [ ] **Step 2: Modificar `boticas.quality.gradle` para leer el baseline y activar la regla de verificación**

Leer primero el archivo completo `service-botica/build-logic/src/main/groovy/boticas.quality.gradle` (son solo 21 líneas) para confirmar que sigue teniendo exactamente este contenido antes de editarlo:

```groovy
plugins {
    id 'boticas.test'
    id 'jacoco'
}

jacoco {
    toolVersion = '0.8.14'
}

tasks.named('jacocoTestReport') {
    dependsOn tasks.named('test')
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named('check') {
    dependsOn tasks.named('jacocoTestReport')
}
```

Si el contenido es distinto de lo anterior, DETENERSE y reportar `NEEDS_CONTEXT` con el contenido real encontrado, en vez de asumir y sobreescribir.

Reemplazar el contenido completo del archivo por:

```groovy
plugins {
    id 'boticas.test'
    id 'jacoco'
}

jacoco {
    toolVersion = '0.8.14'
}

tasks.named('jacocoTestReport') {
    dependsOn tasks.named('test')
    reports {
        xml.required = true
        html.required = true
    }
}

def baselineFile = file("${project.projectDir}/jacoco-baseline.txt")
def coverageBaseline = baselineFile.exists()
        ? baselineFile.readLines().collect { it.trim() }.findAll { !it.isEmpty() }
        : []

tasks.register('jacocoTestCoverageVerification', JacocoCoverageVerification) {
    dependsOn tasks.named('jacocoTestReport')
    executionData.setFrom(tasks.named('jacocoTestReport').get().executionData)
    sourceDirectories.setFrom(tasks.named('jacocoTestReport').get().sourceDirectories)
    classDirectories.setFrom(tasks.named('jacocoTestReport').get().classDirectories)
    violationRules {
        rule {
            element = 'CLASS'
            excludes = coverageBaseline
            limit {
                counter = 'LINE'
                minimum = 1.0
            }
        }
        rule {
            element = 'CLASS'
            excludes = coverageBaseline
            limit {
                counter = 'BRANCH'
                minimum = 1.0
            }
        }
    }
}

tasks.named('check') {
    dependsOn tasks.named('jacocoTestReport')
    dependsOn tasks.named('jacocoTestCoverageVerification')
}
```

Nota sobre `JacocoCoverageVerification`: el plugin `jacoco` de Gradle registra por defecto una task llamada `jacocoTestCoverageVerification` de tipo `JacocoCoverageVerification` en cuanto el plugin `jacoco` se aplica junto con el plugin `java` (que `boticas.java-library`, base de `boticas.test`, ya aplica) — por lo tanto en la práctica la task YA EXISTE con ese nombre por convención del plugin. Verificar esto en el Step 3 (si `tasks.register('jacocoTestCoverageVerification', ...)` falla con `A task with name already exists`, usar `tasks.named('jacocoTestCoverageVerification', JacocoCoverageVerification) { ... }` con el mismo cuerpo de configuración en vez de `tasks.register`, y ajustar el archivo de forma acorde).

- [ ] **Step 3: Verificar que un módulo con baseline compila y su gate pasa**

Ejecutar desde `service-botica/`:

```
.\gradlew.bat :modules:security:check --warning-mode all
```

Esperado: BUILD SUCCESSFUL. Si falla con `A task with name 'jacocoTestCoverageVerification' already exists`, aplicar el ajuste descrito en el Step 2 (usar `tasks.named` en vez de `tasks.register`) y volver a ejecutar este mismo comando.

Si falla con violaciones de cobertura (mensaje `Rule violated for class X: lines/branches covered ratio is Y, but expected minimum is 1.00`), significa que esa clase `X` tiene cobertura <100% y NO está en `jacoco-baseline.txt` — comparar contra el archivo `.superpowers/sdd/jacoco-baseline-raw.txt` (lista completa de las 250 clases detectadas al generar el baseline) para confirmar si `X` debió incluirse y se omitió por error de generación, o si es código nuevo genuino creado después del baseline (en cuyo caso el gate está funcionando correctamente y hay que escribirle tests, no agregarlo al baseline).

- [ ] **Step 4: Ejecutar el build completo**

Ejecutar desde `service-botica/`:

```
.\gradlew.bat check --warning-mode all
```

Esperado: BUILD SUCCESSFUL en los 22 subproyectos.

- [ ] **Step 5: Commit**

```bash
git add service-botica/build-logic/src/main/groovy/boticas.quality.gradle service-botica/bootstrap-app/jacoco-baseline.txt service-botica/modules/catalogo/jacoco-baseline.txt service-botica/modules/organizacion/jacoco-baseline.txt service-botica/modules/security/jacoco-baseline.txt service-botica/shared-kernel/jacoco-baseline.txt service-botica/shared-web/jacoco-baseline.txt
git commit -m "feat(build): activar gate de cobertura 100% para clases nuevas via JaCoCo"
```

---

### Task 2: Frontend — activar `coverage.thresholds` con baseline por archivo

**Files:**
- Modify: `frontend/vitest.config.ts`
- Modify: `frontend/package.json` (script `test`)
- Create: `frontend/coverage-baseline.txt` (generado en Step 1, no escrito a mano)

**Interfaces:**
- Consumes: `@vitest/coverage-v8` (ya instalado, provider `v8`).
- Produces: `coverage.thresholds` (100% lines/branches/functions/statements) y `coverage.exclude` en `vitest.config.ts`, aplicados a los 3 `projects` (erp-web, ui-web, api-client). Script `pnpm test` invoca `vitest run --coverage`.

- [ ] **Step 1: Generar el reporte de cobertura real y derivar el baseline**

Ejecutar desde `frontend/`:

```bash
pnpm test -- --coverage
```

Si el flag `--coverage` no es reconocido por el script `test` actual (`vitest run`, sin passthrough de flags configurado), ejecutar directamente:

```bash
pnpm exec vitest run --coverage
```

Esperado: el comando corre todos los tests existentes (28 en erp-web, 6 en ui-web, 1 en api-client) y termina generando un reporte de cobertura en texto y HTML (`coverage.reporter: ['text', 'html']` ya configurado en `vitest.config.ts`) bajo `frontend/coverage/` (ubicación default de Vitest si no se sobreescribe `coverage.reportsDirectory`).

Del output de texto (`text` reporter imprime una tabla por archivo con % de líneas/branches/funcs/statements en la consola), o del `coverage/index.html`/`coverage/coverage-final.json` generado, extraer la lista de rutas de archivo (relativas a `frontend/`, ej. `apps/erp-web/src/features/seguridad/api/useRoles.ts`) que NO llegan a 100% en alguna de las 4 métricas. Si `coverage/coverage-final.json` existe, es la fuente más fiable y parseable (JSON con una entrada por archivo y sus contadores `s`/`b`/`f`/statementMap`/etc.) — preferirlo sobre parsear el texto de consola.

Escribir esa lista, una ruta relativa por línea (mismo formato de glob que usará `coverage.exclude`, ej. `apps/erp-web/src/features/seguridad/api/useRoles.ts`), en `frontend/coverage-baseline.txt`.

- [ ] **Step 2: Leer el `vitest.config.ts` actual completo**

Leer `frontend/vitest.config.ts` completo antes de editarlo, para confirmar su estructura exacta de `projects` (la investigación previa reportó este contenido aproximado, pero debe confirmarse literal antes de editar):

```ts
test: {
  coverage: {
    reporter: ['text', 'html']
  },
  projects: [
    { test: { name: 'erp-web', environment: 'jsdom', ..., include: ['apps/erp-web/**/*.test.{ts,tsx}'] } },
    { test: { name: 'ui-web', environment: 'jsdom', ..., include: ['packages/ui-web/**/*.test.{ts,tsx}'] } },
    { test: { name: 'api-client', environment: 'jsdom', include: ['packages/api-client/**/*.test.{ts,tsx}'] } }
  ]
}
```

- [ ] **Step 3: Ampliar el bloque `coverage` con thresholds y exclude**

En Vitest 4, cuando se usa `test.projects`, el bloque `coverage` a nivel raíz de `test` (no dentro de cada project) sigue siendo el que controla el reporte y los thresholds globales del run completo — esto se confirma en este mismo Step ejecutando el comando del Step 4 y observando si los thresholds se aplican; si Vitest 4 exige `coverage` dentro de cada entrada de `projects` en cambio (comportamiento distinto detectado en este Step), mover el bloque `coverage` completo dentro de cada objeto de `test.projects[].test` en lugar de dejarlo a nivel raíz, replicando el mismo `thresholds`/`exclude` en las 3 entradas.

Modificar el bloque `coverage` raíz de `frontend/vitest.config.ts` (manteniendo `projects` sin cambios) para que quede:

```ts
coverage: {
  reporter: ['text', 'html'],
  thresholds: {
    lines: 100,
    branches: 100,
    functions: 100,
    statements: 100,
  },
  exclude: [
    '**/main.tsx',
    '**/src/test/mocks/**',
    '**/setup-tests.ts',
    '**/test-setup.ts',
    '**/*.config.ts',
    // Baseline congelado: archivos existentes con cobertura <100% al momento de activar
    // el gate. Esta lista no crece — todo archivo nuevo cae bajo el gate al 100%.
    ...require('node:fs').readFileSync('./coverage-baseline.txt', 'utf-8')
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean),
  ],
}
```

Si el archivo usa `import`/ESM puro sin `require` disponible (a confirmar leyendo el resto de `vitest.config.ts` — si ya usa `import fs from 'node:fs'` en algún punto del archivo, o si es un módulo `.ts` con `"type": "module"` en `package.json`), reemplazar la línea `...require('node:fs').readFileSync(...)` por una lectura vía `import { readFileSync } from 'node:fs'` al inicio del archivo y `...readFileSync('./coverage-baseline.txt', 'utf-8').split('\n').map((line) => line.trim()).filter(Boolean)` en el `exclude`.

- [ ] **Step 4: Ejecutar coverage y verificar que el gate pasa con el baseline actual**

```bash
pnpm exec vitest run --coverage
```

Esperado: exit code 0, sin errores de "Coverage threshold not met" — el baseline generado en Step 1 debe cubrir exactamente los archivos que hoy están por debajo de 100%. Si aparece algún error de threshold para un archivo no listado en `coverage-baseline.txt`, agregarlo al baseline (es un archivo existente que Step 1 no capturó correctamente, no código nuevo) y re-ejecutar este mismo comando.

- [ ] **Step 5: Actualizar el script `test` en `frontend/package.json`**

Leer `frontend/package.json` primero para confirmar el script `test` actual (`"vitest run"` según investigación previa) y el script `check` (`"pnpm lint && pnpm typecheck && pnpm test && pnpm build"`).

Modificar el script `test` para que incluya `--coverage`:

```json
"test": "vitest run --coverage",
```

- [ ] **Step 6: Ejecutar `pnpm check` completo**

```bash
pnpm check
```

Esperado: exit code 0 (lint, typecheck, test con coverage, build, todos en verde).

- [ ] **Step 7: Commit**

```bash
git add frontend/vitest.config.ts frontend/package.json frontend/coverage-baseline.txt
git commit -m "feat(frontend): activar gate de cobertura 100% para archivos nuevos via Vitest"
```

---

### Task 3: Verificación final combinada

**Files:** ninguno (solo verificación).

**Interfaces:** N/A.

- [ ] **Step 1: Backend completo**

```
cd service-botica && .\gradlew.bat check --warning-mode all
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Step 2: Frontend completo**

```
cd frontend && pnpm check
```

Esperado: exit code 0.

- [ ] **Step 3: Confirmar que el gate realmente bloquea código nuevo sin cobertura (prueba de humo, no se commitea)**

Crear temporalmente un archivo trivial nuevo con una rama sin cubrir, por ejemplo `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ZZZSmokeTestGateClass.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

final class ZZZSmokeTestGateClass {
    private ZZZSmokeTestGateClass() {
    }

    static String pick(boolean flag) {
        if (flag) {
            return "a";
        }
        return "b";
    }
}
```

Ejecutar `cd service-botica && .\gradlew.bat :modules:catalogo:check --warning-mode all` — esperado: FALLA con violación de cobertura para `ZZZSmokeTestGateClass` (0% líneas/ramas, no está en el baseline). Esto confirma que el gate protege código nuevo real.

Eliminar el archivo inmediatamente después de confirmar la falla (`rm service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ZZZSmokeTestGateClass.java`) — es solo una prueba de humo, no debe quedar en el repo ni commitearse.

- [ ] **Step 4: Re-confirmar build limpio tras eliminar el archivo de prueba**

```
cd service-botica && .\gradlew.bat :modules:catalogo:check --warning-mode all
```

Esperado: BUILD SUCCESSFUL (de vuelta a verde tras eliminar el archivo de humo).

---

## Self-Review

**Spec coverage:**
- Backend: JaCoCo centralizado en `boticas.quality`, gate por CLASS/LINE/BRANCH al 100%, baseline derivado de reporte real → Task 1. ✓
- Frontend: Vitest thresholds al 100%, `coverage.exclude` con baseline + exclusiones estructurales, script `test` con `--coverage` → Task 2. ✓
- Verificación de que el gate realmente aplica a código nuevo (no solo que no rompe lo existente) → Task 3 Step 3 (prueba de humo explícita, con limpieza). ✓
- Baseline generado a partir de datos reales, no asumido → ya generado y documentado en Global Constraints con conteos exactos verificables. ✓
- Fuera de alcance del spec respetado: no se escriben tests para código viejo, no se toca CI (no existe), no se modifica cada `build.gradle` de módulo individual. ✓

**Placeholder scan:** sin "TBD"/"TODO". Las dos incertidumbres técnicas señaladas en el spec (ruta del plugin `boticas.quality`, sintaxis exacta de `coverage` con `projects` en Vitest 4) fueron resueltas: la primera se confirmó por lectura directa (`service-botica/build-logic/src/main/groovy/boticas.quality.gradle`, contenido citado literalmente en Task 1 Step 2); la segunda queda con una verificación explícita y accionable en Task 2 Step 3 (correr el comando y observar si el threshold aplica, con la alternativa exacta si no aplica) en vez de asumida.

**Type consistency:** el nombre del archivo de baseline es `jacoco-baseline.txt` en backend (consistente entre Global Constraints, Task 1 Step 1 y Step 2) y `coverage-baseline.txt` en frontend (consistente entre Task 2 Step 1, Step 3 y Step 7) — nombres distintos entre stacks es intencional, no inconsistencia, dado que son mecanismos y formatos de archivo distintos (FQCN Java vs. rutas de archivo TS).

**Scope check:** 3 tareas técnicas (backend, frontend, verificación combinada), cada una con entregable verificable de forma independiente. No requiere descomposición adicional. El plan de `TipoDocumentoIdentidad` permanece pausado y no se toca aquí; una vez este plan termine, ese otro se retoma y automáticamente queda sujeto al gate recién activado para cualquier archivo que cree desde ese punto en adelante.
