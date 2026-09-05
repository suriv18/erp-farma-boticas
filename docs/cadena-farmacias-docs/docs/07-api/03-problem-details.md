# API-FAR-003 — Manejo de Errores con Problem Details

RFC 9457 define `application/problem+json` para errores HTTP estructurados. [REF-44]

## Contrato base

```json
{
  "type": "https://api.example.com/problems/stock-insuficiente",
  "title": "Stock insuficiente",
  "status": 409,
  "detail": "No existe cantidad vendible suficiente para confirmar la venta.",
  "instance": "/api/v1/ventas/01...",
  "code": "INV_STOCK_INSUFICIENTE",
  "correlationId": "01...",
  "errors": []
}
```

## Catálogo inicial de códigos

- `CAT_PRODUCTO_NO_VENDIBLE`
- `INV_STOCK_INSUFICIENTE`
- `INV_LOTE_BLOQUEADO`
- `INV_LOTE_VENCIDO`
- `DSP_RECETA_REQUERIDA`
- `DSP_ACTOR_NO_COMPETENTE`
- `CTL_RECETA_ESPECIAL_INVALIDA`
- `POS_TURNO_NO_ABIERTO`
- `POS_IDEMPOTENCY_CONFLICT`
- `FIS_CPE_RECHAZADO`
- `RCL_LOTE_INMOVILIZADO`
- `AUTHZ_SCOPE_DENIED`

## Regla

Los mensajes técnicos de base de datos, stack traces, tokens, secretos y payloads sensibles no se exponen al consumidor.
