# BPM-FAR-009 — TO-BE Productos Fiscalizados / Controlados

## 1. Objetivo

Modelar los medicamentos que contienen estupefacientes, psicotrópicos, precursores u otras sustancias sujetas a fiscalización sanitaria como un flujo regulatorio especializado.

## 2. Fuente normativa principal

D.S. N.° 023-2001-SA — Reglamento de Estupefacientes, Psicotrópicos, Precursores y otras sustancias sujetas a fiscalización sanitaria.

DIGEMID mantiene además estándares/listados y formatos de fiscalización y comercialización actualizados.

## 3. Clasificación previa a toda operación

```text
Producto/SKU
    ↓
Sustancia(s)
    ↓
¿Fiscalizada?
    │
   Sí
    ↓
Lista / clasificación regulatoria vigente
    ↓
Política de adquisición + almacenamiento + receta + dispensación + registro + balance
```

La clasificación no debe quedar hardcodeada eternamente en Java; debe ser versionable y actualizable con fuente oficial.

## 4. Prescripción y dispensación

El D.S. 023-2001-SA distingue listas y tipos de receta. Para las Listas II A, III A, III B y III C contempla recetarios especiales; la receta especial tiene vigencia de tres días, debe retenerse al ser atendida y conservarse copia en el establecimiento por dos años. Para otras listas existen reglas diferentes, por lo que no se utilizará un único `requiere_receta_especial = true/false` como única lógica regulatoria.

Flujo:

```text
Escanear producto controlado
      ↓
Resolver regla vigente por clasificación
      ↓
Validar tipo de receta exigido
      ↓
Validar fecha / integridad / datos requeridos
      ↓
Validación por QF cuando corresponda
      ↓
Registrar paciente + prescriptor + receta + cantidad
      ↓
Registrar dispensación
      ↓
Retener/archivar receta cuando aplique
      ↓
Actualizar registro/libro de control
      ↓
Salida de stock por lote
      ↓
POS / comprobante
```

## 5. Libro/registro de control

El artículo 47 establece información a registrar para farmacias/boticas que dispensan medicamentos con estupefacientes, incluyendo proveedor, cantidad/concentración dispensada, prescriptor, paciente, número de receta especial y fecha de dispensación, según corresponda.

El sistema deberá soportar un **registro electrónico auditable** sin asumir que por existir en nuestro software sustituye automáticamente cualquier formalidad de visación/calificación requerida por DIGEMID.

## 6. Balance trimestral

El artículo 50 exige, para determinadas listas, balances trimestrales; el cierre es el último día útil del trimestre y la presentación se realiza dentro de los quince días calendario siguientes, con documentación adjunta según corresponda.

```text
Movimientos regulados
      ↓
Cierre regulatorio trimestral
      ↓
Existencia inicial
+ ingresos
- dispensaciones/consumos
± ajustes autorizados
= saldo
      ↓
Conciliar con existencia física y registro
      ↓
Generar balance
      ↓
Revisión QF
      ↓
Presentación / archivo
```

## 7. Reglas candidatas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-CTL-001 | La regla de receta depende de la clasificación regulatoria vigente. | NORM |
| RC-CTL-002 | Receta especial de listas aplicables no puede atenderse vencida según plazo normativo. | NORM |
| RC-CTL-003 | La receta especial atendida debe quedar retenida/archivada conforme a la norma aplicable. | NORM |
| RC-CTL-004 | La dispensación controlada registra paciente, prescriptor, receta y cantidad cuando aplique. | NORM |
| RC-CTL-005 | Cada movimiento controlado es trazable a establecimiento, producto/lote y actor. | NORM/DOM |
| RC-CTL-006 | El balance regulatorio se genera desde movimientos cerrados y conciliados; no desde valores editados manualmente sin evidencia. | DOM |
| RC-CTL-007 | La clasificación oficial debe poder versionarse. | DOM |

## 8. Fuentes

- D.S. N.° 023-2001-SA: https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf
- DIGEMID — Psicotrópicos y Estupefacientes: https://www.digemid.minsa.gob.pe/webDigemid/psicotropicos-y-estupefacientes/
- DIGEMID — Formatos y trámites empresa: https://www.digemid.minsa.gob.pe/webDigemid/formatos-y-tramites%20empresa/
- DIGEMID — Estándares de Productos Farmacéuticos: https://www.digemid.minsa.gob.pe/webDigemid/estandares-de-productos-farmaceuticos/
