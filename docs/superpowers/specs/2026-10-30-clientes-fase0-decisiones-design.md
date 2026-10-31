# Clientes — Fase 0: decisiones por cerrar

**Fecha:** 2026-10-30
**Estado:** D1 a D6 aceptadas por el usuario el 2026-10-30 y registradas en [ADR-012](../../cadena-farmacias-docs/docs/05-arquitectura/adr/ADR-012-clientes-consentimiento-y-alcance.md). Siguen `POR_VALIDAR` la base jurídica, la retención y el umbral de identificación.
**Plan:** [módulo Clientes](../plans/2026-10-30-modulo-clientes.md)

Clasificación de evidencia según el README de `cadena-farmacias-docs`: `NORM` es obligación normativa, `DOM/TEC` es decisión de diseño y `POR_VALIDAR` requiere confirmación externa. Las propuestas de abajo son `DOM/TEC` y no deben presentarse como obligación `NORM`.

## Fuentes

- `RF-POS-006` Gestionar cliente de venta: "identificar cliente cuando sea requerido o voluntario conforme a privacidad".
- `RN-SEC-001` (NORM/SEC): el tratamiento de clientes identificados se sujeta a la Ley 29733 y su Reglamento.
- `RN-SEC-003` (NORM/SEC): el consentimiento de marketing no se infiere de una compra ni de una receta; se modela por separado.
- `RN-FVG-002` (NORM/SEC): datos de paciente protegidos, con acceso por finalidad.
- `04-dominio/10-decisiones-abiertas.md` §12 (privacidad y fidelización) y §13 (cliente distinto de paciente).
- `06-datos/04-clasificacion-retencion-datos.md`: "cliente identificado, dirección delivery, contacto" como datos personales con finalidad y minimización.

## D1. Cliente y paciente

- **Propuesta:** son entidades distintas, como indica §13. `ClienteRetail` no exige historia clínica. `reporte_seguridad.paciente_cliente_id` seguirá apuntando al cliente solo como referencia; el módulo no modela paciente.
- **Alternativa:** unificar en una persona. Se descarta por finalidades distintas.
- **Decide el usuario:** confirmar la separación.

## D2. Alcance de la v1

- **Dentro:** registrar, editar, bloquear y desbloquear cliente; búsqueda por documento o nombre; consentimiento por finalidad; selector en el POS (fase 4).
- **Fuera:** fidelización, crédito por cliente, delivery y segmentación. Los docs ya los dejan fuera (`06-datos/01-modelo-conceptual.md:82`, `08-consolidacion-v1-v2-campos.md:62`).
- **Decide el usuario:** confirmar o ampliar.

## D3. Consentimiento (Ley 29733)

- **Propuesta de finalidades (catálogo):** `IDENTIFICACION_VENTA`, `MARKETING`, `FIDELIZACION`. Marketing y fidelización se consienten por separado (RN-SEC-003); identificación para la venta no se condiciona a marketing.
- **Propuesta de modelo:** catálogo de finalidades; por cada consentimiento, versión del texto, canal, fecha, quién lo registró y evidencia; revocación como nuevo estado sin borrar el histórico (reemplaza el `ON DELETE CASCADE` actual).
- **Base jurídica por finalidad:** `POR_VALIDAR` con asesoría legal. No se asume en el diseño.
- **Retención y anonimización:** plazos `POR_VALIDAR`.
- **Decide el usuario:** confirmar las finalidades y quién valida la base jurídica y la retención.

## D4. Cliente sin identificar y documento

- **Propuesta:** `tipo_cliente = SIN_IDENTIFICAR` implica documento nulo. La venta anónima sigue permitida por defecto.
- **Umbral de identificación obligatoria por monto:** `POR_VALIDAR` contra la normativa tributaria vigente. No se codifica ningún valor hasta confirmarlo.
- **Documento:** validar contra `tipo_documento_identidad` con FK y longitud por tipo (DNI de 8 dígitos, RUC de 11). Hoy `cliente.tipo_documento` es `VARCHAR(2)` y el catálogo quedó en `VARCHAR(1)` tras V024.
- **Decide el usuario:** confirmar y definir quién valida el umbral.

## D5. Datos sensibles y aislamiento

- **Propuesta:** clasificar `fecha_nacimiento` y `email` como datos personales con minimización. Hacer `fecha_nacimiento` opcional.
- **Cifrado y RLS por tenant:** hoy el aislamiento depende de la aplicación y de las FKs compuestas con `tenant_id`. Decidir si se añade RLS en un ADR aparte.
- **Decide el usuario:** si entra en la v1 o queda para después.

## D6. Permisos

- **Propuesta:** `clientes.clientes.consultar`, `.registrar`, `.editar`, `.bloquear` y `clientes.consentimientos.gestionar`, concedidos al rol ADMIN de FARMALAB (como `V025`). La sección del frontend pasa de `ventas.ventas.` a `clientes.clientes.consultar`. El POS necesitará consultar clientes sin gestionarlos.
- **Decide el usuario:** confirmar los nombres y si el cajero puede registrar cliente.

## Documentos a actualizar al cerrar la fase

1. `ADR-012` en `05-arquitectura/adr/` (estado propuesto hasta aprobación).
2. Casos de uso y criterios de aceptación de `RF-POS-006`. Los RF nuevos se numeran solo tras la aprobación, para no tocar los recuentos y la matriz de trazabilidad antes de tiempo.
3. `04-dominio/10-decisiones-abiertas.md` §12 y §13: marcar como decididas.

## Preguntas abiertas

- ¿Quién valida la base jurídica y la retención del consentimiento?
- ¿Se necesita dirección de delivery en la v1? Propuesta: no.
- ¿Quién puede bloquear a un cliente y por qué motivos?
