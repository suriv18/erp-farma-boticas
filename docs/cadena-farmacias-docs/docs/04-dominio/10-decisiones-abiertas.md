# DOM-FAR-010 — Decisiones Abiertas antes de Arquitectura/Datos

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

No se decide hasta estudiar concurrencia y performance.

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

La RM 734-2025/MINSA aprobó una relación de productos y servicios prohibidos en farmacias/boticas. Debe analizarse su anexo antes de cerrar el catálogo de artículos no farmacéuticos que la plataforma permitirá comercializar. [REF-42]

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

Después de validar estas preguntas se evaluará:

- monolito modular vs componentes de tienda;
- multi-módulo DDD;
- Clean/Hexagonal;
- CQRS selectivo;
- mensajería/outbox;
- caché;
- sincronización tienda-central;
- arquitectura de alta disponibilidad.
