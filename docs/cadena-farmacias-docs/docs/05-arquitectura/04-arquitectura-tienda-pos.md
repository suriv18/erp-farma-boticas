# ARC-FAR-004 — Arquitectura de Tienda y POS

## 1. Evidencia de mercado

La arquitectura offline no es una invención del proyecto:

- Dynamics 365 Commerce documenta una base offline y cambio automático cuando el servicio central no está disponible, con sincronización posterior.
- LS Central documenta dos opciones: base offline por POS y **Offline POS Server Database**, donde varias cajas se conectan a una base/servicio local de tienda. LS Retail señala que el esquema local mejora resiliencia y reduce dependencia de WAN.

Estas fuentes sustentan el patrón; no convierten el modo offline en obligación normativa.

## 2. Perfil recomendado

Se acepta una arquitectura **online-first, offline-capable**.

### Perfil ONLINE

```text
POS → Core Central
```

Ventajas:

- datos en tiempo real;
- menor infraestructura local;
- menor complejidad operacional.

### Perfil STORE_EDGE

```text
POS 1 ─┐
POS 2 ─┼─→ Store Edge → Store DB
POS n ─┘        │
                └──── Sync ──── Core Central
```

Se recomienda **una base/servicio por tienda**, no una base aislada por caja, cuando se requiera operación offline en múltiples cajas. Esto reduce conflictos locales y mantenimiento duplicado. Es una decisión `TEC` inspirada en patrones actuales de retail.

## 3. Qué datos puede necesitar la tienda

Proyección mínima candidata:

- establecimiento y cajas;
- SKUs activos vendibles en esa tienda;
- atributos regulatorios necesarios para decidir vendibilidad;
- lotes y stock local;
- precios/promociones publicadas con versión/vigencia;
- parámetros de impuestos/fiscalidad requeridos por el POS;
- operadores/roles offline mínimos `POR_DEFINIR`;
- configuración de hardware;
- estados de recall/bloqueos relevantes.

No se replica indiscriminadamente todo el ERP ni todo el catálogo clínico/regulatorio.

## 4. Clasificación de operaciones offline

Cada caso de uso de tienda debe clasificarse:

- `OFFLINE_ALLOWED`;
- `OFFLINE_DEGRADED`;
- `REQUIRES_ONLINE`.

Ejemplos iniciales **no normativos**:

| Operación | Política inicial |
|---|---|
| Venta simple con efectivo | candidato `OFFLINE_ALLOWED` |
| Venta con tarjeta | depende del adquirente/conectividad |
| Consulta stock de otra tienda | `REQUIRES_ONLINE` |
| Fidelización en tiempo real | `OFFLINE_DEGRADED` o online |
| Producto controlado | conservador: `REQUIRES_ONLINE` hasta validar proceso |
| Cambio regulatorio no sincronizado | bloquear si la copia local está expirada |
| Recall confirmado localmente | bloquear siempre |

La matriz definitiva debe aprobarse con negocio, QF, fiscal y seguridad.

## 5. Política de datos obsoletos

Todo dataset local debe poseer:

- `version`;
- `generated_at`;
- `effective_from`;
- `expires_at` cuando aplique;
- origen;
- checksum/hash cuando aporte valor.

Si un dato crítico supera su antigüedad máxima permitida, la operación debe degradarse o bloquearse según política.

## 6. Stock offline

No se debe asumir que el stock central es instantáneamente exacto durante desconexión.

El Store Edge registra localmente consumos/reservas y los sincroniza al volver la conectividad. Para limitar riesgo de sobreventa omnicanal se evaluará:

- `offline_sellable_qty`;
- safety stock;
- restricciones por categoría;
- deshabilitar reserva externa sobre ciertas cantidades durante desconexión;
- reconciliation posterior.

El algoritmo final permanece `POR_VALIDAR` hasta conocer volumen, omnicanalidad y tolerancia al riesgo.

## 7. Seguridad local

La operación offline exige controles adicionales:

- cifrado de almacenamiento local cuando corresponda;
- device identity;
- rotación de credenciales/certificados;
- mínimo dato personal local;
- auditoría local encadenable/sincronizable;
- límites de sesión offline;
- capacidad de revocar dispositivos al reconectar.

La estrategia concreta de IAM se resolverá en la fase de seguridad.
