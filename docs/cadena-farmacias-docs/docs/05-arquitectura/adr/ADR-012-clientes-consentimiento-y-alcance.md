# ADR-012 — Clientes: alcance, consentimiento y permisos

**Estado:** Aceptado para el diseño de la v1; implementación pendiente. Las validaciones externas listadas abajo siguen `POR_VALIDAR`.
**Fecha:** 2026-10-30. **Origen:** DOM/TEC. No es una obligación `NORM`: las obligaciones de privacidad están en `RN-SEC-001`, `RN-SEC-003` y `RN-FVG-002`.
**Especificación de decisiones:** [clientes-fase0](../../../../../superpowers/specs/2026-10-30-clientes-fase0-decisiones-design.md). **Plan:** [módulo Clientes](../../../../../superpowers/plans/2026-10-30-modulo-clientes.md).

## Contexto

El modelo físico ya tiene `sch_venta.cliente` y `sch_venta.cliente_consentimiento` (V007), pero el módulo `clientes` está vacío y las ventas son anónimas. `RF-POS-006` pide identificar al cliente cuando sea requerido o voluntario conforme a privacidad. `04-dominio/10-decisiones-abiertas.md` §12 y §13 dejaban abiertas la identidad cliente/paciente y las finalidades de tratamiento.

## Decisión

1. **Cliente y paciente son entidades distintas.** `ClienteRetail` no exige historia clínica. `reporte_seguridad.paciente_cliente_id` conserva su referencia al cliente; el módulo `clientes` no modela paciente.
2. **Alcance de la v1.** Registrar, editar, bloquear y desbloquear cliente; buscar por documento o nombre; consentimiento por finalidad; selector en el POS. Quedan fuera fidelización, crédito por cliente, delivery y segmentación.
3. **Consentimiento por finalidad.**
   - Catálogo de finalidades: `IDENTIFICACION_VENTA`, `MARKETING`, `FIDELIZACION`.
   - Marketing y fidelización se consienten por separado y no se infieren de una compra ni de una receta (`RN-SEC-003`). La identificación para la venta no se condiciona a marketing.
   - Cada consentimiento registra versión del texto, canal, fecha, quién lo registró y evidencia. La revocación es un nuevo estado y conserva el histórico; reemplaza el `ON DELETE CASCADE` actual.
4. **Cliente sin identificar.** `tipo_cliente = SIN_IDENTIFICAR` implica documento nulo, y la venta anónima sigue permitida por defecto. El documento se valida contra `tipo_documento_identidad`, con FK y longitud por tipo (DNI de 8 dígitos, RUC de 11), y se alinea la longitud de `cliente.tipo_documento` con el catálogo.
5. **Datos personales.** `fecha_nacimiento` y `email` se clasifican como datos personales con minimización, y `fecha_nacimiento` es opcional. El cifrado y la RLS por tenant no entran en la v1; hoy el aislamiento depende de la aplicación y de las FKs compuestas con `tenant_id`, y se decidirá en un ADR aparte.
6. **Permisos.** `clientes.clientes.consultar`, `clientes.clientes.registrar`, `clientes.clientes.editar`, `clientes.clientes.bloquear` y `clientes.consentimientos.gestionar`, sembrados en una migración y concedidos al rol ADMIN de FARMALAB, como `V025`. La sección del frontend pasa de `ventas.ventas.` a `clientes.clientes.consultar`. Qué otros roles los reciben, y si el cajero puede registrar cliente, se define al implementar la fase 1.

## Pendiente de validación externa (`POR_VALIDAR`)

- Base jurídica de cada finalidad de consentimiento, con asesoría legal.
- Plazos de retención y anonimización.
- Umbral de identificación obligatoria por monto, contra la normativa tributaria vigente. No se codifica ningún valor hasta confirmarlo.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Unificar cliente y paciente en una persona | Finalidades de tratamiento distintas (§13) |
| Un único consentimiento general | Contradice `RN-SEC-003` |
| Mantener el borrado en cascada del consentimiento | Elimina la evidencia de lo consentido y revocado |
| Incluir fidelización, crédito y delivery en la v1 | Fuera del alcance ya delimitado en `06-datos` |
| Mantener `ventas.ventas.` como permiso de la sección | Mezcla la autorización de clientes con la de ventas |

## Consecuencias

Se requiere una migración de modelo (documento, finalidades, consentimiento y permisos), un módulo `clientes` completo con `catalogo::api` en `allowedDependencies` y en `build.gradle`, y `clienteId` opcional en ventas. Hasta cerrar las validaciones externas, el sistema no puede afirmar una base jurídica ni un plazo de retención. Las ventas anónimas siguen funcionando.
