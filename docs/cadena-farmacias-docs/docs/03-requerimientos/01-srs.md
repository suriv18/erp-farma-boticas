# SRS-FAR-001 — Especificación de Requerimientos del Sistema Integral para Cadena de Farmacias

**Versión:** 0.1  
**Estado:** Borrador funcional trazable  
**Fecha de corte de fuentes:** 2026-08-30

## 1. Propósito

Definir los requisitos del **Sistema Integral de Gestión para Cadena de Farmacias**, separando las capacidades de ERP corporativo, Retail/POS, Supply Chain/WMS, dominio farmacéutico y capacidades transversales.

Este SRS se deriva de los documentos de negocio y procesos `BPM-FAR-001` a `BPM-FAR-013`. No constituye todavía un diseño de base de datos ni una selección definitiva de arquitectura tecnológica.

## 2. Principio de trazabilidad

Todo requisito deberá poder relacionarse con una o más de estas fuentes:

- `NORM`: norma o fuente sanitaria oficial;
- `FISCAL`: SUNAT/comprobantes/tributación;
- `FUNC`: proceso o necesidad funcional validada;
- `ERP`: práctica ERP/retail contrastada con productos actuales;
- `DOM`: decisión de dominio propuesta;
- `SEC`: privacidad/seguridad;
- `AUD`: auditoría/trazabilidad;
- `CFG`: política parametrizable de la cadena;
- `POR_VALIDAR`: decisión pendiente.

No se presentará como obligación legal una característica tomada solamente de SAP, Dynamics, LS Retail, Oracle u Odoo.

## 3. Alcance funcional

```text
Cadena de Farmacias
├── Organización / establecimientos
├── Catálogo farmacéutico
├── Proveedores y compras
├── Inventario / lotes / vencimientos
├── Transferencias / reposición
├── Precios / promociones
├── Retail / POS / caja / CPE
├── Prescripción / dispensación
├── Productos fiscalizados
├── Devoluciones
├── Alertas / recall
├── Farmacovigilancia / tecnovigilancia
├── ERP financiero / cierre
├── Observatorio / reportes regulatorios
├── Seguridad / privacidad
├── Auditoría
├── Integraciones
└── Reportes / analítica
```

## 4. Actores principales

- Administrador de plataforma/cadena.
- Administrador corporativo.
- Jefe de compras.
- Comprador.
- Proveedor (actor externo/integración).
- Responsable de almacén/CD.
- Responsable de inventario.
- Jefe/encargado de establecimiento.
- Cajero/operador POS.
- Químico Farmacéutico / Director Técnico.
- Químico Farmacéutico asistente.
- Personal técnico en farmacia dentro de sus competencias permitidas.
- Responsable de farmacovigilancia/tecnovigilancia.
- Finanzas / Contabilidad / Tesorería.
- Auditor/Compliance.
- Cliente/paciente/usuario.
- Sistemas externos: SUNAT, ERP, adquirentes, e-commerce, delivery y fuentes regulatorias cuando exista integración.

## 5. Restricciones normativas verificadas relevantes

1. El D.S. N.° 015-2025-SA modificó el artículo 43 del Reglamento de Establecimientos Farmacéuticos: el personal técnico en farmacia de farmacias y boticas está impedido de realizar actos correspondientes a la dispensación de productos farmacéuticos de venta bajo receta médica o de ofrecer alternativas al medicamento prescrito; además, el Director Técnico supervisa al personal.
2. El D.S. N.° 023-2001-SA define reglas específicas para productos fiscalizados. Para medicamentos de las Listas II A, III A, III B y III C se utiliza receta especial y el reglamento establece vigencia de tres días; las recetas atendidas tienen requisitos de retención/archivo y control según los artículos aplicables.
3. SUNAT distingue factura, boleta y notas electrónicas y mantiene reglas de emisión, relación con comprobantes previos y envío del SEE del contribuyente.
4. DIGEMID publica indicadores de precios con datos informados mensualmente por farmacias y boticas privadas al Observatorio Peruano de Productos Farmacéuticos.
5. La Ley N.° 29733 y su Reglamento vigente desde el 31/03/2025 regulan el tratamiento de datos personales; esto afecta clientes identificados, recetas, farmacovigilancia, fidelización y marketing.

## 6. Exclusiones/decisiones abiertas

Por ahora no se considera decidido:

- ERP propio vs. integración con ERP externo;
- operación POS offline y mecanismo de sincronización;
- motor contable completo vs. interface a ERP;
- política de costeo (promedio, FIFO u otra);
- política FEFO por categoría;
- alcance de e-commerce/delivery en el MVP;
- mecanismo técnico exacto del Observatorio;
- proveedor de pagos/adquirente;
- tecnología de autenticación;
- base de datos/arquitectura física definitiva.

## 7. Criterio de calidad de un RF

Cada RF debe ser:

- identificable;
- verificable;
- trazable a proceso/regla;
- independiente de una pantalla concreta cuando sea posible;
- explícito respecto a actor/resultado;
- no ambiguo respecto a obligaciones normativas;
- compatible con auditoría e histórico.

## 8. Documentos asociados

- [Requerimientos funcionales](02-requerimientos-funcionales.md)
- [Requerimientos no funcionales](03-requerimientos-no-funcionales.md)
- [Reglas de negocio](04-reglas-negocio.md)
- [Matriz de trazabilidad](07-matriz-trazabilidad.md)
- [Procesos TO-BE](../02-procesos/01-mapa-procesos.md)
- [Referencias](../99-referencias.md)
