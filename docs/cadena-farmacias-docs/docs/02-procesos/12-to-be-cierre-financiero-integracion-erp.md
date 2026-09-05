# BPM-FAR-012 — TO-BE Cierre Financiero e Integración Retail → ERP

## 1. Objetivo

Definir la frontera entre transacciones operativas de tienda y registros consolidados/contabilizados en ERP.

## 2. Principio

El POS es la fuente operativa de la transacción retail, pero el ERP es responsable de la consolidación financiera y contable.

```text
POS / Tiendas / E-commerce
        ↓
Transacciones validadas
        ↓
Conciliación por tienda / turno / medio pago
        ↓
Statement / resumen financiero
        ↓
ERP
        ↓
Contabilidad / Tesorería / CxC / impuestos / bancos
```

## 3. Validaciones previas al posting

Antes de contabilizar un bloque se deberá validar, según configuración:

- totales de cabecera vs. líneas;
- impuestos;
- medios de pago;
- devoluciones/notas;
- existencia de dimensiones contables requeridas;
- estado tributario del CPE cuando aplique;
- integridad de stock/costo para operaciones que generen movimiento;
- duplicidad/idempotencia del lote de integración.

Dynamics 365 Commerce documenta un patrón similar: las transacciones de tienda son validadas antes de incorporarse a statements y posting financiero. Se utiliza como benchmark técnico.

## 4. Cierre de tienda vs. cierre contable

No deben confundirse:

```text
CIERRE TURNO/CAJA
    → responsabilidad operativa retail

CIERRE DIARIO TIENDA
    → conciliación y consolidación

POSTING ERP
    → registro financiero

CIERRE CONTABLE MENSUAL
    → proceso de Finanzas/Contabilidad
```

Una tienda puede estar cerrada operativamente aunque existan transacciones pendientes de posting por error de integración; dicho error debe ser visible y reintentable sin duplicar asientos.

## 5. Flujo diario propuesto

```text
Cerrar turnos
   ↓
Validar transacciones
   ↓
Resolver excepciones
   ↓
Conciliar medios de pago
   ↓
Generar resumen por tienda/fecha
   ↓
Publicar a ERP
   ↓
ERP acepta / rechaza
   ↓
Registrar identificador de posting
   ↓
Conciliar bancos/adquirentes posteriormente
```

## 6. Reglas candidatas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-FIN-001 | Una transacción se contabiliza una sola vez por clave idempotente. | DOM/TEC |
| RC-FIN-002 | El cierre de caja no borra transacciones pendientes de integración. | DOM |
| RC-FIN-003 | Una corrección posterior genera ajuste/reversa trazable, no edición destructiva del histórico contabilizado. | ERP/AUD |
| RC-FIN-004 | Diferencias de caja se registran y clasifican; no se compensan silenciosamente. | DOM/AUD |
| RC-FIN-005 | El estado de posting ERP es independiente del estado de venta POS. | DOM |

## 7. Decisiones abiertas

- ERP propio vs. integración con ERP externo.
- nivel de detalle a contabilizar: transacción individual vs. resumen diario;
- tratamiento de costos: promedio, FIFO u otra política contable/inventario;
- integración síncrona vs. asíncrona/outbox;
- conciliación con adquirentes/tarjetas;
- arquitectura offline de tiendas.

Estas decisiones deberán convertirse en ADR una vez se conozca el tamaño/operación real de la cadena.

## 8. Fuentes

- Microsoft Dynamics 365 Commerce — Statements: https://learn.microsoft.com/en-us/dynamics365/commerce/tasks/create-calculate-post-statement-retail-store
- Microsoft Dynamics 365 Commerce — Validación de transacciones: https://learn.microsoft.com/en-us/dynamics365/commerce/valid-checker
- Microsoft Dynamics 365 Commerce — Posting parameters: https://learn.microsoft.com/en-us/dynamics365/commerce/dev-itpro/commerce-posting-parameters
- SAP Retail — Financial Transactions (Store): https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/9905622a5c1f49ba84e9076fc83a9c2c/bbcbc353b677b44ce10000000a174cb4.html
