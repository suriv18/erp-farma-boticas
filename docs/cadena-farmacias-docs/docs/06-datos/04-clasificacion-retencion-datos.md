# DAT-FAR-004 — Clasificación, Privacidad y Retención de Datos

## 1. Objetivo

Clasificar la información antes del modelo físico para aplicar minimización, acceso, cifrado, auditoría y retención apropiados. El tratamiento de datos personales deberá alinearse a la Ley N.° 29733 y su Reglamento aprobado por D.S. N.° 016-2024-JUS. [REF-46]

## 2. Clasificación propuesta

| Clase | Ejemplos | Tratamiento |
|---|---|---|
| Pública | catálogo comercial publicado, precios públicos | integridad/disponibilidad |
| Interna | costos, órdenes, stock, márgenes | acceso laboral por función |
| Confidencial | proveedores, finanzas, reglas comerciales | RBAC/ABAC + auditoría |
| Personal | cliente identificado, dirección delivery, contacto | finalidad y minimización |
| Personal sensible/contextual | receta, condición de salud inferible, farmacovigilancia | acceso reforzado y minimización |
| Seguridad | credenciales, tokens, secretos, claves | segregación/secret manager |
| Pago | datos de tarjeta si llegaran al entorno | reducir alcance PCI; no almacenar si no es necesario |

## 3. Retención

No se fijan plazos universales sin fuente. Cada dataset debe mantener:

- fundamento de retención;
- plazo mínimo/máximo;
- evento inicial del cómputo;
- tratamiento al vencer (eliminar, anonimizar, archivar);
- excepción por litigio/auditoría/obligación legal.

Los plazos regulatorios específicos de recetas/controlados se documentarán en sus RN y no se extrapolarán a todos los datos.

## 4. Minimización

- `ClienteRetail` no exige historia clínica.
- `Paciente` solo se identifica cuando el proceso lo requiere y existe base legítima.
- Farmacovigilancia no exige `VentaId`.
- POS no debe recibir datos personales que no necesita para cobrar/dispensar.
- analítica debe preferir datos agregados/seudonimizados cuando sea suficiente.

## 5. Datos de cliente retail (ADR-012)

Clasificación de las columnas de `sch_venta.cliente` y `sch_venta.cliente_consentimiento`, aplicada según [ADR-012](../05-arquitectura/adr/ADR-012-clientes-consentimiento-y-alcance.md). Origen `DOM/TEC`: la clasificación es una decisión de diseño, no una obligación normativa.

| Dato | Clase | Tratamiento |
|---|---|---|
| `tipo_documento`, `numero_documento` | Personal | Se exigen solo para identificar al cliente; la venta anónima no los requiere |
| `nombres`, `apellidos`, `razon_social` | Personal | Mínimos para identificar al cliente |
| `email`, `telefono` | Personal | Opcionales; usarlos para marketing exige el consentimiento de esa finalidad |
| `fecha_nacimiento` | Personal | Opcional; solo se registra si el proceso lo necesita |
| `motivo_bloqueo` | Interna | Visible solo para quien puede bloquear o consultar al cliente |
| Consentimiento (finalidad, versión del texto, fechas, evidencia) | Personal | Se conserva como prueba; no se borra con el cliente |

Pendiente `POR_VALIDAR`: plazos de retención y anonimización de estos datos, base jurídica de cada finalidad y decisión sobre cifrado y RLS por tenant (ADR aparte).
