# STD-FAR-005 — CQRS Selectivo, Result y Domain Events

## 1. Commands

Un Command expresa intención de cambio:

```text
ConfirmarVentaCommand
BloquearLoteCommand
RegistrarDispensacionCommand
EmitirNotaCreditoCommand
```

Reglas:

- nombre imperativo;
- datos mínimos;
- sin lógica;
- idempotency key cuando la operación lo exija;
- handler transaccional claramente delimitado.

## 2. Queries

Las Queries no modifican estado de negocio.

Pueden leer proyecciones optimizadas sin reconstruir un agregado cuando no sea necesario.

## 3. Result Pattern

Errores esperables usan códigos estables:

```text
STOCK_INSUFICIENTE
LOTE_NO_VENDIBLE
RECETA_REQUERIDA
USUARIO_SIN_COMPETENCIA
CPE_YA_PROCESADO
POSTING_DUPLICADO
```

La API mapea esos errores a Problem Details según [07/API](../07-api/03-problem-details.md).

## 4. Domain Events

Un evento representa un hecho ya ocurrido:

```text
VentaConfirmada
StockConsumido
DispensacionConfirmada
CasoRecallAbierto
```

Reglas:

- nombre en pasado;
- inmutable;
- contiene identificadores/contexto necesario, no objetos enteros arbitrarios;
- no garantiza por sí mismo mensajería externa.

## 5. Eventos internos vs integración

```text
Domain Event interno
      ↓
Handler local
```

Si cruza un límite de despliegue:

```text
Domain Event
      ↓
Integration Event
      ↓
Outbox
      ↓
transporte
```

No todo Domain Event debe salir del módulo.
