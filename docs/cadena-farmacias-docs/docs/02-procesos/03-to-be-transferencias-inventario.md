# BPM-FAR-003 — TO-BE Transferencias e Inventario Distribuido

## 1. Objetivo

Mover productos entre almacenes/tiendas preservando cantidades, lotes, vencimientos y trazabilidad.

## 2. Flujo

```text
Origen detecta excedente / destino requiere stock
                    ↓
             Solicitud transferencia
                    ↓
                 Aprobación
                    ↓
              Reserva en origen
                    ↓
             Picking por lote
                    ↓
                 Despacho
                    ↓
            STOCK EN TRÁNSITO
                    ↓
               Recepción destino
                    ↓
          Comparación enviado/recibido
                    ↓
           ¿Existen diferencias?
             ┌──────┴───────┐
             │              │
            No             Sí
             │              ↓
             │        Incidencia/ajuste
             ↓
       Stock destino disponible
             ↓
              Cierre
```

## 3. Invariantes candidatas

- No transferir stock inexistente.
- No transferir stock bloqueado salvo proceso autorizado específico.
- El lote despachado debe ser el lote recibido o existir diferencia documentada.
- Despacho disminuye disponibilidad de origen y aumenta stock en tránsito; no debe aumentar inmediatamente stock disponible del destino.
- La recepción debe ser explícita.
- Diferencias necesitan motivo y actor.

## 4. Selección de lote

La selección podrá utilizar FEFO cuando la política del producto/almacén lo establezca.

```text
Lote A vence 10/2026
Lote B vence 04/2027
Lote C vence 09/2027
          ↓
Política FEFO
          ↓
Lote A primero
```

FEFO se mantiene como `MKT/DOM` hasta fijar las políticas del negocio y normas específicas aplicables.
