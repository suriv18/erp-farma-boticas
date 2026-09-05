# BPM-FAR-011 — TO-BE Farmacovigilancia y Tecnovigilancia

## 1. Objetivo

Capturar sospechas de reacciones adversas a medicamentos (SRAM) e incidentes adversos de dispositivos desde la operación de la cadena, preservando confidencialidad y permitiendo su derivación al sistema oficial aplicable.

## 2. Alcance inicial

El portal DIGEMID mantiene formatos para profesionales de salud de establecimientos públicos/privados y establecimientos farmacéuticos (farmacias, boticas y droguerías), además de mecanismos electrónicos como NotiMED/NotiVAC.

Por tanto, la cadena debe al menos poder:

- registrar un reporte recibido en local/canal;
- asociarlo a producto cuando sea posible;
- identificar gravedad y datos mínimos conforme al formulario/proceso aplicable;
- asignarlo al responsable farmacéutico;
- proteger los datos del paciente/reportante;
- dar seguimiento a información faltante;
- registrar su notificación externa o cierre.

## 3. Flujo

```text
Paciente/cliente/profesional comunica evento
        ↓
Registrar reporte inicial
        ↓
Clasificar: medicamento / dispositivo
        ↓
Validar datos mínimos
        ↓
Evaluar gravedad/prioridad
        ↓
Asignar QF / responsable
        ↓
Completar información
        ↓
Notificar por canal oficial cuando corresponda
        ↓
Registrar identificador/evidencia de notificación
        ↓
Seguimiento
        ↓
Cierre
```

## 4. Separación de dominio

Farmacovigilancia **no debe depender de que exista una venta POS**. Un evento puede ser reportado aunque el producto haya sido adquirido en otro establecimiento.

```text
ReporteFarmacovigilancia
    producto?       sí/no
    venta_origen?   opcional
    paciente?       protegido
    reportante?     protegido
```

## 5. Plazos

El sistema deberá soportar plazos regulatorios parametrizados por tipo/gravedad y fuente normativa vigente. El D.S. 016-2011 contiene plazos para centros de referencia respecto de eventos graves y leves/moderados; antes de aplicarlos de forma idéntica a cada farmacia privada se validará el rol exacto que corresponde a la cadena dentro del Sistema Peruano de Farmacovigilancia.

Por ello no se hardcodeará una única regla global del tipo `grave = 24h` sin resolver el sujeto obligado específico.

## 6. Reglas candidatas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-FVG-001 | Un reporte de farmacovigilancia puede existir sin venta POS asociada. | NORM/DOM |
| RC-FVG-002 | Datos clínicos/personales del reporte tienen acceso restringido. | NORM/SEC |
| RC-FVG-003 | Todo envío oficial conserva fecha, canal, responsable y evidencia/identificador cuando exista. | DOM/AUD |
| RC-FVG-004 | Los plazos se parametrizan por sujeto, gravedad y norma vigente. | DOM/NORM |
| RC-FVG-005 | Una actualización no elimina el reporte original; conserva trazabilidad. | DOM/AUD |

## 7. Buenas Prácticas de Farmacovigilancia

DIGEMID mantiene el Manual de Buenas Prácticas de Farmacovigilancia aprobado por RM 1053-2020/MINSA y modificado por RM 049-2025/MINSA. Su aplicabilidad concreta depende del tipo de establecimiento/empresa y responsabilidades regulatorias que asumamos; se utilizará como fuente de requisitos en la siguiente fase.

## 8. Fuentes

- DIGEMID — Farmacovigilancia y Tecnovigilancia: https://www.digemid.minsa.gob.pe/webDigemid/farmacovigilancia-y-tecnovigilancia/
- DIGEMID — Formatos para profesionales de salud: https://www.digemid.minsa.gob.pe/webDigemid/formatos-profesionales-salud/
- DIGEMID — NotiMED/NotiVAC: https://www.digemid.minsa.gob.pe/webDigemid/formulario-electronico/
- RM 1053-2020/MINSA: https://www.digemid.minsa.gob.pe/webDigemid/normas-legales/2020/resolucion-ministerial-n-1053-2020-minsa/
- RM 049-2025/MINSA: https://www.digemid.minsa.gob.pe/webDigemid/normas-legales/2025/resolucion-ministerial-n-049-2025-minsa/
