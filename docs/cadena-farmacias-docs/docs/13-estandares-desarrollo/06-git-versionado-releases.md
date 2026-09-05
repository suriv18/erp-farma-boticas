# STD-FAR-006 — Git, Versionado y Releases

## 1. Git

Todo cambio productivo debe ser trazable a commit/revisión.

La estrategia concreta de branching queda pendiente del tamaño del equipo, pero se exige:

- ramas principales protegidas;
- pull request para cambios relevantes;
- prohibición de secretos;
- history legible;
- tags/releases trazables.

## 2. Commits

Los mensajes deben describir intención. Conventional Commits puede adoptarse si el equipo obtiene beneficio, pero no se declara obligatorio en esta línea base.

## 3. Semantic Versioning

SemVer 2.0.0 se usa como referencia para APIs/artefactos donde existe contrato público y compatibilidad significativa. [REF-63]

```text
MAJOR → incompatibilidad
MINOR → funcionalidad compatible
PATCH → corrección compatible
```

## 4. Contrato Store Edge

Además de la versión del software debe existir versionado del protocolo/schema de sincronización. No se debe inferir compatibilidad únicamente desde la versión de la aplicación.

## 5. Breaking changes

Deben incluir:

- impacto;
- migración;
- ventana de compatibilidad;
- consumidores afectados;
- plan de rollout;
- rollback/forward-fix.
