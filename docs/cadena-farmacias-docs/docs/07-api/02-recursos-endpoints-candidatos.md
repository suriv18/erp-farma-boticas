# API-FAR-002 — Recursos y Endpoints Candidatos

> Esta lista valida cobertura arquitectónica; no es todavía el archivo OpenAPI.

## Organización

```http
GET/POST /api/v1/establecimientos
GET/PATCH /api/v1/establecimientos/{id}
GET/POST /api/v1/establecimientos/{id}/asignaciones-profesionales
```

## Catálogo

```http
GET /api/v1/productos-regulados
GET /api/v1/productos-regulados/{id}
GET/POST /api/v1/skus
GET /api/v1/skus/{id}/vendibilidad?establecimientoId=...
```

## Compras

```http
POST /api/v1/solicitudes-compra
POST /api/v1/solicitudes-compra/{id}/aprobaciones
POST /api/v1/ordenes-compra
POST /api/v1/recepciones-compra
```

## Inventario

```http
GET /api/v1/inventario/posiciones
GET /api/v1/lotes/{id}
POST /api/v1/lotes/{id}/bloqueos
POST /api/v1/reservas-inventario
POST /api/v1/transferencias
POST /api/v1/transferencias/{id}/despachos
POST /api/v1/transferencias/{id}/recepciones
```

## POS

```http
POST /api/v1/turnos-caja
POST /api/v1/turnos-caja/{id}/cierres
POST /api/v1/ventas
GET  /api/v1/ventas/{id}
POST /api/v1/ventas/{id}/pagos
POST /api/v1/devoluciones
```

## Dispensación

```http
POST /api/v1/prescripciones
POST /api/v1/prescripciones/{id}/validaciones
POST /api/v1/dispensaciones
GET  /api/v1/dispensaciones/{id}
```

## Fiscal

```http
POST /api/v1/comprobantes
GET  /api/v1/comprobantes/{id}
POST /api/v1/comprobantes/{id}/reintentos
POST /api/v1/notas-credito
```

## Recall / FVG

```http
POST /api/v1/casos-recall
POST /api/v1/casos-recall/{id}/afectaciones
POST /api/v1/reportes-seguridad
```

## Regla

La API de comando no debe permitir modificar directamente estados internos como `estado=CONFIRMADA`; debe exponer operaciones del lenguaje de negocio.

## Surtido diverso — contratos candidatos

~~~http
GET/POST /api/v1/rubros-comerciales
GET/POST /api/v1/skus/{id}/evidencias-sanitarias
GET /api/v1/skus/{id}/vendibilidad?establecimientoId=...&canal=...
~~~

Usar UUID públicos y contexto autorizado. La consulta de vendibilidad debe devolver decisión, motivo y versión aplicable; el backend la revalida al confirmar. Evidencias sanitarias y política por local requieren implementación adicional. Ver [trazabilidad](../03-requerimientos/09-surtido-multiproducto.md).


El contrato del surtido queda definido en [API-FAR-008](08-contrato-surtido.md); prevalece para campos, permisos, concurrencia, respuestas y errores.
