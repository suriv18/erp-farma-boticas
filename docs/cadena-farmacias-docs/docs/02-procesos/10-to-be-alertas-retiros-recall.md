# BPM-FAR-010 — TO-BE Alertas, Inmovilizaciones y Retiros (Recall)

## 1. Objetivo

Permitir que una alerta sanitaria o decisión interna de calidad se convierta rápidamente en inmovilización y trazabilidad por producto, lote y establecimiento.

DIGEMID publica alertas que pueden ordenar o comunicar retiro del mercado de lotes específicos de productos farmacéuticos o dispositivos médicos. El sistema debe poder actuar a nivel de lote, no solo de producto.

## 2. Ingreso de alerta

```text
Alerta DIGEMID / proveedor / calidad interna
        ↓
Registrar fuente y documento
        ↓
Identificar producto(s)
        ↓
Identificar lote(s) afectados
        ↓
Clasificar acción
```

Acciones de dominio candidatas:

- `INFORMATIVA`;
- `BLOQUEO_VENTA`;
- `INMOVILIZACION`;
- `RETIRO`;
- `DESTRUCCION_PENDIENTE`;
- otras a validar.

Estos nombres son **DOM**, no una taxonomía normativa oficial cerrada.

## 3. Ejecución en cadena

```text
Alerta/lote afectado
       ↓
Consultar stock corporativo
       ↓
Bloquear lote en todos los canales
       ↓
Farmacia 001 → 4 unidades
Farmacia 002 → 0
CD           → 25 unidades
E-commerce   → quitar disponibilidad
       ↓
Tareas de localización/inmovilización
       ↓
Confirmación física por establecimiento
       ↓
Transferencia / devolución / destrucción según instrucción
       ↓
Cerrar recall con conciliación
```

## 4. Venta histórica y localización

Debe ser posible responder:

- ¿qué lotes siguen en stock?;
- ¿en qué locales?;
- ¿qué cantidades fueron inmovilizadas?;
- ¿qué unidades fueron transferidas o destruidas?;
- ¿hubo ventas del lote antes del bloqueo?;
- ¿qué canal las realizó?;
- si la política legal/privacidad lo permite, ¿qué clientes identificados podrían requerir comunicación?

No se asumirá que todo comprador debe ser identificado: eso depende del tipo de venta y de la base legal aplicable.

## 5. Reglas candidatas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-RCL-001 | Un bloqueo por lote impide nuevas salidas vendibles de ese lote. | DOM/SEGURIDAD_SANITARIA |
| RC-RCL-002 | El bloqueo debe propagarse a tienda, POS y canales digitales. | DOM |
| RC-RCL-003 | La evidencia de inmovilización se registra por establecimiento. | DOM/AUD |
| RC-RCL-004 | Cierre de recall exige conciliación de cantidades afectadas y destino. | DOM |
| RC-RCL-005 | La alerta conserva fuente, fecha, documento y alcance. | DOM/AUD |

## 6. Fuentes

- DIGEMID — Alertas: https://www.digemid.minsa.gob.pe/webDigemid/alertas-/
- Ejemplo de retiro de lote por control de calidad: https://www.digemid.minsa.gob.pe/webDigemid/alertas-modificaciones/2025/alerta-digemid-no-124-2025/
- DIGEMID — Productos robados: https://www.digemid.minsa.gob.pe/webDigemid/establecimientos/productos-robados/
