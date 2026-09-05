# UX-FAR-007 — UX Offline, Sincronización y Conflictos

## Estados de conectividad

- `ONLINE`
- `DEGRADADO`
- `OFFLINE`
- `SINCRONIZANDO`
- `REQUIERE_ATENCION`

Estos estados son UX/operativos; el modelo técnico definitivo puede usar nombres diferentes.

## Regla principal

Una operación confirmada localmente **no debe inducir al usuario a repetirla** porque el servidor central no respondió.

Ejemplo:

```text
VENTA CONFIRMADA LOCALMENTE
Sincronización pendiente
ID local: X
```

no:

```text
ERROR DE RED. INTENTE NUEVAMENTE
```

si intentar nuevamente pudiera duplicar el negocio.

## Conflictos

Los conflictos que requieren decisión humana deben ir a una bandeja de excepciones con:

- operación local;
- regla central;
- diferencia;
- riesgo;
- acciones permitidas;
- actor que resolvió;
- auditoría.

El cajero no resolverá conflictos de stock/contabilidad que pertenecen a backoffice.
