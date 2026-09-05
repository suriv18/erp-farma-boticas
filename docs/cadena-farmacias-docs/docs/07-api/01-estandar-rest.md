# API-FAR-001 — Estándar REST

## 1. Alcance

Aplica a APIs del Core Central, Store Edge cuando exponga HTTP y adaptadores internos que utilicen REST.

## 2. Recursos y métodos

Se utilizarán recursos y semántica HTTP conforme a RFC 9110. [REF-43]

Ejemplos preferidos:

```http
POST   /api/v1/ventas
GET    /api/v1/ventas/{ventaId}
POST   /api/v1/ventas/{ventaId}/pagos
POST   /api/v1/prescripciones/{id}/validaciones
POST   /api/v1/dispensaciones
POST   /api/v1/transferencias/{id}/despachos
POST   /api/v1/transferencias/{id}/recepciones
```

No forzar CRUD cuando el lenguaje de dominio expresa un acto:

```http
POST /api/v1/lotes/{id}/bloqueos
POST /api/v1/casos-recall/{id}/inmovilizaciones
```

## 3. Status codes base

- `200` consulta/operación con representación.
- `201` recurso creado.
- `202` procesamiento asíncrono aceptado.
- `204` operación exitosa sin body.
- `400` solicitud sintáctica/semántica inválida de entrada.
- `401` no autenticado.
- `403` autenticado sin autorización/competencia.
- `404` recurso no visible/no encontrado según política.
- `409` conflicto de estado/concurrencia/idempotencia.
- `412` precondición fallida cuando se use ETag/version.
- `422` solo si el equipo decide distinguir validación semántica y el contrato lo documenta.
- `429` rate/resource limiting.

## 4. Identificadores

La API no expondrá PK internas secuenciales. El baseline PostgreSQL 18 utiliza `uuid_publico UUID DEFAULT uuidv7()` como identificador público de entidades que se exponen fuera de su agregado/contexto; las PK `BIGINT IDENTITY` permanecen internas.

## 5. Paginación y filtros

Convención candidata:

```text
?page=0&size=50&sort=fecha,desc
```

Para movimientos de alto volumen se evaluará cursor pagination. No se mezclará paginación offset y cursor en el mismo endpoint sin contrato claro.

## 6. Versionado

Base candidata: `/api/v1`. Cambios aditivos compatibles no crean una nueva versión mayor. Cambios incompatibles requieren política formal de deprecación.

## 7. Richardson

Objetivo base: **Nivel 2** (recursos + métodos/status HTTP). HATEOAS/Level 3 se utilizará selectivamente y no como requisito global.
