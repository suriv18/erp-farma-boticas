# ERP Botica

Monorepo de una plataforma integral para cadena de farmacias en Perú.

## Fuente única de verdad

El punto de entrada canónico es
[`docs/cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md`](docs/cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md).
Allí se define la precedencia entre requisitos, ADR, código existente y artefactos legados.

## Estado actual

- `service-botica/`: monolito modular Java/Spring; Organización contiene el primer agregado y caso
  de uso probados, pero aún no hay endpoints, adapters de persistencia, entidades JPA ni migraciones SQL.
- `frontend/`: scaffold React operativo; login simulado, dashboard con mock/datos fijos, inventario
  con fixtures, Organización contra contrato mock y siete módulos placeholder.
- `docs/cadena-farmacias-docs/`: documentación canónica de negocio a entrega.
- `docs/arquitectura`, `docs/sprint`, `docs/decisiones-adr` y `docs/db`: antecedentes o borradores
  legados conservados para trazabilidad.

Una pantalla visible, un módulo detectado o una tabla en el DDL legado no equivalen a una capacidad
terminada. La entrega sigue el flujo `RF/RN → CU/CA → dominio → ADR → contrato/modelo → slice → pruebas`.

## Verificación local

Backend:

```powershell
cd service-botica
.\gradlew.bat check
```

Frontend:

```powershell
cd frontend
corepack pnpm check
corepack pnpm format:check
```

El consolidado documental se regenera desde sus fuentes con:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./docs/cadena-farmacias-docs/scripts/regenerar-documentacion.ps1
```
