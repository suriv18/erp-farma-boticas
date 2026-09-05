# STD-FAR-007 — Pruebas de Arquitectura y Code Review

## 1. Arquitectura como código verificable

No basta documentar dependencias permitidas. Se deben automatizar reglas cuando el stack lo permita.

Si se adopta Java, ArchUnit puede verificar dependencias, ciclos y arquitectura Onion/Hexagonal mediante pruebas automatizadas. [REF-62]

Reglas candidatas:

```text
domain !-> infrastructure
application !-> web framework internals innecesarios
module A !-> persistence internals of module B
no cycles between business modules
controllers -> application only
```

## 2. Code Review checklist mínimo

### Dominio

- ¿usa lenguaje ubicuo?
- ¿preserva invariantes?
- ¿evita regla crítica en controller/UI?
- ¿cambió una regla normativa sin fuente?

### Arquitectura

- ¿respeta Ports & Adapters?
- ¿introduce dependencia entre módulos?
- ¿requiere ADR?
- ¿crea acoplamiento con proveedor externo?

### Datos

- ¿preserva tenant/store/ownership?
- ¿afecta lote/stock histórico?
- ¿requiere migración?
- ¿expone dato sensible?

### API

- ¿respeta HTTP/Problem Details?
- ¿idempotencia?
- ¿breaking change?
- ¿OpenAPI actualizado?

### Seguridad

- ¿autorización backend?
- ¿secreto/log sensible?
- ¿inyección/XSS/CSRF/SSRF según superficie?

### Operación

- ¿telemetría suficiente?
- ¿retry seguro?
- ¿Store Edge compatible?
- ¿runbook requerido?

## 3. Complejidad

No se fija un número universal de líneas por método/clase. Las métricas son señales para revisión, no sustitutos de diseño.
