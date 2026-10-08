# Tipo de documento de identidad — SUNAT 06

## Implementación

Tabla global `sch_catalogo.tipo_documento_identidad`, sin `tenant_id`, con código
natural como clave primaria, sigla, denominación oficial, máximo y mínimo de
caracteres del número de documento y
estado activo. Disponible en la migración incremental
[V018](../migrations/V018__catalogo_tipo_documento_identidad_sunat.sql) y en el
[SQL original](../cadena_farmacias_postgresql18.sql).

Aplicar V018 una sola vez después de V017 mediante el mecanismo de migraciones
del proyecto. Las bases creadas con el original actualizado ya incluyen este
objeto: no ejecutar encima su migración equivalente.

## Fuente verificada

- [SUNAT: guías y manuales](https://cpe.sunat.gob.pe/guias-y-manuales).
- [Reglas de validación, versión 26.08.2026](https://cpe.sunat.gob.pe/sites/default/files/2026-08/Reglas%20de%20validaci%C3%B3n%20-%20actualizado%20al%2026.08.2026.xlsx).
- Consulta: 15/09/2026. Hoja **Catálogos**, celdas **A86:B98**, 13 registros.
- SHA-256 del XLSX: `cb5e871cfe3b81abea7faf25b156e5d35b21e8c4837979350919926612da73b6`.
- Se conserva la redacción oficial, incluida «Registro Unico de Contributentes»
  del código 6; se eliminan espacios finales. La fecha de versión identifica el
  archivo consultado, no la fecha de entrada en vigencia de cada código.
- El rango nombrado `Catalogo06` del XLSX termina en la fila 91; la extracción
  utiliza el bloque completo hasta la fila 98 para incluir B–H.

| Código | Denominación oficial |
|---|---|
| 0 | DOC.TRIB.NO.DOM.SIN.RUC |
| 1 | Documento Nacional de Identidad |
| 4 | Carnet de extranjería |
| 6 | Registro Unico de Contributentes |
| 7 | Pasaporte |
| A | Cédula Diplomática de identidad |
| B | DOC.IDENT.PAIS.RESIDENCIA-NO.D |
| C | Tax Identification Number - TIN – Doc Trib PP.NN |
| D | Identification Number - IN – Doc Trib PP. JJ |
| E | TAM- Tarjeta Andina de Migración |
| F | Permiso Temporal de Permanencia - PTP |
| G | Salvoconducto |
| H | Carné Permiso Temp.Perman. - CPP |

## Campos de presentación y longitud

Orden de columnas: `codigo`, `sigla`, `denominacion`, `max`, `min`,
`es_activo`.

Las siglas son etiquetas locales de presentación, no una columna del catálogo
oficial SUNAT. Se mantienen la clave natural y el estado de los catálogos globales
existentes. No se agregan identidad, UUID ni tenant a este catálogo de referencia.

| Código | Sigla local | max | min |
|---|---|---|---|
| 0 | DTSR | NULL | NULL |
| 1 | DNI | 8 | 8 |
| 4 | CE | NULL | NULL |
| 6 | RUC | 11 | 11 |
| 7 | PAS | NULL | NULL |
| A | CDI | NULL | NULL |
| B | DIPR | NULL | NULL |
| C | TIN | NULL | NULL |
| D | IN | NULL | NULL |
| E | TAM | NULL | NULL |
| F | PTP | NULL | NULL |
| G | SC | NULL | NULL |
| H | CPP | NULL | NULL |

Los límites son `SMALLINT` opcionales y positivos; cuando ambos están definidos,
`min <= max`. DNI tiene longitud fija 8 y RUC 11, como indica el
[anexo I, campo 10, de SUNAT](https://www.sunat.gob.pe/legislacion/superin/2026/anexo-000049-2026.pdf).
Para los demás documentos, `NULL` significa que no se configura una longitud
general. No autoriza números vacíos ni elimina las validaciones específicas del
comprobante. Los límites del anexo para documentos extranjeros corresponden a
ese procedimiento y no se trasladan como restricciones universales.

Estos campos almacenan metadatos: la aplicación debe usarlos para validar los
números. No validan automáticamente las columnas de otras tablas.

## Uso e integración

```sql
SELECT codigo, sigla, denominacion, max, min
FROM sch_catalogo.tipo_documento_identidad
WHERE es_activo = '1'
ORDER BY codigo;
```

- Los códigos son texto: `1` y `6`, sin convertirlos a `01` o `06`.
- El código `0` no debe asignarse automáticamente a un cliente sin identificar.
- La aplicación debe validar el tipo y número según el comprobante y la
  operación. No todos los códigos del catálogo se aceptan en todos los CPE.
- La tabla no impone una longitud universal al número de documento extranjero.
- Este cambio crea y carga el catálogo. Las columnas existentes `tipo_documento`
  de usuario, profesional farmacéutico, proveedor y cliente, así como
  `paciente_tipo_documento` de prescripción y `cliente_tipo_documento` de CPE,
  aún no tienen una FK hacia él. Su integración requiere revisar los valores
  existentes y agregar las relaciones en una migración posterior.
- `factura_proveedor.tipo_documento_sunat` identifica un tipo de comprobante;
  corresponde a otro catálogo y no debe apuntar a esta tabla.
- Para futuras actualizaciones, crear otra migración con referencias documentales
  explícitas. No editar V018 una vez aplicada ni eliminar códigos referenciados.

## Validación

Se compararon los 13 pares código/denominación con el XLSX oficial y se verificó
que la definición y carga del SQL original coincidan con V018. La ejecución en
una instancia PostgreSQL 18 queda pendiente; el cambio no aplica migraciones a
una base de datos en funcionamiento.
