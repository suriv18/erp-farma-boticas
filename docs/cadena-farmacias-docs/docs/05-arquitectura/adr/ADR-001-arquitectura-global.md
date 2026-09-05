# ADR-001 — Arquitectura global Core Central + Store Edge

**Estado:** Aceptado.

## Contexto
La cadena necesita un Core corporativo y, cuando la continuidad de tienda lo justifique, operación local tolerante a pérdida de conectividad.

## Decisión
El Core Central se implementará como monolito modular multi-módulo. Store Edge será un deployable separado y opcional por establecimiento. No se adopta microservicios por Bounded Context.

## Consecuencias
- dominio modular y despliegue central simple;
- Store Edge solo donde exista requisito operativo;
- sincronización explícita tienda-central;
- no se asume que el motor local sea PostgreSQL hasta ADR específico.
