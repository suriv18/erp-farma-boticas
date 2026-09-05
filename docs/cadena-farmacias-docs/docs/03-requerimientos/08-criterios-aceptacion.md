# CA-FAR — Criterios de Aceptación

**Versión:** 0.2  
**Estado:** Borrador verificable para QA  
**Fecha de corte:** 2026-08-30

## 1. Convenciones

- Formato `Dado / Cuando / Entonces`.
- IDs: `CA-<RF>-NN-TIPO`.
- Tipos usados: `POS` positivo, `VAL` validación, `SEC/COMP/PRIV` seguridad/competencia/privacidad, `AUD` auditoría, `HIS` histórico, `IDE` idempotencia, `CON` concurrencia, `SEP` separación de conceptos, `NORM/FISCAL` regla normativa/fiscal.
- Los criterios `NORM/FISCAL` solo son válidos dentro del alcance documentado por su `RN-*`; no universalizan plazos o mecanismos.

## 2. Cobertura

Esta versión cubre **206 RF** vinculados a los 22 casos de uso prioritarios.

## 3. Criterios por RF

### RF-AUD-001 — Auditar operaciones críticas

#### CA-RF-AUD-001-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Auditar operaciones críticas"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-001-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-001-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-002 — Auditar cambios de maestros regulatorios

#### CA-RF-AUD-002-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Auditar cambios de maestros regulatorios"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-002-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-002-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-003 — Auditar acceso a datos sensibles

#### CA-RF-AUD-003-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Auditar acceso a datos sensibles"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-003-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-003-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-004 — Auditar ajustes de stock

#### CA-RF-AUD-004-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Auditar ajustes de stock"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-004-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-004-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-005 — Auditar descuentos/devoluciones

#### CA-RF-AUD-005-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Auditar descuentos/devoluciones"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-005-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-005-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-006 — Auditar roles/permisos

#### CA-RF-AUD-006-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Auditar roles/permisos"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-006-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-006-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-007 — Correlacionar operaciones distribuidas

#### CA-RF-AUD-007-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Correlacionar operaciones distribuidas"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-007-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-007-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-AUD-008 — Conservar auditoría sin edición destructiva

#### CA-RF-AUD-008-01-POS

**Dado** un evento crítico dentro del ámbito autorizado  
**Cuando** se ejecuta "Conservar auditoría sin edición destructiva"  
**Entonces** la auditoría conserva actor, local/ámbito, recurso, fecha, resultado y correlación suficiente

#### CA-RF-AUD-008-02-IMM

**Dado** un evento de auditoría registrado  
**Cuando** un usuario ordinario intenta modificarlo o borrarlo  
**Entonces** la operación es rechazada o está fuera de los casos de uso de negocio

#### CA-RF-AUD-008-03-SEC

**Dado** datos sensibles o credenciales asociados al evento  
**Cuando** se persiste/consulta la auditoría  
**Entonces** no se almacenan ni exponen secretos, tokens o payloads sensibles completos


### RF-CAT-001 — Registrar producto maestro

#### CA-RF-CAT-001-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Registrar producto maestro"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-001-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Registrar producto maestro"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-001-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Registrar producto maestro"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-CAT-002 — Registrar SKU/presentación comercial

#### CA-RF-CAT-002-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Registrar SKU/presentación comercial"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-002-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Registrar SKU/presentación comercial"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-002-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Registrar SKU/presentación comercial"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-CAT-007 — Registrar registro sanitario

#### CA-RF-CAT-007-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Registrar registro sanitario"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-007-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Registrar registro sanitario"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-007-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Registrar registro sanitario"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-CAT-008 — Registrar condición de venta

#### CA-RF-CAT-008-01-POS

**Dado** un producto con condición de venta sustentada  
**Cuando** se publica una nueva versión  
**Entonces** los canales aplican esa condición desde su vigencia y la versión queda identificable

#### CA-RF-CAT-008-02-FLOW

**Dado** un producto cuya condición exige receta  
**Cuando** se agrega a una venta  
**Entonces** se activa/exige el flujo de dispensación antes del cierre de la línea/venta según el proceso

#### CA-RF-CAT-008-03-HIS

**Dado** ventas cerradas con una condición anterior  
**Cuando** cambia la condición de venta  
**Entonces** las ventas históricas no se reinterpretan retroactivamente


### RF-CAT-009 — Clasificar producto fiscalizado

#### CA-RF-CAT-009-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Clasificar producto fiscalizado"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-009-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Clasificar producto fiscalizado"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-009-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Clasificar producto fiscalizado"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-CAT-013 — Versionar atributos regulatorios

#### CA-RF-CAT-013-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Versionar atributos regulatorios"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-013-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Versionar atributos regulatorios"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-013-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Versionar atributos regulatorios"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-CAT-015 — Detectar duplicados de producto/SKU

#### CA-RF-CAT-015-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Detectar duplicados de producto/SKU"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-015-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Detectar duplicados de producto/SKU"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-015-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Detectar duplicados de producto/SKU"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-CAT-018 — Auditar cambios del maestro

#### CA-RF-CAT-018-01-POS

**Dado** un producto/SKU con datos válidos y fuente identificada  
**Cuando** se ejecuta "Auditar cambios del maestro"  
**Entonces** la información queda registrada en una versión efectiva y consultable por sus identificadores

#### CA-RF-CAT-018-02-HIS

**Dado** que el atributo regulatorio ya fue utilizado en una transacción cerrada  
**Cuando** se publica una modificación mediante "Auditar cambios del maestro"  
**Entonces** la transacción histórica conserva la versión originalmente aplicada

#### CA-RF-CAT-018-03-VAL

**Dado** un duplicado o una combinación regulatoria incompatible  
**Cuando** se intenta "Auditar cambios del maestro"  
**Entonces** el sistema rechaza la publicación y no crea una identidad regulatoria paralela inconsistente


### RF-COM-001 — Registrar proveedor

#### CA-RF-COM-001-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Registrar proveedor"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-001-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Registrar proveedor"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-001-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Registrar proveedor"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-002 — Mantener habilitación del proveedor

#### CA-RF-COM-002-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Mantener habilitación del proveedor"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-002-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Mantener habilitación del proveedor"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-002-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Mantener habilitación del proveedor"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-003 — Crear solicitud de compra

#### CA-RF-COM-003-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Crear solicitud de compra"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-003-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Crear solicitud de compra"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-003-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Crear solicitud de compra"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-004 — Aprobar solicitud de compra

#### CA-RF-COM-004-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Aprobar solicitud de compra"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-004-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Aprobar solicitud de compra"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-004-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Aprobar solicitud de compra"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-005 — Generar orden de compra

#### CA-RF-COM-005-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Generar orden de compra"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-005-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Generar orden de compra"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-005-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Generar orden de compra"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-006 — Versionar/modificar OC antes de recepción

#### CA-RF-COM-006-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Versionar/modificar OC antes de recepción"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-006-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Versionar/modificar OC antes de recepción"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-006-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Versionar/modificar OC antes de recepción"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-007 — Registrar condiciones comerciales

#### CA-RF-COM-007-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Registrar condiciones comerciales"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-007-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Registrar condiciones comerciales"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-007-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Registrar condiciones comerciales"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-008 — Registrar despacho/documento proveedor

#### CA-RF-COM-008-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Registrar despacho/documento proveedor"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-008-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Registrar despacho/documento proveedor"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-008-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Registrar despacho/documento proveedor"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-009 — Registrar recepción contra OC

#### CA-RF-COM-009-01-POS

**Dado** una OC con saldo pendiente y un despacho recibido  
**Cuando** se registra la recepción  
**Entonces** se crean cantidades recibidas y movimientos por SKU/lote sin exceder reglas de tolerancia no autorizadas

#### CA-RF-COM-009-02-PART

**Dado** una entrega parcial  
**Cuando** se confirma la recepción  
**Entonces** la OC conserva saldo pendiente y la recepción queda cerrada por sus cantidades reales

#### CA-RF-COM-009-03-INV

**Dado** una recepción observada  
**Cuando** se ingresa físicamente  
**Entonces** el stock puede quedar en cuarentena/no vendible hasta la conformidad aplicable


### RF-COM-010 — Registrar diferencias de recepción

#### CA-RF-COM-010-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Registrar diferencias de recepción"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-010-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Registrar diferencias de recepción"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-010-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Registrar diferencias de recepción"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-011 — Registrar factura de proveedor

#### CA-RF-COM-011-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Registrar factura de proveedor"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-011-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Registrar factura de proveedor"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-011-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Registrar factura de proveedor"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-012 — Conciliar OC-recepción-factura

#### CA-RF-COM-012-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Conciliar OC-recepción-factura"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-012-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Conciliar OC-recepción-factura"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-012-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Conciliar OC-recepción-factura"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-013 — Gestionar excepción de matching

#### CA-RF-COM-013-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Gestionar excepción de matching"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-013-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Gestionar excepción de matching"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-013-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Gestionar excepción de matching"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-014 — Generar cuenta por pagar

#### CA-RF-COM-014-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Generar cuenta por pagar"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-014-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Generar cuenta por pagar"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-014-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Generar cuenta por pagar"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-COM-016 — Auditar ciclo Procure-to-Pay

#### CA-RF-COM-016-01-POS

**Dado** un ciclo de compra con proveedor, documentos y productos válidos  
**Cuando** se ejecuta "Auditar ciclo Procure-to-Pay"  
**Entonces** el hecho de compra queda relacionado con solicitud/OC/recepción/factura según corresponda y conserva trazabilidad

#### CA-RF-COM-016-02-SEP

**Dado** que existen eventos físicos y financieros distintos  
**Cuando** se procesa "Auditar ciclo Procure-to-Pay"  
**Entonces** una factura no crea stock y una recepción no crea por sí sola una obligación financiera distinta a la definida por el proceso

#### CA-RF-COM-016-03-AUD

**Dado** una aprobación, diferencia o excepción de compra  
**Cuando** se confirma "Auditar ciclo Procure-to-Pay"  
**Entonces** motivo, actor y referencias del ciclo Procure-to-Pay quedan auditables


### RF-CTL-001 — Clasificar producto por lista fiscalizada

#### CA-RF-CTL-001-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Clasificar producto por lista fiscalizada"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-001-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-001-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Clasificar producto por lista fiscalizada"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-002 — Determinar tipo de receta exigible

#### CA-RF-CTL-002-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Determinar tipo de receta exigible"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-002-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-002-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Determinar tipo de receta exigible"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-003 — Validar receta especial

#### CA-RF-CTL-003-01-3D

**Dado** una receta especial de una lista a la que aplica el plazo de tres días  
**Cuando** han transcurrido más de tres días desde su expedición  
**Entonces** no se permite atenderla

#### CA-RF-CTL-003-02-TAMPER

**Dado** una receta con enmendaduras o sospecha/evidencia de adulteración/falsificación  
**Cuando** se valida  
**Entonces** se bloquea la atención y se habilita el flujo de incidencia aplicable

#### CA-RF-CTL-003-03-TRACE

**Dado** una receta especial válida que se atiende  
**Cuando** se cierra la dispensación  
**Entonces** quedan trazados adquirente, cantidad, QF y retención/archivo cuando corresponda


### RF-CTL-004 — Bloquear receta especial vencida

#### CA-RF-CTL-004-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Bloquear receta especial vencida"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-004-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-004-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Bloquear receta especial vencida"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-005 — Bloquear receta adulterada/sospechosa

#### CA-RF-CTL-005-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Bloquear receta adulterada/sospechosa"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-005-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-005-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Bloquear receta adulterada/sospechosa"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-006 — Retener receta atendida

#### CA-RF-CTL-006-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Retener receta atendida"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-006-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-006-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Retener receta atendida"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-007 — Registrar adquirente y cantidad dispensada

#### CA-RF-CTL-007-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Registrar adquirente y cantidad dispensada"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-007-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-007-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Registrar adquirente y cantidad dispensada"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-008 — Registrar dispensación parcial controlada

#### CA-RF-CTL-008-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Registrar dispensación parcial controlada"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-008-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-008-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Registrar dispensación parcial controlada"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-010 — Gestionar almacenamiento restringido

#### CA-RF-CTL-010-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Gestionar almacenamiento restringido"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-010-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-010-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Gestionar almacenamiento restringido"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-011 — Registrar libro/registro de existencias

#### CA-RF-CTL-011-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Registrar libro/registro de existencias"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-011-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-011-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Registrar libro/registro de existencias"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-012 — Conciliar saldo físico vs registro

#### CA-RF-CTL-012-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Conciliar saldo físico vs registro"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-012-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-012-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Conciliar saldo físico vs registro"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-013 — Registrar robo/sustracción/siniestro

#### CA-RF-CTL-013-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Registrar robo/sustracción/siniestro"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-013-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-013-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Registrar robo/sustracción/siniestro"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-014 — Preparar balance trimestral

#### CA-RF-CTL-014-01-SCOPE

**Dado** un establecimiento y listas/sujeto configurados como obligados  
**Cuando** se prepara el balance  
**Entonces** solo se incluyen los productos/movimientos del alcance normativo aplicable

#### CA-RF-CTL-014-02-RECON

**Dado** saldo inicial, movimientos y saldo final  
**Cuando** se genera el balance  
**Entonces** las diferencias se bloquean o quedan justificadas antes de marcarlo listo

#### CA-RF-CTL-014-03-HIS

**Dado** un balance presentado  
**Cuando** existe una corrección posterior  
**Entonces** se conserva la versión/evidencia presentada y la nueva regularización


### RF-CTL-015 — Registrar presentación de balance

#### CA-RF-CTL-015-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Registrar presentación de balance"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-015-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-015-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Registrar presentación de balance"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-016 — Reportar receta falsificada/adulterada

#### CA-RF-CTL-016-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Reportar receta falsificada/adulterada"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-016-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-016-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Reportar receta falsificada/adulterada"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-017 — Restringir promoción de controlados

#### CA-RF-CTL-017-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Restringir promoción de controlados"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-017-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-017-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Restringir promoción de controlados"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-CTL-018 — Auditar acceso y movimientos controlados

#### CA-RF-CTL-018-01-POS

**Dado** un producto fiscalizado con lista normativa identificada  
**Cuando** se ejecuta "Auditar acceso y movimientos controlados"  
**Entonces** el sistema aplica la regla correspondiente a esa lista/tipo de receta y registra el movimiento controlado

#### CA-RF-CTL-018-02-NORM

**Dado** una receta especial sujeta a plazo o condiciones bloqueantes  
**Cuando** se valida para dispensación  
**Entonces** se rechaza si excede la vigencia aplicable o presenta enmendaduras/sospecha de adulteración según la norma correspondiente

#### CA-RF-CTL-018-03-AUD

**Dado** una operación con producto controlado  
**Cuando** se confirma "Auditar acceso y movimientos controlados"  
**Entonces** actor, adquirente/cantidad cuando aplique, receta/evidencia y movimiento quedan trazables para conciliación


### RF-DEV-001 — Registrar solicitud de devolución

#### CA-RF-DEV-001-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Registrar solicitud de devolución"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-001-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-001-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-002 — Validar elegibilidad comercial

#### CA-RF-DEV-002-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Validar elegibilidad comercial"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-002-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-002-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-003 — Autorizar devolución

#### CA-RF-DEV-003-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Autorizar devolución"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-003-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-003-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-004 — Emitir nota de crédito

#### CA-RF-DEV-004-01-LINK

**Dado** una devolución/ajuste elegible y un CPE previo compatible  
**Cuando** se emite nota de crédito  
**Entonces** queda vinculada al comprobante anterior conforme al mecanismo SUNAT

#### CA-RF-DEV-004-02-NOINV

**Dado** la nota de crédito aceptada  
**Cuando** cambia el estado fiscal  
**Entonces** no se incrementa automáticamente stock vendible

#### CA-RF-DEV-004-03-IDE

**Dado** un error técnico de transmisión  
**Cuando** se reintenta la misma nota  
**Entonces** no se genera una segunda nota fiscal equivalente


### RF-DEV-005 — Registrar estado SUNAT de nota

#### CA-RF-DEV-005-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Registrar estado SUNAT de nota"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-005-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-005-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-006 — Recibir producto devuelto

#### CA-RF-DEV-006-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Recibir producto devuelto"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-006-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-006-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-007 — Enviar producto devuelto a evaluación

#### CA-RF-DEV-007-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Enviar producto devuelto a evaluación"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-007-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-007-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-008 — Determinar disposición de producto devuelto

#### CA-RF-DEV-008-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Determinar disposición de producto devuelto"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-008-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-008-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-009 — Reingresar a stock solo con autorización

#### CA-RF-DEV-009-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Reingresar a stock solo con autorización"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-009-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-009-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-010 — Gestionar devolución parcial

#### CA-RF-DEV-010-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Gestionar devolución parcial"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-010-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-010-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-012 — Registrar reembolso

#### CA-RF-DEV-012-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Registrar reembolso"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-012-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-012-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DEV-013 — Auditar devolución

#### CA-RF-DEV-013-01-POS

**Dado** una venta/comprobante previo elegible  
**Cuando** se ejecuta "Auditar devolución"  
**Entonces** la devolución/ajuste queda vinculada a la transacción original sin modificarla destructivamente

#### CA-RF-DEV-013-02-SEP

**Dado** una nota de crédito o reembolso  
**Cuando** se confirma el efecto comercial/fiscal  
**Entonces** el producto físico no reingresa automáticamente al stock vendible

#### CA-RF-DEV-013-03-VAL

**Dado** un producto devuelto físicamente  
**Cuando** se recibe  
**Entonces** queda no vendible/en evaluación hasta una decisión de disposición o reingreso autorizada


### RF-DSP-001 — Registrar/recibir prescripción

#### CA-RF-DSP-001-01-REG

**Dado** una receta presentada  
**Cuando** se registra  
**Entonces** se captura soporte/tipo, paciente, prescriptor, fecha y líneas disponibles y queda pendiente de validación

#### CA-RF-DSP-001-02-PRIV

**Dado** datos de receta/paciente  
**Cuando** un perfil comercial intenta consultarlos sin finalidad/permiso  
**Entonces** el acceso se deniega y audita

#### CA-RF-DSP-001-03-STATE

**Dado** una receta recién registrada  
**Cuando** finaliza el registro  
**Entonces** no se marca automáticamente como atendida/dispensada


### RF-DSP-002 — Capturar datos del prescriptor

#### CA-RF-DSP-002-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Capturar datos del prescriptor"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-002-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-002-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Capturar datos del prescriptor"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-003 — Capturar datos del paciente

#### CA-RF-DSP-003-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Capturar datos del paciente"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-003-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-003-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Capturar datos del paciente"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-004 — Registrar líneas prescritas

#### CA-RF-DSP-004-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Registrar líneas prescritas"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-004-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-004-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Registrar líneas prescritas"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-005 — Validar integridad de receta

#### CA-RF-DSP-005-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Validar integridad de receta"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-005-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-005-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Validar integridad de receta"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-006 — Validar vigencia de receta

#### CA-RF-DSP-006-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Validar vigencia de receta"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-006-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-006-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Validar vigencia de receta"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-007 — Validar condición de venta

#### CA-RF-DSP-007-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Validar condición de venta"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-007-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-007-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Validar condición de venta"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-008 — Asignar QF responsable

#### CA-RF-DSP-008-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Asignar QF responsable"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-008-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-008-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Asignar QF responsable"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-009 — Analizar prescripción

#### CA-RF-DSP-009-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Analizar prescripción"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-009-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-009-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Analizar prescripción"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-010 — Registrar observación al prescriptor

#### CA-RF-DSP-010-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Registrar observación al prescriptor"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-010-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-010-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Registrar observación al prescriptor"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-011 — Seleccionar producto/lote dispensable

#### CA-RF-DSP-011-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Seleccionar producto/lote dispensable"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-011-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-011-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Seleccionar producto/lote dispensable"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-012 — Registrar dispensación total

#### CA-RF-DSP-012-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Registrar dispensación total"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-012-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-012-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Registrar dispensación total"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-013 — Registrar dispensación parcial

#### CA-RF-DSP-013-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Registrar dispensación parcial"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-013-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-013-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Registrar dispensación parcial"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-014 — Registrar entrega e información al usuario

#### CA-RF-DSP-014-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Registrar entrega e información al usuario"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-014-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-014-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Registrar entrega e información al usuario"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-015 — Relacionar dispensación con venta POS

#### CA-RF-DSP-015-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Relacionar dispensación con venta POS"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-015-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-015-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Relacionar dispensación con venta POS"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-016 — Impedir dispensación por personal no competente

#### CA-RF-DSP-016-01-DEN

**Dado** personal técnico y un medicamento de venta bajo receta  
**Cuando** intenta confirmar la dispensación  
**Entonces** el sistema deniega el acto

#### CA-RF-DSP-016-02-ADMIN

**Dado** el mismo personal técnico  
**Cuando** realiza una acción administrativa expresamente permitida por su rol  
**Entonces** puede completarla sin adquirir competencia para dispensar

#### CA-RF-DSP-016-03-AUD

**Dado** un intento denegado de dispensación  
**Cuando** se evalúa la autorización  
**Entonces** queda auditado usuario, local, operación y resultado


### RF-DSP-017 — Impedir alternativa automática al medicamento prescrito

#### CA-RF-DSP-017-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Impedir alternativa automática al medicamento prescrito"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-017-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-017-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Impedir alternativa automática al medicamento prescrito"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-018 — Conservar evidencia de receta atendida

#### CA-RF-DSP-018-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Conservar evidencia de receta atendida"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-018-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-018-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Conservar evidencia de receta atendida"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-019 — Registrar rechazo de receta

#### CA-RF-DSP-019-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Registrar rechazo de receta"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-019-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-019-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Registrar rechazo de receta"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-020 — Auditar dispensación

#### CA-RF-DSP-020-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Auditar dispensación"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-020-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-020-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Auditar dispensación"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-021 — Consultar histórico de dispensaciones

#### CA-RF-DSP-021-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Consultar histórico de dispensaciones"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-021-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-021-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Consultar histórico de dispensaciones"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-DSP-022 — Proteger datos de prescripción

#### CA-RF-DSP-022-01-POS

**Dado** una prescripción y actor competente según la etapa  
**Cuando** se ejecuta "Proteger datos de prescripción"  
**Entonces** la operación de dispensación conserva receta, QF/actor, producto, cantidad y evidencia aplicable

#### CA-RF-DSP-022-02-COMP

**Dado** un producto de venta bajo receta y un usuario que no posee competencia profesional para dispensarlo  
**Cuando** intenta confirmar el acto farmacéutico  
**Entonces** el sistema deniega la operación aunque el usuario tenga acceso técnico a la aplicación

#### CA-RF-DSP-022-03-PRIV

**Dado** datos de receta/paciente  
**Cuando** se consultan o modifican mediante "Proteger datos de prescripción"  
**Entonces** se aplica acceso por finalidad/rol/ámbito y la operación sensible queda auditada


### RF-ERP-001 — Generar resumen financiero de tienda

#### CA-RF-ERP-001-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Generar resumen financiero de tienda"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-001-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-001-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-002 — Validar transacciones antes de posting

#### CA-RF-ERP-002-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Validar transacciones antes de posting"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-002-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-002-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-003 — Conciliar medios de pago

#### CA-RF-ERP-003-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Conciliar medios de pago"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-003-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-003-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-007 — Generar asiento/posting de venta

#### CA-RF-ERP-007-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Generar asiento/posting de venta"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-007-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-007-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-008 — Generar posting de devoluciones

#### CA-RF-ERP-008-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Generar posting de devoluciones"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-008-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-008-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-009 — Generar posting de inventario/costo

#### CA-RF-ERP-009-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Generar posting de inventario/costo"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-009-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-009-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-010 — Aplicar clave idempotente de posting

#### CA-RF-ERP-010-01-ONE

**Dado** una clave idempotente de posting ya aceptada  
**Cuando** el proceso se ejecuta otra vez  
**Entonces** no se crea un segundo asiento/posting válido

#### CA-RF-ERP-010-02-FAIL

**Dado** un timeout con resultado desconocido  
**Cuando** se reintenta  
**Entonces** se consulta/reconcilia por clave antes de crear una nueva intención

#### CA-RF-ERP-010-03-REV

**Dado** un posting aceptado que necesita corrección  
**Cuando** se regulariza  
**Entonces** se usa reversa/ajuste con nueva trazabilidad y no edición destructiva


### RF-ERP-011 — Registrar aceptación/rechazo ERP

#### CA-RF-ERP-011-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Registrar aceptación/rechazo ERP"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-011-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-011-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-012 — Reintentar posting fallido

#### CA-RF-ERP-012-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Reintentar posting fallido"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-012-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-012-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-013 — Registrar ajustes/reversas

#### CA-RF-ERP-013-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Registrar ajustes/reversas"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-013-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-013-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-014 — Separar cierre de caja y cierre contable

#### CA-RF-ERP-014-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Separar cierre de caja y cierre contable"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-014-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-014-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-015 — Consultar pendientes de integración

#### CA-RF-ERP-015-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Consultar pendientes de integración"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-015-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-015-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-ERP-018 — Auditar posting financiero

#### CA-RF-ERP-018-01-POS

**Dado** transacciones operativas consistentes y un período/lote de integración válido  
**Cuando** se ejecuta "Auditar posting financiero"  
**Entonces** el posting/resumen conserva referencias a las transacciones fuente y al resultado del ERP

#### CA-RF-ERP-018-02-IDE

**Dado** una clave idempotente ya aceptada por el destino  
**Cuando** se reintenta la misma intención  
**Entonces** no se genera un segundo posting financiero válido

#### CA-RF-ERP-018-03-REV

**Dado** un posting ya contabilizado que requiere corrección  
**Cuando** se procesa el ajuste  
**Entonces** se crea reversa/ajuste trazable y no se edita destructivamente el asiento original


### RF-FVG-001 — Registrar reporte de evento

#### CA-RF-FVG-001-01-NOSELL

**Dado** una sospecha comunicada sobre un producto que no fue vendido por la cadena  
**Cuando** el reportante la registra  
**Entonces** el sistema permite crear el reporte

#### CA-RF-FVG-001-02-MIN

**Dado** información parcial disponible  
**Cuando** se crea el reporte  
**Entonces** se conserva lo recibido y puede solicitarse follow-up sin inventar campos ausentes

#### CA-RF-FVG-001-03-PRIV

**Dado** datos de salud/personales  
**Cuando** se guarda/consulta el reporte  
**Entonces** se aplican controles de acceso y auditoría de datos sensibles


### RF-FVG-002 — Clasificar medicamento/dispositivo

#### CA-RF-FVG-002-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Clasificar medicamento/dispositivo"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-002-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Clasificar medicamento/dispositivo"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-002-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-003 — Registrar producto sospechoso

#### CA-RF-FVG-003-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Registrar producto sospechoso"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-003-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Registrar producto sospechoso"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-003-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-004 — Registrar paciente protegido

#### CA-RF-FVG-004-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Registrar paciente protegido"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-004-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Registrar paciente protegido"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-004-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-005 — Registrar reportante

#### CA-RF-FVG-005-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Registrar reportante"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-005-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Registrar reportante"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-005-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-006 — Registrar descripción del evento

#### CA-RF-FVG-006-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Registrar descripción del evento"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-006-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Registrar descripción del evento"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-006-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-007 — Clasificar gravedad/prioridad

#### CA-RF-FVG-007-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Clasificar gravedad/prioridad"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-007-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Clasificar gravedad/prioridad"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-007-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-008 — Asignar responsable QF

#### CA-RF-FVG-008-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Asignar responsable QF"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-008-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Asignar responsable QF"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-008-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-009 — Solicitar información faltante

#### CA-RF-FVG-009-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Solicitar información faltante"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-009-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Solicitar información faltante"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-009-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-010 — Determinar plazo aplicable

#### CA-RF-FVG-010-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Determinar plazo aplicable"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-010-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Determinar plazo aplicable"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-010-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-011 — Registrar notificación oficial

#### CA-RF-FVG-011-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Registrar notificación oficial"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-011-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Registrar notificación oficial"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-011-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-012 — Actualizar reporte sin borrar historia

#### CA-RF-FVG-012-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Actualizar reporte sin borrar historia"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-012-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Actualizar reporte sin borrar historia"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-012-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-013 — Cerrar reporte

#### CA-RF-FVG-013-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Cerrar reporte"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-013-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Cerrar reporte"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-013-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-FVG-014 — Auditar acceso a datos sensibles

#### CA-RF-FVG-014-01-POS

**Dado** un reporte de sospecha/evento con información disponible  
**Cuando** se ejecuta "Auditar acceso a datos sensibles"  
**Entonces** el reporte puede registrarse aun sin venta POS y conserva el contenido original recibido

#### CA-RF-FVG-014-02-PRIV

**Dado** datos de paciente/reportante/evento  
**Cuando** se acceden mediante "Auditar acceso a datos sensibles"  
**Entonces** se aplica mínimo privilegio/finalidad y se audita el acceso sensible

#### CA-RF-FVG-014-03-HIS

**Dado** información adicional posterior  
**Cuando** se actualiza el reporte  
**Entonces** se agrega complemento/versión sin borrar el reporte inicial ni la evidencia de notificación previa


### RF-INT-001 — Integrar SUNAT CPE

#### CA-RF-INT-001-01-POS

**Dado** un contrato externo versionado y una operación válida  
**Cuando** se ejecuta "Integrar SUNAT CPE"  
**Entonces** se registra solicitud, respuesta/estado y correlación sin perder la transacción de negocio fuente

#### CA-RF-INT-001-02-IDE

**Dado** un reintento de una integración reintentable  
**Cuando** se envía la misma intención  
**Entonces** se usa una clave idempotente o mecanismo equivalente para impedir duplicados

#### CA-RF-INT-001-03-ERR

**Dado** un error del sistema externo  
**Cuando** se clasifica la respuesta  
**Entonces** se distingue reintentable/definitivo y se conserva el error para seguimiento sin bucles infinitos


### RF-INT-002 — Integrar ERP externo cuando aplique

#### CA-RF-INT-002-01-POS

**Dado** un contrato externo versionado y una operación válida  
**Cuando** se ejecuta "Integrar ERP externo cuando aplique"  
**Entonces** se registra solicitud, respuesta/estado y correlación sin perder la transacción de negocio fuente

#### CA-RF-INT-002-02-IDE

**Dado** un reintento de una integración reintentable  
**Cuando** se envía la misma intención  
**Entonces** se usa una clave idempotente o mecanismo equivalente para impedir duplicados

#### CA-RF-INT-002-03-ERR

**Dado** un error del sistema externo  
**Cuando** se clasifica la respuesta  
**Entonces** se distingue reintentable/definitivo y se conserva el error para seguimiento sin bucles infinitos


### RF-INT-008 — Aplicar idempotencia

#### CA-RF-INT-008-01-POS

**Dado** un contrato externo versionado y una operación válida  
**Cuando** se ejecuta "Aplicar idempotencia"  
**Entonces** se registra solicitud, respuesta/estado y correlación sin perder la transacción de negocio fuente

#### CA-RF-INT-008-02-IDE

**Dado** un reintento de una integración reintentable  
**Cuando** se envía la misma intención  
**Entonces** se usa una clave idempotente o mecanismo equivalente para impedir duplicados

#### CA-RF-INT-008-03-ERR

**Dado** un error del sistema externo  
**Cuando** se clasifica la respuesta  
**Entonces** se distingue reintentable/definitivo y se conserva el error para seguimiento sin bucles infinitos


### RF-INT-009 — Registrar errores/reintentos

#### CA-RF-INT-009-01-POS

**Dado** un contrato externo versionado y una operación válida  
**Cuando** se ejecuta "Registrar errores/reintentos"  
**Entonces** se registra solicitud, respuesta/estado y correlación sin perder la transacción de negocio fuente

#### CA-RF-INT-009-02-IDE

**Dado** un reintento de una integración reintentable  
**Cuando** se envía la misma intención  
**Entonces** se usa una clave idempotente o mecanismo equivalente para impedir duplicados

#### CA-RF-INT-009-03-ERR

**Dado** un error del sistema externo  
**Cuando** se clasifica la respuesta  
**Entonces** se distingue reintentable/definitivo y se conserva el error para seguimiento sin bucles infinitos


### RF-INV-001 — Registrar lote

#### CA-RF-INV-001-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Registrar lote"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-001-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Registrar lote"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-001-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-002 — Registrar fecha de vencimiento

#### CA-RF-INV-002-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Registrar fecha de vencimiento"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-002-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Registrar fecha de vencimiento"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-002-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-003 — Registrar inventario por ubicación

#### CA-RF-INV-003-01-MOV

**Dado** una entrada o salida de inventario  
**Cuando** cambia la existencia por ubicación  
**Entonces** existe un movimiento justificante asociado

#### CA-RF-INV-003-02-STATE

**Dado** stock en cuarentena o bloqueado  
**Cuando** se consulta disponibilidad de venta  
**Entonces** esa cantidad no forma parte del disponible vendible

#### CA-RF-INV-003-03-RECON

**Dado** un período de movimientos  
**Cuando** se reconstruye el saldo  
**Entonces** el saldo resultante coincide con la proyección de existencias o genera una diferencia investigable


### RF-INV-004 — Distinguir estado de inventario

#### CA-RF-INV-004-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Distinguir estado de inventario"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-004-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Distinguir estado de inventario"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-004-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-005 — Registrar movimiento de inventario

#### CA-RF-INV-005-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Registrar movimiento de inventario"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-005-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Registrar movimiento de inventario"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-005-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-006 — Consultar kardex

#### CA-RF-INV-006-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Consultar kardex"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-006-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Consultar kardex"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-006-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-010 — Bloquear lote

#### CA-RF-INV-010-01-BLOCK

**Dado** un lote existente y una fuente/motivo válido  
**Cuando** un actor autorizado lo bloquea  
**Entonces** todas las existencias del alcance pasan a no vendible sin eliminar el stock físico

#### CA-RF-INV-010-02-POS

**Dado** el lote bloqueado  
**Cuando** se intenta vender/reservar en un canal afectado  
**Entonces** la operación se rechaza

#### CA-RF-INV-010-03-AUD

**Dado** un bloqueo o liberación  
**Cuando** cambia el estado  
**Entonces** se conserva actor, motivo, fuente, fecha y alcance


### RF-INV-011 — Liberar lote bloqueado

#### CA-RF-INV-011-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Liberar lote bloqueado"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-011-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Liberar lote bloqueado"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-011-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-012 — Gestionar cuarentena

#### CA-RF-INV-012-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Gestionar cuarentena"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-012-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Gestionar cuarentena"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-012-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-013 — Consultar próximos vencimientos

#### CA-RF-INV-013-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Consultar próximos vencimientos"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-013-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Consultar próximos vencimientos"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-013-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-018 — Aplicar política de selección de lote

#### CA-RF-INV-018-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Aplicar política de selección de lote"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-018-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Aplicar política de selección de lote"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-018-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-019 — Evitar stock negativo no autorizado

#### CA-RF-INV-019-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Evitar stock negativo no autorizado"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-019-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Evitar stock negativo no autorizado"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-019-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-020 — Trazar origen-destino de lote

#### CA-RF-INV-020-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Trazar origen-destino de lote"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-020-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Trazar origen-destino de lote"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-020-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-INV-021 — Consultar stock corporativo

#### CA-RF-INV-021-01-POS

**Dado** un SKU, ubicación y lote aplicable válidos  
**Cuando** se ejecuta "Consultar stock corporativo"  
**Entonces** todo cambio de cantidad/estado queda sustentado por un movimiento trazable

#### CA-RF-INV-021-02-VAL

**Dado** stock bloqueado, en cuarentena, no vendible o insuficiente  
**Cuando** una operación intenta consumirlo mediante "Consultar stock corporativo"  
**Entonces** el sistema no lo expone como disponible ni permite un saldo negativo no autorizado

#### CA-RF-INV-021-03-CON

**Dado** dos operaciones concurrentes sobre la misma disponibilidad  
**Cuando** ambas intentan reservar/vender/ajustar inventario  
**Entonces** la integridad impide doble asignación y el saldo final puede reconstruirse desde movimientos


### RF-OBS-001 — Determinar establecimientos obligados a reporte

#### CA-RF-OBS-001-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Determinar establecimientos obligados a reporte"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-001-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-001-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-OBS-002 — Seleccionar productos reportables

#### CA-RF-OBS-002-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Seleccionar productos reportables"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-002-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-002-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-OBS-003 — Generar reporte mensual de precios

#### CA-RF-OBS-003-01-MONTH

**Dado** un período mensual y establecimientos obligados  
**Cuando** se genera el reporte  
**Entonces** se incluye solo el alcance reportable definido para ese período

#### CA-RF-OBS-003-02-SOURCE

**Dado** un registro de precio reportado  
**Cuando** se audita  
**Entonces** puede identificarse la versión/fuente del precio que lo produjo

#### CA-RF-OBS-003-03-RETRY

**Dado** un reporte presentado con observaciones  
**Cuando** se reprocesan errores  
**Entonces** el envío anterior no se borra y se conserva evidencia de la nueva presentación


### RF-OBS-004 — Validar consistencia del reporte

#### CA-RF-OBS-004-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Validar consistencia del reporte"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-004-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-004-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-OBS-005 — Registrar presentación

#### CA-RF-OBS-005-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Registrar presentación"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-005-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-005-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-OBS-006 — Reprocesar observaciones

#### CA-RF-OBS-006-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Reprocesar observaciones"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-006-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-006-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-OBS-007 — Consultar cumplimiento por período

#### CA-RF-OBS-007-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Consultar cumplimiento por período"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-007-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-007-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-OBS-008 — Auditar cambios previos al envío

#### CA-RF-OBS-008-01-POS

**Dado** un período, establecimiento obligado y productos reportables determinados  
**Cuando** se ejecuta "Auditar cambios previos al envío"  
**Entonces** el reporte conserva la fuente/versión de precio utilizada para cada registro

#### CA-RF-OBS-008-02-CFG

**Dado** que cambia el mecanismo técnico o alcance regulatorio del Observatorio  
**Cuando** se genera/presenta el reporte  
**Entonces** el sistema utiliza la configuración/fuente vigente sin asumir universalmente API, archivo o portal específico

#### CA-RF-OBS-008-03-HIS

**Dado** un envío presentado con observaciones posteriores  
**Cuando** se reprocesan correcciones  
**Entonces** el envío anterior permanece histórico y se conserva la nueva evidencia


### RF-ORG-002 — Registrar establecimiento farmacéutico

#### CA-RF-ORG-002-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Registrar establecimiento farmacéutico"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-002-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Registrar establecimiento farmacéutico"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-002-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Registrar establecimiento farmacéutico"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-ORG-003 — Mantener autorización sanitaria del establecimiento

#### CA-RF-ORG-003-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Mantener autorización sanitaria del establecimiento"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-003-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Mantener autorización sanitaria del establecimiento"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-003-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Mantener autorización sanitaria del establecimiento"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-ORG-004 — Asignar Director Técnico

#### CA-RF-ORG-004-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Asignar Director Técnico"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-004-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Asignar Director Técnico"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-004-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Asignar Director Técnico"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-ORG-005 — Registrar profesionales QF asistentes

#### CA-RF-ORG-005-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Registrar profesionales QF asistentes"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-005-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Registrar profesionales QF asistentes"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-005-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Registrar profesionales QF asistentes"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-ORG-006 — Registrar personal técnico

#### CA-RF-ORG-006-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Registrar personal técnico"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-006-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Registrar personal técnico"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-006-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Registrar personal técnico"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-ORG-010 — Gestionar vigencia/inactivación de local

#### CA-RF-ORG-010-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Gestionar vigencia/inactivación de local"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-010-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Gestionar vigencia/inactivación de local"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-010-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Gestionar vigencia/inactivación de local"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-ORG-012 — Auditar cambios organizacionales

#### CA-RF-ORG-012-01-POS

**Dado** un establecimiento y actores organizacionales válidos  
**Cuando** se ejecuta "Auditar cambios organizacionales"  
**Entonces** el cambio queda asociado al establecimiento y conserva vigencia/histórico sin borrar períodos anteriores

#### CA-RF-ORG-012-02-VAL

**Dado** que falta una autorización, acreditación o relación requerida por la regla aplicable  
**Cuando** se intenta "Auditar cambios organizacionales"  
**Entonces** el sistema rechaza la transición o la deja pendiente de validación sin inventar habilitaciones

#### CA-RF-ORG-012-03-AUD

**Dado** un cambio organizacional crítico  
**Cuando** se confirma "Auditar cambios organizacionales"  
**Entonces** se registra actor, fecha, establecimiento, valor anterior/nuevo o referencia suficiente para reconstruir el cambio


### RF-POS-001 — Abrir turno de caja

#### CA-RF-POS-001-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Abrir turno de caja"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-001-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-001-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Abrir turno de caja"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-002 — Registrar venta

#### CA-RF-POS-002-01-RX

**Dado** una línea cuya condición exige receta  
**Cuando** el cajero intenta cerrar la venta sin dispensación válida  
**Entonces** el sistema impide el cierre

#### CA-RF-POS-002-02-STOCK

**Dado** stock no vendible o insuficiente  
**Cuando** se confirma la venta  
**Entonces** no se produce venta cerrada ni salida válida de inventario

#### CA-RF-POS-002-03-ATOMIC

**Dado** una confirmación exitosa  
**Cuando** se completa la venta  
**Entonces** venta, pago y movimientos quedan correlacionados y un reintento no duplica la operación


### RF-POS-003 — Agregar producto por escaneo/búsqueda

#### CA-RF-POS-003-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Agregar producto por escaneo/búsqueda"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-003-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-003-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Agregar producto por escaneo/búsqueda"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-004 — Validar stock vendible

#### CA-RF-POS-004-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Validar stock vendible"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-004-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-004-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Validar stock vendible"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-005 — Aplicar precio/promoción vigente

#### CA-RF-POS-005-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Aplicar precio/promoción vigente"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-005-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-005-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Aplicar precio/promoción vigente"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-006 — Gestionar cliente de venta

#### CA-RF-POS-006-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Gestionar cliente de venta"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-006-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-006-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Gestionar cliente de venta"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-007 — Gestionar múltiples medios de pago

#### CA-RF-POS-007-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Gestionar múltiples medios de pago"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-007-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-007-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Gestionar múltiples medios de pago"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-008 — Registrar pago mixto

#### CA-RF-POS-008-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Registrar pago mixto"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-008-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-008-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Registrar pago mixto"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-009 — Emitir boleta electrónica

#### CA-RF-POS-009-01-TYPE

**Dado** una venta a consumidor final apta para boleta  
**Cuando** se emite el CPE  
**Entonces** se genera una boleta con serie/correlativo y datos fiscales según configuración vigente

#### CA-RF-POS-009-02-STATE

**Dado** un CPE enviado  
**Cuando** SUNAT responde  
**Entonces** la respuesta actualiza el estado tributario sin sobrescribir el estado retail de la venta

#### CA-RF-POS-009-03-IDE

**Dado** una falla después de asignar correlativo/enviar  
**Cuando** se reintenta  
**Entonces** no se crea un correlativo/CPE adicional para la misma intención


### RF-POS-010 — Emitir factura electrónica

#### CA-RF-POS-010-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Emitir factura electrónica"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-010-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-010-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Emitir factura electrónica"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-011 — Registrar estado SUNAT del CPE

#### CA-RF-POS-011-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Registrar estado SUNAT del CPE"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-011-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-011-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Registrar estado SUNAT del CPE"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-012 — Reintentar envío CPE

#### CA-RF-POS-012-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Reintentar envío CPE"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-012-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-012-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Reintentar envío CPE"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-013 — Imprimir/entregar representación

#### CA-RF-POS-013-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Imprimir/entregar representación"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-013-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-013-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Imprimir/entregar representación"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-014 — Cancelar venta antes de cierre

#### CA-RF-POS-014-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Cancelar venta antes de cierre"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-014-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-014-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Cancelar venta antes de cierre"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-015 — Registrar retiro/ingreso de efectivo

#### CA-RF-POS-015-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Registrar retiro/ingreso de efectivo"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-015-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-015-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Registrar retiro/ingreso de efectivo"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-016 — Realizar arqueo

#### CA-RF-POS-016-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Realizar arqueo"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-016-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-016-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Realizar arqueo"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-017 — Registrar diferencia de caja

#### CA-RF-POS-017-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Registrar diferencia de caja"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-017-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-017-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Registrar diferencia de caja"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-018 — Cerrar turno

#### CA-RF-POS-018-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Cerrar turno"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-018-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-018-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Cerrar turno"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-019 — Consultar ventas de turno/local

#### CA-RF-POS-019-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Consultar ventas de turno/local"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-019-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-019-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Consultar ventas de turno/local"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-POS-022 — Auditar operaciones POS sensibles

#### CA-RF-POS-022-01-POS

**Dado** un turno activo, operador autorizado y productos vendibles  
**Cuando** se ejecuta "Auditar operaciones POS sensibles"  
**Entonces** la operación conserva local, caja, turno, operador, líneas, importes y estado de forma trazable

#### CA-RF-POS-022-02-SEP

**Dado** una venta con pago y/o CPE  
**Cuando** cambia el estado de uno de esos componentes  
**Entonces** venta, pago y estado tributario permanecen relacionados pero no se sobrescriben como un único estado

#### CA-RF-POS-022-03-IDE

**Dado** un reintento técnico o solicitud repetida con la misma intención  
**Cuando** se vuelve a ejecutar "Auditar operaciones POS sensibles"  
**Entonces** no se crea una segunda venta/CPE/movimiento equivalente cuando la operación exige idempotencia


### RF-PRE-001 — Registrar precio base

#### CA-RF-PRE-001-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Registrar precio base"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-001-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-001-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-002 — Definir precio por ámbito

#### CA-RF-PRE-002-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Definir precio por ámbito"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-002-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-002-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-003 — Aprobar precio

#### CA-RF-PRE-003-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Aprobar precio"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-003-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-003-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-004 — Publicar precio

#### CA-RF-PRE-004-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Publicar precio"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-004-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-004-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-005 — Registrar promoción

#### CA-RF-PRE-005-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Registrar promoción"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-005-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-005-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-006 — Validar compatibilidad de promoción

#### CA-RF-PRE-006-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Validar compatibilidad de promoción"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-006-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-006-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-007 — Resolver precio efectivo

#### CA-RF-PRE-007-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Resolver precio efectivo"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-007-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-007-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-008 — Registrar descuentos manuales

#### CA-RF-PRE-008-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Registrar descuentos manuales"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-008-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-008-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-009 — Registrar evidencia de precio aplicado

#### CA-RF-PRE-009-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Registrar evidencia de precio aplicado"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-009-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-009-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-010 — Preparar reporte Observatorio

#### CA-RF-PRE-010-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Preparar reporte Observatorio"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-010-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-010-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-011 — Validar reporte Observatorio

#### CA-RF-PRE-011-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Validar reporte Observatorio"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-011-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-011-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-012 — Registrar envío/carga al Observatorio

#### CA-RF-PRE-012-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Registrar envío/carga al Observatorio"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-012-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-012-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-013 — Reprocesar errores de reporte

#### CA-RF-PRE-013-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Reprocesar errores de reporte"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-013-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-013-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-PRE-014 — Consultar histórico de precios

#### CA-RF-PRE-014-01-POS

**Dado** un SKU, ámbito, canal y vigencia válidos  
**Cuando** se ejecuta "Consultar histórico de precios"  
**Entonces** el precio/promoción queda versionado y el POS puede reproducir qué regla fue aplicada

#### CA-RF-PRE-014-02-VAL

**Dado** que una promoción entra en conflicto con una restricción sanitaria o de receta  
**Cuando** se intenta aplicarla  
**Entonces** la promoción no elimina el bloqueo ni el requisito farmacéutico

#### CA-RF-PRE-014-03-AUD

**Dado** un precio, promoción o descuento manual aplicado  
**Cuando** se confirma la operación  
**Entonces** se conserva fuente/versión, actor y motivo cuando corresponda


### RF-RCL-001 — Registrar alerta

#### CA-RF-RCL-001-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Registrar alerta"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-001-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-001-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-002 — Clasificar acción de alerta

#### CA-RF-RCL-002-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Clasificar acción de alerta"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-002-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-002-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-003 — Identificar stock afectado

#### CA-RF-RCL-003-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Identificar stock afectado"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-003-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-003-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-004 — Bloquear venta por lote

#### CA-RF-RCL-004-01-BLOCK

**Dado** una alerta activa que incluye un lote  
**Cuando** se activa el bloqueo  
**Entonces** ninguna existencia de ese lote en el alcance se ofrece como vendible

#### CA-RF-RCL-004-02-CHANNEL

**Dado** el mismo lote  
**Cuando** POS/e-commerce/reserva intenta consumirlo  
**Entonces** todos los canales afectados reciben rechazo consistente

#### CA-RF-RCL-004-03-HIST

**Dado** ventas anteriores al bloqueo  
**Cuando** se ejecuta el recall  
**Entonces** esas ventas permanecen históricas y son localizables, no modificadas


### RF-RCL-005 — Propagar bloqueo a canales

#### CA-RF-RCL-005-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Propagar bloqueo a canales"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-005-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-005-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-006 — Generar tareas de inmovilización

#### CA-RF-RCL-006-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Generar tareas de inmovilización"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-006-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-006-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-007 — Confirmar inmovilización física

#### CA-RF-RCL-007-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Confirmar inmovilización física"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-007-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-007-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-008 — Localizar movimientos/ventas históricos

#### CA-RF-RCL-008-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Localizar movimientos/ventas históricos"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-008-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-008-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-009 — Gestionar destino del lote

#### CA-RF-RCL-009-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Gestionar destino del lote"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-009-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-009-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-010 — Conciliar recall

#### CA-RF-RCL-010-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Conciliar recall"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-010-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-010-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-011 — Cerrar recall

#### CA-RF-RCL-011-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Cerrar recall"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-011-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-011-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-RCL-012 — Auditar recall

#### CA-RF-RCL-012-01-POS

**Dado** una alerta con producto/lote y alcance identificados  
**Cuando** se ejecuta "Auditar recall"  
**Entonces** se localizan existencias afectadas y se conserva fuente, versión y trazabilidad de la acción

#### CA-RF-RCL-012-02-BLOCK

**Dado** un lote incluido en una alerta activa de bloqueo  
**Cuando** POS/e-commerce/reserva intenta venderlo  
**Entonces** la operación se rechaza en todos los canales del alcance afectado

#### CA-RF-RCL-012-03-REC

**Dado** un recall en proceso  
**Cuando** se intenta cerrarlo  
**Entonces** se exige conciliación de cantidades afectadas/localizadas/destino y evidencia por establecimiento


### RF-SEC-002 — Gestionar roles

#### CA-RF-SEC-002-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Gestionar roles"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-002-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-002-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-003 — Gestionar permisos

#### CA-RF-SEC-003-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Gestionar permisos"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-003-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-003-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-004 — Gestionar ámbitos de acceso

#### CA-RF-SEC-004-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Gestionar ámbitos de acceso"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-004-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-004-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-005 — Restringir actos farmacéuticos por competencia

#### CA-RF-SEC-005-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Restringir actos farmacéuticos por competencia"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-005-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-005-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-006 — Proteger datos personales

#### CA-RF-SEC-006-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Proteger datos personales"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-006-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-006-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-007 — Proteger datos de salud/recetas/FVG

#### CA-RF-SEC-007-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Proteger datos de salud/recetas/FVG"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-007-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-007-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-010 — Registrar accesos denegados

#### CA-RF-SEC-010-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Registrar accesos denegados"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-010-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-010-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-SEC-011 — Gestionar segregación de funciones

#### CA-RF-SEC-011-01-AUTH

**Dado** un usuario autenticado  
**Cuando** intenta "Gestionar segregación de funciones"  
**Entonces** la autorización evalúa rol, permiso, ámbito y reglas contextuales/profesionales antes de permitir el acto

#### CA-RF-SEC-011-02-DEN

**Dado** un usuario sin permiso, fuera de ámbito o sin competencia profesional  
**Cuando** intenta la operación  
**Entonces** recibe una denegación controlada y no se modifica el estado de negocio

#### CA-RF-SEC-011-03-AUD

**Dado** una operación sensible permitida o denegada  
**Cuando** termina la evaluación de acceso  
**Entonces** se registra el evento de seguridad/auditoría sin exponer secretos ni datos innecesarios


### RF-TRF-001 — Crear solicitud de transferencia

#### CA-RF-TRF-001-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Crear solicitud de transferencia"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-001-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-001-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-002 — Aprobar transferencia

#### CA-RF-TRF-002-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Aprobar transferencia"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-002-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-002-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-003 — Reservar stock en origen

#### CA-RF-TRF-003-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Reservar stock en origen"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-003-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-003-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-004 — Realizar picking por lote

#### CA-RF-TRF-004-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Realizar picking por lote"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-004-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-004-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-005 — Despachar transferencia

#### CA-RF-TRF-005-01-TRANSIT

**Dado** una transferencia aprobada y picking confirmado  
**Cuando** se despacha  
**Entonces** disminuye el disponible de origen y aumenta stock en tránsito, no el disponible del destino

#### CA-RF-TRF-005-02-TRACE

**Dado** una transferencia por lote  
**Cuando** se despacha  
**Entonces** el lote/origen/destino/cantidades y documento quedan trazables

#### CA-RF-TRF-005-03-BLOCK

**Dado** stock bloqueado  
**Cuando** se intenta incluir en un despacho ordinario  
**Entonces** el sistema lo rechaza salvo un proceso específico autorizado


### RF-TRF-006 — Registrar documento de traslado

#### CA-RF-TRF-006-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Registrar documento de traslado"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-006-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-006-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-007 — Recibir transferencia

#### CA-RF-TRF-007-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Recibir transferencia"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-007-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-007-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-008 — Registrar diferencias de transferencia

#### CA-RF-TRF-008-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Registrar diferencias de transferencia"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-008-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-008-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-009 — Resolver incidencia de transferencia

#### CA-RF-TRF-009-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Resolver incidencia de transferencia"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-009-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-009-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


### RF-TRF-010 — Cerrar transferencia

#### CA-RF-TRF-010-01-POS

**Dado** origen, destino y stock transferible válidos  
**Cuando** se ejecuta "Cerrar transferencia"  
**Entonces** la transferencia conserva trazabilidad por SKU/lote desde origen hasta destino

#### CA-RF-TRF-010-02-STA

**Dado** una transferencia despachada pero no recibida  
**Cuando** se consulta su stock  
**Entonces** la cantidad está en tránsito y no se considera disponible en destino

#### CA-RF-TRF-010-03-VAL

**Dado** una diferencia, daño o lote bloqueado  
**Cuando** se intenta continuar/cerrar la transferencia  
**Entonces** se exige incidencia/resolución o autorización específica antes del cierre


## 4. Criterios E2E transversales

### CA-E2E-FAR-001 — Compra a venta

**Dado** un sku publicado y una oc aprobadan SKU publicado y una OC aprobada  
**Cuando** se recibe por lote, se publica precio y se vendee recibe por lote, se publica precio y se vende  
**Entonces** la venta puede rastrearse hasta recepción/proveedor/lote y el inventario/erp reflejan eventos separados y consistentes.a venta puede rastrearse hasta recepción/proveedor/lote y el inventario/ERP reflejan eventos separados y consistentes.

### CA-E2E-FAR-002 — Venta bajo receta

**Dado** un producto cuya condición exige recetan producto cuya condición exige receta  
**Cuando** el cliente intenta comprarlol cliente intenta comprarlo  
**Entonces** el pos no cierra la línea sin una dispensación válida ejecutada por actor competente.l POS no cierra la línea sin una dispensación válida ejecutada por actor competente.

### CA-E2E-FAR-003 — Controlado

**Dado** un producto fiscalizado y receta especial aplicablen producto fiscalizado y receta especial aplicable  
**Cuando** se dispensae dispensa  
**Entonces** se aplican vigencia/validaciones de su lista, se registra el movimiento controlado y queda conciliable.e aplican vigencia/validaciones de su lista, se registra el movimiento controlado y queda conciliable.

### CA-E2E-FAR-004 — Devolución

**Dado** una venta y cpe previosna venta y CPE previos  
**Cuando** se gestiona una devolucióne gestiona una devolución  
**Entonces** nota de crédito/reembolso y disposición física son procesos vinculados pero independientes y no reescriben la venta.ota de crédito/reembolso y disposición física son procesos vinculados pero independientes y no reescriben la venta.

### CA-E2E-FAR-005 — Recall

**Dado** una alerta de lotena alerta de lote  
**Cuando** se activae activa  
**Entonces** se bloquean todos los canales, se inmoviliza por local y el cierre exige conciliación.e bloquean todos los canales, se inmoviliza por local y el cierre exige conciliación.

### CA-E2E-FAR-006 — FVG sin venta

**Dado** una sram/evento de un producto no vendido por la cadenana SRAM/evento de un producto no vendido por la cadena  
**Cuando** se reportae reporta  
**Entonces** se registra y procesa sin exigir ticket pos, preservando privacidad.e registra y procesa sin exigir ticket POS, preservando privacidad.

### CA-E2E-FAR-007 — Cierre ERP

**Dado** un turno cerrado con transacciones válidasn turno cerrado con transacciones válidas  
**Cuando** se publica al erp y ocurre un reintentoe publica al ERP y ocurre un reintento  
**Entonces** una clave idempotente evita doble posting y las correcciones posteriores son reversas/ajustes trazables.na clave idempotente evita doble posting y las correcciones posteriores son reversas/ajustes trazables.

### CA-E2E-FAR-008 — Bloqueo sanitario concurrente

**Dado** una venta en curso y un bloqueo de lote concurrentena venta en curso y un bloqueo de lote concurrente  
**Cuando** ambas compiten por confirmarmbas compiten por confirmar  
**Entonces** el sistema no deja una nueva venta cerrada con stock que ya quedó no vendible según el orden transaccional definido.l sistema no deja una nueva venta cerrada con stock que ya quedó no vendible según el orden transaccional definido.

## 5. Gobierno de aceptación

- Un RF no se considera terminado solo por retornar HTTP 200; debe satisfacer sus CA positivos, negativos, de seguridad y de histórico aplicables.
- Los CA regulatorios deben revalidarse cuando cambie la fuente oficial.
- Los CA de concurrencia/idempotencia deberán ejecutarse como pruebas de integración/contrato, no solo unitarias.
- Los CA que dependen de una política `CFG/POR_VALIDAR` se mantienen como candidatos hasta que la cadena apruebe esa política.

## Navegación dinámica RBAC

### CA-RF-SEC-013-01 — Sesión y contexto
**Dado** un usuario autenticado con tenant/empresa/establecimiento vigente  
**Cuando** consulta `/api/v1/me/navigation?application=ERP_WEB`  
**Entonces** el backend deriva la identidad desde la sesión y devuelve solo opciones visibles según sus permisos/ámbitos.

### CA-RF-SEC-013-02 — Sin IDs internos
**Dado** un árbol de navegación válido  
**Cuando** se serializa la respuesta  
**Entonces** no se exponen `menu_id`, `permiso_id`, `rol_id` ni PK técnicas.

### CA-RF-SEC-013-03 — Aplicación validada
**Dado** un parámetro `application` no permitido  
**Cuando** se procesa la consulta  
**Entonces** se rechaza de forma controlada y no se construye SQL dinámico a partir del valor recibido.

### CA-RF-SEC-014-01 — Ocultar no autoriza
**Dado** un endpoint protegido cuyo menú no es visible al usuario  
**Cuando** el usuario invoca directamente el endpoint  
**Entonces** el backend ejecuta igualmente la autorización y devuelve `403` si no posee permiso/ámbito suficiente.

### CA-RF-SEC-014-02 — Menú visible no basta
**Dado** que una opción aparece en navegación por caché o configuración  
**Cuando** el usuario ha perdido el permiso antes de ejecutar la operación  
**Entonces** el endpoint deniega la acción aunque el menú siga temporalmente visible.
