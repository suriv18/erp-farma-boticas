# DOM-FAR-010 — Decisiones de dominio: estado y pendientes

**Versión:** 0.1  
**Estado:** No resolver por intuición.

## 1. Operación offline POS

### Pregunta

¿Cada tienda debe poder vender cuando pierde conectividad con el centro?

### Impacto

- autorización de stock;
- riesgo de sobreventa;
- sincronización de lotes;
- CPE diferido;
- precios/promociones cacheadas;
- recetas/dispensación;
- seguridad local;
- reconciliación posterior.

### Estado

`POR_VALIDAR` con volumen, conectividad, tolerancia de negocio y SUNAT.

## 2. Modelo físico del inventario

Alternativas:

1. ledger append-only + saldo materializado;
2. saldo transaccional + movimientos;
3. event sourcing parcial;
4. combinación por local/central.

El baseline central adoptó saldo transaccional, movimientos y version_lock. Las pruebas de concurrencia/performance y la evolución para artículos sin lote continúan pendientes.

## 3. Granularidad de lote

Debe confirmarse por categoría:

- producto farmacéutico;
- dispositivo con serie/lote;
- producto sanitario;
- artículo retail no regulado.

No todo SKU necesariamente se controla de la misma manera.

## 4. Dispensación parcial/múltiple de receta

No se implementará una regla general hasta validar:

- tipo de receta;
- producto;
- normativa;
- prácticas de la cadena.

## 5. Catálogo DIGEMID: integración automática

Opciones:

- consulta manual/verificación;
- importación periódica;
- API si existe y es formalmente utilizable;
- proveedor de datos;
- carga maestra controlada.

No se hará scraping como dependencia crítica sin autorización/contrato.

## 6. SUNAT

Definir:

- SEE del Contribuyente;
- facturador;
- PSE/OSE si aplica;
- contingencia;
- certificados;
- retención de XML/CDR;
- SLA/reintentos.

Debe cerrarse por ADR de integración fiscal.

## 7. ERP financiero

Aún falta decidir si:

- construiremos contabilidad/tesorería completa;
- integraremos un ERP externo;
- o aplicaremos una estrategia híbrida.

No diseñar plan de cuentas ni asientos exhaustivos antes de esa decisión.

## 8. Precios y promociones

Pendientes:

- prioridad entre promociones;
- stacking;
- cupones;
- precios por cliente/canal;
- restricciones por categoría regulatoria;
- redondeos;
- impuestos por tipo de artículo.

## 9. Política FEFO

Debe definirse por categoría/almacén/canal. No debe codificarse como algoritmo único global.

## 10. Devoluciones

Definir por categoría:

- qué devoluciones comerciales acepta la cadena;
- condiciones;
- tiempo;
- producto abierto/cerrado;
- cadena de frío;
- productos controlados;
- disposición sanitaria.

La política comercial no debe contradecir obligaciones sanitarias.

## 11. Productos prohibidos/servicios prohibidos

El anexo de la RM 734-2025/MINSA fue revisado el 2026-09-15. Las restricciones y excepciones relevantes se documentan en [DAT-FAR-009](../06-datos/09-adecuacion-surtido-multiproducto-peru.md). Falta traducirlas a decisiones versionadas por SKU y establecimiento; no son una autorización genérica de artículos retail. [REF-42, REF-64]

## 12. Privacidad y fidelización

Antes de CRM:

- finalidades;
- consentimiento/base jurídica;
- segmentación;
- historial sensible de medicamentos;
- marketing de productos con receta;
- retención de datos.

## 13. Identidad de paciente/cliente

No se asumirá que todo comprador debe identificarse ni que `Cliente` y `Paciente` son la misma entidad.

```text
Comprador/Cliente Retail
      ≠
Paciente de Prescripción/FVG
```

Pueden coincidir, pero tienen finalidades distintas.

## 14. Arquitectura

La arquitectura base está recogida en ADR-001 a ADR-010. La siguiente lista conserva los temas del análisis; su estado vigente se consulta en esos ADR:

- monolito modular vs componentes de tienda;
- multi-módulo DDD;
- Clean/Hexagonal;
- CQRS selectivo;
- mensajería/outbox;
- caché;
- sincronización tienda-central;
- arquitectura de alta disponibilidad.

## 15. Cierre de diseño de surtido — 2026-09-15

Las decisiones de granularidad de lote/serie y admisibilidad para este alcance están resueltas en [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md). La revisión sanitaria de artículos concretos es una tarea operativa con evidencia; no se sustituye por una decisión global de software.
