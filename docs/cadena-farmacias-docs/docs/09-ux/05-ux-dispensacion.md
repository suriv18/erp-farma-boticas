# UX-FAR-005 — UX de Prescripción y Dispensación

## Objetivo

Separar la decisión farmacéutica del cobro, manteniendo continuidad del flujo de atención.

## Flujo

```text
Buscar/crear prescripción
      ↓
Datos mínimos y evidencia
      ↓
Productos prescritos
      ↓
Validaciones
      ↓
Competencia profesional
      ↓
Selección de SKU/lote vendible
      ↓
Confirmar dispensación
      ↓
Enviar referencia al POS
```

## Alertas

Clasificación UX candidata:

- `BLOQUEANTE`: venta prohibida/no válida.
- `REQUIERE_ACCION_QF`: necesita decisión profesional.
- `INFORMATIVA`: advertencia no bloqueante documentada.

No se usarán colores como único mecanismo de comunicación.

## Trazabilidad visible

La pantalla debe poder mostrar por qué un producto fue rechazado:

```text
No vendible
- lote vencido
- recall activo
- receta requerida
```

sin exponer reglas internas o datos personales innecesarios.
