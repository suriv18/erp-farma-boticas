# ARC-FAR-003 — Estrategia de Solución

## 1. Decisión principal

La arquitectura global combina:

> **Core Central en Monolito Modular multi-módulo + DDD + Clean Architecture/Ports & Adapters + CQRS selectivo + Result Pattern + Domain Events**, complementado por un **Store Edge opcional por tienda** cuando sea necesaria continuidad offline.

La razón de no elegir microservicios para los 16 Bounded Contexts es evitar complejidad distribuida prematura: transacciones distribuidas, versionado de contratos internos, operación de brokers, observabilidad distribuida y consistencia eventual innecesaria.

## 2. Modularidad

Cada módulo debe:

- tener API pública explícita;
- ocultar su persistencia interna;
- no acceder directamente a tablas/repositorios de otro módulo;
- comunicarse sincrónicamente por interfaces públicas cuando la consistencia inmediata sea necesaria;
- usar eventos internos cuando el efecto sea desacoplable;
- poder probarse de forma aislada.

Si el stack elegido es Java/Spring, Spring Modulith es una herramienta candidata porque su documentación vigente soporta módulos orientados al dominio, verificación de dependencias, pruebas de módulos y documentación de relaciones. Su adopción tecnológica queda para ADR de stack.

## 3. Arquitectura interna por módulo

```text
infrastructure/input
        ↓
application
        ↓
domain
        ↑
application ports
        ↑
infrastructure/output
```

### Domain

- Aggregates.
- Entities.
- Value Objects.
- Domain Services/Policies.
- Domain Events.
- Repository interfaces cuando pertenecen al dominio.

### Application

- Commands.
- Queries.
- Handlers / Use Cases.
- Transaction boundaries.
- Authorization orchestration.
- Result Pattern.

### Infrastructure

- REST/controllers.
- persistence.
- adapters SUNAT/DIGEMID/ERP.
- messaging.
- store sync.
- object storage.

## 4. CQRS selectivo

No significa separar bases de datos inicialmente.

### Command side

Utiliza agregados/invariantes para modificar estado.

### Query side

Puede usar proyecciones/SQL optimizado sin reconstruir agregados cuando sea innecesario.

No se adopta Event Sourcing global.

## 5. Result Pattern

Errores esperables:

- `LOTE_NO_VENDIBLE`;
- `STOCK_INSUFICIENTE`;
- `RECETA_REQUERIDA`;
- `RECETA_CONTROLADA_VENCIDA`;
- `USUARIO_SIN_COMPETENCIA_PROFESIONAL`;
- `CPE_YA_EMITIDO`;
- `POSTING_YA_PROCESADO`.

Se representan como resultados del caso de uso, no como excepciones técnicas genéricas.

Excepciones se reservan para fallos inesperados/técnicos.

## 6. Domain Events

Ejemplos:

- `VentaConfirmada`;
- `StockConsumido`;
- `DispensacionConfirmada`;
- `CasoRecallAbierto`;
- `ComprobanteAceptado`;
- `PostingRetailConfirmado`.

Dentro del mismo proceso pueden ser manejados internamente. Para cruzar límites de despliegue se utiliza Outbox.

## 7. Arquitectura distribuida mínima

Los únicos límites de despliegue que se justifican inicialmente son:

1. Core Central.
2. Store Edge/POS cuando se habilite offline.
3. Integraciones externas.

No se distribuye el Core por Bounded Context hasta que exista evidencia de escala, autonomía organizativa o aislamiento operacional.
