# DOM-FAR-004 — Estados y Transiciones

**Versión:** 0.1  
**Estado:** Candidatos de workflow; los estados no normativos se etiquetan `DOM/TEC`.

## 1. Principio

Un estado solo debe existir cuando cambia las operaciones permitidas. No se usarán estados para representar hechos derivables, por ejemplo `VENCIDO` cuando basta comparar la fecha de vencimiento, salvo que una integración requiera una proyección específica.

## 2. Establecimiento

```text
CONFIGURACION
   ↓
HABILITADO
   ├── SUSPENDIDO
   └── INACTIVO
```

- `HABILITADO` exige configuración y evidencia de operación conforme a las reglas del negocio.
- `SUSPENDIDO/INACTIVO` bloquean nuevas operaciones según política.

## 3. Producto Regulado

```text
BORRADOR
   ↓
ACTIVO
   ├── SUSPENDIDO
   ├── BLOQUEADO
   └── RETIRADO
```

El `estado regulatorio externo` debe conservarse separado del `estado interno comercial` cuando sean conceptos diferentes.

No se debe convertir automáticamente todo cambio externo en el mismo enum interno sin ACL/mapeo.

## 4. Orden de Compra

```text
BORRADOR
   ↓
PENDIENTE_APROBACION
   ├── RECHAZADA
   ↓
APROBADA
   ↓
EMITIDA
   ├── PARCIALMENTE_RECIBIDA
   └── COMPLETAMENTE_RECIBIDA

BORRADOR/PENDIENTE/APROBADA/EMITIDA
   └── CANCELADA (según reglas)
```

Una recepción parcial no modifica cantidades históricas recibidas al modificar la OC posteriormente.

## 5. Recepción de Compra

```text
BORRADOR
  ↓
EN_VERIFICACION
  ├── RECHAZADA
  ├── ACEPTADA_PARCIAL
  └── ACEPTADA
```

Diferencias quedan registradas; no se “arreglan” sobrescribiendo lo recibido.

## 6. Lote

Estado sanitario/operativo candidato:

```text
HABILITADO
   ├── CUARENTENA
   ├── BLOQUEADO
   ├── INMOVILIZADO_RECALL
   └── DISPOSICION_FINAL
```

Condiciones derivadas paralelas:

```text
vigente / próximo a vencer / vencido
```

La venta exige simultáneamente:

- condición temporal válida;
- estado sanitario vendible;
- producto regulatoriamente comercializable;
- posición con cantidad disponible.

## 7. Transferencia

```text
BORRADOR
  ↓
SOLICITADA
  ↓
APROBADA
  ↓
RESERVADA
  ↓
DESPACHADA
  ↓
EN_TRANSITO
  ├── RECIBIDA_PARCIAL
  └── RECIBIDA
```

Cancelación después de despacho no “desaparece” la mercadería; requiere retorno/regularización.

## 8. Lista de Precio / Promoción

### Lista de precio

```text
BORRADOR → PUBLICADA → VENCIDA
              └── SUSPENDIDA
```

### Promoción

```text
BORRADOR → PROGRAMADA → ACTIVA → FINALIZADA
                           └── SUSPENDIDA
```

La activación real también depende del rango temporal; no debe confiar solo en el enum.

## 9. Turno de Caja

```text
ABIERTO
  ↓
EN_ARQUEO
  ↓
CERRADO
  ↓
CONCILIADO
```

Una venta presencial no debe registrarse en un turno cerrado.

## 10. Venta

```text
BORRADOR
  ↓
STOCK_RESERVADO
  ↓
PENDIENTE_PAGO
  ↓
CONFIRMADA
  ├── DEVOLUCION_PARCIAL (condición/proyección)
  └── DEVOLUCION_TOTAL (condición/proyección)

Estados previos pueden terminar en CANCELADA.
```

No agregar `CPE_ACEPTADO` al enum de `Venta`; el estado fiscal pertenece a `BC-FIS`.

## 11. Prescripción

Estados candidatos:

```text
REGISTRADA
  ↓
EN_VALIDACION
  ├── RECHAZADA
  └── VALIDADA
       └── ANULADA si existe causa permitida
```

`EXPIRADA` debería ser una decisión/condición calculada por política de validez, no un estado universal manual.

La posibilidad de dispensación parcial/múltiple queda `POR_VALIDAR`; no se introduce todavía un estado `PARCIALMENTE_CONSUMIDA` como regla general.

## 12. Dispensación

```text
BORRADOR
  ↓
EN_EVALUACION
  ├── DENEGADA
  └── AUTORIZADA
       ↓
    CONFIRMADA
```

`CONFIRMADA` significa que el acto quedó registrado; la venta/pago se administra en otro contexto.

## 13. Receta Controlada

```text
REGISTRADA
  ↓
EN_VALIDACION_ESPECIAL
  ├── RECHAZADA
  └── VALIDADA
       ↓
    ATENDIDA
       ↓
    RETENIDA/ARCHIVADA según regla aplicable
```

Los plazos/retención dependen de la lista y norma; no son un workflow universal para toda receta. [REF-26]

## 14. Comprobante Electrónico

Estado interno normalizado candidato:

```text
PENDIENTE_GENERACION
  ↓
GENERADO
  ↓
PENDIENTE_ENVIO
  ↓
ENVIADO
  ├── ACEPTADO
  ├── RECHAZADO
  └── OBSERVADO/PENDIENTE según contrato externo
```

Los estados específicos del PSE/SUNAT se traducen mediante ACL. SUNAT distingue documentos y respuestas fiscales; no se copiarán códigos de proveedor a todo el dominio. [REF-27][REF-28]

## 15. Devolución Comercial

```text
SOLICITADA
  ↓
VALIDADA
  ├── RECHAZADA
  └── ACEPTADA
       ↓
    REEMBOLSO_PENDIENTE
       ↓
    REEMBOLSADA
       ↓
    CERRADA
```

El estado sanitario del producto físico se resuelve aparte en inventario.

## 16. Caso Recall

```text
ABIERTO
  ↓
ALCANCE_IDENTIFICADO
  ↓
BLOQUEO_EN_PROCESO
  ↓
INMOVILIZACION_EN_PROCESO
  ↓
CONCILIACION
  ↓
CERRADO
```

Puede existir `CANCELADO` únicamente si se documenta la razón/autoridad que deja sin efecto el caso.

## 17. Reporte de Farmacovigilancia

```text
BORRADOR
  ↓
LISTO_PARA_ENVIO
  ↓
ENVIADO
  ↓
ACUSE_REGISTRADO
  ↓
CERRADO
```

Complementaciones no borran la versión anterior.

## 18. Posting ERP

```text
PENDIENTE
  ↓
PROCESANDO
  ├── POSTEADO
  ├── ERROR_REINTENTABLE
  └── ERROR_DEFINITIVO

POSTEADO → REVERSADO mediante operación explícita
```

`ERROR_REINTENTABLE` y `ERROR_DEFINITIVO` son categorías técnicas/de integración, no conceptos contables normativos.

## 19. Regla de concurrencia

Todo estado que protege una transición económica o sanitaria debe validar versión/concurrencia. Ejemplos:

- dos ventas intentando consumir la última unidad;
- dos cierres del mismo turno;
- dos publicaciones simultáneas de precio para el mismo scope/período;
- doble atención de una receta cuando la norma/política no lo permite;
- doble posting con la misma clave idempotente.
