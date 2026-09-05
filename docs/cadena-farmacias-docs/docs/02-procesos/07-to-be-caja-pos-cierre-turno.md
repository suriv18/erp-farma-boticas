# BPM-FAR-007 — TO-BE Caja, POS y Cierre de Turno

## 1. Objetivo

Definir cómo una venta se convierte en una transacción de caja conciliable y posteriormente en información financiera para el ERP.

## 2. Apertura

```text
Trabajador autenticado
       ↓
Asignar caja / terminal / turno
       ↓
Registrar fondo inicial cuando aplique
       ↓
Abrir turno
       ↓
POS habilitado
```

La política de fondo inicial, arqueo ciego, doble conteo o aprobación de diferencias queda como **POR_VALIDAR** con la cadena.

## 3. Operación durante el turno

El POS puede producir:

- ventas;
- pagos por efectivo, tarjeta u otros medios configurados;
- anulaciones conforme a autorización;
- devoluciones;
- notas de crédito asociadas cuando corresponda;
- ingresos/retiros de efectivo autorizados;
- aperturas de gaveta auditables;
- operaciones offline si finalmente se aprueba esa capacidad.

## 4. Emisión tributaria

Para consumidores finales, el sistema debe soportar Boleta de Venta Electrónica; para operaciones que correspondan, Factura Electrónica. SUNAT permite emisión desde sistemas del contribuyente y contempla notas electrónicas vinculadas.

La capa POS no debe considerar una venta `FISCALMENTE_CONFIRMADA` solo porque el pago fue exitoso; debe existir un estado separado de emisión/aceptación tributaria.

```text
Pago aprobado
    ↓
Venta cerrada comercialmente
    ↓
Generar CPE
    ↓
Enviar a SEE/OSE/PSE según decisión tecnológica
    ↓
Respuesta tributaria
    ↓
ACEPTADO / OBSERVADO / RECHAZADO / PENDIENTE
```

La estrategia exacta de integración SUNAT será un ADR posterior.

## 5. Cierre de turno

```text
Solicitar cierre
     ↓
Bloquear nuevas ventas del turno
     ↓
Calcular ventas por medio de pago
     ↓
Registrar conteo físico / declaraciones
     ↓
Comparar esperado vs. contado
     ↓
Registrar diferencia
     ↓
Justificar / aprobar según umbral
     ↓
Cerrar turno
     ↓
Generar resumen conciliable
     ↓
ERP / conciliación financiera
```

Los sistemas de retail empresariales actuales manejan conciliación por tienda/caja/medio de pago y posterior posting financiero; esto se toma como patrón de mercado, no como norma sanitaria.

## 6. Invariantes candidatas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-POS-001 | Una caja no puede operar sin turno abierto y usuario autorizado. | DOM |
| RC-POS-002 | Toda venta debe identificar establecimiento, terminal, turno y operador. | DOM/AUD |
| RC-POS-003 | El cierre conserva esperado, contado y diferencia; no sobreescribe la diferencia. | DOM/AUD |
| RC-POS-004 | Una transacción rechazada tributariamente no se marca como CPE aceptado. | NORM/DOM |
| RC-POS-005 | Cambios de precio manuales y anulaciones requieren permisos y auditoría. | SEC/DOM |
| RC-POS-006 | La venta offline no se habilita hasta aceptar una política de sincronización y límites. | POR_VALIDAR |

## 7. Fuentes

- SUNAT — SEE desde Sistemas del Contribuyente: https://cpe.sunat.gob.pe/sistema_emision/see_contribuyente
- SUNAT — Boleta de Venta Electrónica: https://cpe.sunat.gob.pe/tipos_de_comprobantes/boleta
- Microsoft Dynamics 365 Commerce — Store Commerce capabilities: https://learn.microsoft.com/en-us/dynamics365/commerce/dev-itpro/store-commerce-capabilities
- Microsoft Dynamics 365 Commerce — Store statements: https://learn.microsoft.com/en-us/dynamics365/commerce/tasks/create-calculate-post-statement-retail-store
