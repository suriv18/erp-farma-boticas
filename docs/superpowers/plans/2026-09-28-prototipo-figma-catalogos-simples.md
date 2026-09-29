# Prototipo Figma de catálogos maestros simples — Plan de trabajo

> **Para quien ejecute este plan:** este NO es un plan de código. Es un plan de trabajo de diseño en Figma usando las herramientas MCP `mcp__claude_ai_Figma__*`. No hay tests unitarios ni TDD — la verificación de cada tarea es visual, vía `get_screenshot`, comparando contra los criterios de aceptación listados en cada tarea. Antes de cualquier llamada a `use_figma`, se debe invocar la skill `figma-use` (y `figma-generate-library` para las tareas de fundaciones de estilos/componentes) — es un prerequisito obligatorio, no opcional.

**Goal:** Construir en el archivo de Figma `dAGg5r6W2taj38y4MMH3xH` un componente maestro reutilizable "Catálogo simple" y sus 8 instancias (rubro comercial, condición de venta, forma farmacéutica, vía de administración, unidad de medida, clasificación controlada, tipo de documento de identidad, principio activo), siguiendo el spec `docs/superpowers/specs/2026-09-28-prototipo-figma-catalogos-simples-design.md`.

**Architecture:** Un componente maestro con las variantes de estado (con datos / vacío / cargando / error) para el bloque de tabla, más 2 componentes de modal (crear/editar) reutilizados por instancia. Cada uno de los 8 catálogos es una página nueva del archivo de Figma que instancia el componente maestro y sobreescribe el contenido (columnas, campos de formulario, datos de ejemplo).

**Tech Stack:** Figma (vía MCP `mcp__claude_ai_Figma__*` + skills `figma-use`, `figma-generate-library`, `figma-generate-design`), archivo `dAGg5r6W2taj38y4MMH3xH`.

## Global Constraints

- Paleta de color: reutilizar la escala `success` ya usada en el login — `#ecfdf5, #d1fae5, #a7f3d0, #6ee7b7, #34d399, #10b981, #059669, #047857, #065f46, #064e3b, #022c22` (50→950) — más blanco para acentos. No introducir un tercer color base.
- No tocar código de `frontend/` ni `service-botica/` en este plan — es solo diseño en Figma.
- No modificar la página "Login – Botica" existente ni su contenido.
- Los datos de ejemplo deben ser reales/plausibles del dominio farmacéutico peruano, nunca "Lorem ipsum" ni "Item 1/2/3".
- Antes de cualquier `use_figma`, invocar la skill `figma-use`; antes de crear componentes/variantes con fundaciones de estilo, invocar también `figma-generate-library`.

---

## Task 1: Fundaciones de estilo — paleta y estilos de texto en Figma

**Herramientas:** `figma-use` skill + `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_variable_defs`, `mcp__claude_ai_Figma__get_screenshot`

**Consumido por:** Task 2 (componente maestro) y todas las instancias (Task 4-11) referencian estas variables/estilos por nombre.

**Produce:**
- Variables de color de Figma bajo la colección `success` con los 11 pasos (`success/50` … `success/950`) con los valores hex del listado de Global Constraints.
- Estilos de texto: `Heading/Page` (título de página), `Body/Description` (descripción bajo el título), `Label/Field` (labels de formulario), `Body/Table` (celdas de tabla), reutilizando la tipografía ya usada en el frame "Login – Botica" (mismo font-family/weight que el resto del archivo).

- [ ] **Paso 1: Cargar la skill `figma-use`**

Invocar la skill antes de cualquier llamada a `use_figma`, como indica su prerequisito obligatorio.

- [ ] **Paso 2: Inspeccionar el frame de Login para reutilizar tipografía**

```
mcp__claude_ai_Figma__get_metadata(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "2:2")
```
Identificar los nodos de texto del título "Bienvenido de nuevo" y del párrafo de descripción para extraer su `fontFamily`/`fontWeight`/`fontSize` reales (no asumir valores).

- [ ] **Paso 3: Crear la colección de variables de color `success` vía `use_figma`**

Usar `use_figma` (siguiendo el patrón que indique la skill `figma-use` para crear variables de colección `COLOR`) para crear 11 variables `success/50` … `success/950` con los valores hex de Global Constraints, en una colección nueva llamada "Catálogos — Color".

- [ ] **Paso 4: Crear los 4 estilos de texto reutilizando la tipografía detectada en el paso 2**

`Heading/Page`, `Body/Description`, `Label/Field`, `Body/Table`.

- [ ] **Paso 5: Verificar visualmente**

```
mcp__claude_ai_Figma__get_screenshot(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "<node de la página de estilos>")
```
Criterio de aceptación: las 11 variables de color existen con los valores correctos (confirmar con `get_variable_defs`), los 4 estilos de texto existen y usan la misma familia tipográfica que el login.

---

## Task 2: Componente maestro "Catálogo simple" — estructura base y estado "con datos"

**Herramientas:** `figma-use` + `figma-generate-library` skills, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** variables `success/*` y estilos de texto de Task 1.

**Produce:** un componente de Figma llamado `Catálogo simple / Página` con estas capas nombradas exactamente así (los siguientes tasks de instancia dependen de estos nombres para hacer overrides):
- `Header` (contiene `Breadcrumb`, `Título`, `Descripción`, `BotónNuevo`)
- `Filtros` (contiene `CampoBúsqueda`, `SelectEstado`)
- `Tabla` (contiene `FilaEncabezado` con slots de columna, y `FilasDatos` como lista de instancias de fila)
- `Fila` (componente separado, con celdas: `CeldaCódigo`, `CeldaDenominación`, celdas específicas por catálogo como slots de texto genéricos `CeldaExtra1`, `CeldaExtra2`, `CeldaExtra3`, `CeldaEstado` con badge, `CeldaAcciones` con 2 íconos)

- [ ] **Paso 1: Cargar las skills `figma-use` y `figma-generate-library`**

- [ ] **Paso 2: Crear una página nueva en el archivo llamada "02 · Catálogo simple (maestro)"**

- [ ] **Paso 3: Construir el frame `Header` (1440px de ancho, replicando el patrón de `PageHeader` del frontend: breadcrumb + título + descripción + botón alineado a la derecha)**

Botón "+ Nuevo {Recurso}" con fondo `success/600`, texto blanco, siguiendo el mismo estilo del botón "Iniciar sesión" del login.

- [ ] **Paso 4: Construir el frame `Filtros` (campo de búsqueda con ícono de lupa + select de Estado con opciones Todos/Activo/Inactivo)**

- [ ] **Paso 5: Construir el componente `Fila` con sus celdas nombradas como se especifica arriba**

`CeldaEstado` usa un badge: fondo `success/100` + texto `success/800` para "Activo", fondo neutro + texto gris para "Inactivo". `CeldaAcciones` tiene 2 íconos (lápiz para editar, ícono de power para activar/desactivar).

- [ ] **Paso 6: Ensamblar `Tabla` con `FilaEncabezado` + 3 instancias de `Fila` con datos de ejemplo genéricos ("Código 001", "Denominación de ejemplo") solo para validar el layout — estos se sobreescriben en cada instancia real de Task 4-11**

- [ ] **Paso 7: Combinar `Header` + `Filtros` + `Tabla` en el componente `Catálogo simple / Página`**

- [ ] **Paso 8: Verificar visualmente**

```
mcp__claude_ai_Figma__get_screenshot(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "<node del componente>")
```
Criterio de aceptación: el layout coincide con el patrón de `SupportCatalogPage.tsx` (header con acción a la derecha, filtros debajo, tabla con columna Estado y Acciones al final), colores usan la paleta `success`, tipografía consistente con el login.

---

## Task 3: Estados adicionales de la tabla (vacío, cargando, error) y modales Crear/Editar

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** componente `Catálogo simple / Página` y componente `Fila` de Task 2.

**Produce:**
- Variante de componente `Tabla` con 4 estados: `Con datos`, `Vacío`, `Cargando`, `Error` (nombres de variante exactos, usados como propiedad `Estado` del componente).
- Componente `Modal / Crear` con: título "Nuevo {Recurso}", slot de formulario (`CamposFormulario` como frame vacío para overrides), botón "Crear" (fondo `success/600`).
- Componente `Modal / Editar` con: título "Editar {Recurso}", mismo slot `CamposFormulario`, botón "Guardar" (fondo `success/600`).
- Componente `Campo / Texto`, `Campo / Select`, `Campo / Checkbox`, `Campo / Numérico` — piezas de formulario reutilizables para poblar `CamposFormulario` en cada instancia.

- [ ] **Paso 1: Cargar la skill `figma-use`**

- [ ] **Paso 2: Crear la variante `Vacío` de `Tabla`: icono + texto "No se encontraron registros." centrado, sin filas**

- [ ] **Paso 3: Crear la variante `Cargando`: 5 filas skeleton (rectángulos grises con esquinas redondeadas simulando texto cargando)**

- [ ] **Paso 4: Crear la variante `Error`: icono de alerta + texto "No se pudo cargar el listado de {recurso}s." + botón "Reintentar"**

- [ ] **Paso 5: Construir los componentes `Campo / Texto`, `Campo / Select`, `Campo / Checkbox`, `Campo / Numérico`, con label arriba y el mismo estilo de borde/focus ring `success` usado en los inputs del login**

- [ ] **Paso 6: Construir `Modal / Crear` y `Modal / Editar` con overlay oscuro semitransparente detrás, tarjeta blanca centrada, título, slot `CamposFormulario`, botones de acción (Cancelar en gris + acción primaria en `success/600`)**

- [ ] **Paso 7: Verificar visualmente cada variante y modal**

```
mcp__claude_ai_Figma__get_screenshot(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "<node de cada variante/modal>")
```
Criterio de aceptación: las 4 variantes de `Tabla` son distinguibles entre sí y coherentes con los mensajes exactos usados en `SupportCatalogPage.tsx` (`"No se encontraron registros."`, `"No se pudo cargar el listado de ${resourceLabel}s."`); ambos modales usan la paleta `success` y los 4 tipos de campo son reutilizables.

- [ ] **Paso 8: Confirmar con el usuario que el componente maestro completo (Tasks 1-3) se ve correcto antes de replicarlo 8 veces**

Este es un punto de checkpoint explícito: construir el maestro está más concentrado en tiempo/tokens que las 8 instancias; conviene que el usuario apruebe el maestro antes de la réplica masiva.

---

## Task 4: Instancia — Rubro comercial

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** componente `Catálogo simple / Página`, `Modal / Crear`, `Modal / Editar`, `Campo / Texto`, `Campo / Checkbox`, `Campo / Numérico` de Tasks 1-3.

**Produce:** página de Figma "Rubro comercial" con 1 frame de listado + 2 frames de modal (crear/editar), instanciando el maestro con:
- Breadcrumb: "Catálogo / Rubros comerciales"; Título: "Rubros comerciales"; Descripción: "Administra los rubros comerciales del catálogo."
- Columnas de tabla: Código, Denominación, Es farmacéutico (sí/no), Orden, Estado, Acciones
- Campos de formulario: Código (texto), Denominación (texto), Es farmacéutico (checkbox), Orden (numérico)
- 5 filas de datos de ejemplo reales: `FARMA` (Farmacéutico, farmacéutico=sí, orden 1), `BEBE` (Bebé y cuidado infantil, no, orden 2), `COSMETICA` (Cosmética y cuidado personal, no, orden 3), `CONVENIENCIA` (Conveniencia y snacks, no, orden 4), `CUIDADO_PERSONAL` (Cuidado personal e higiene, no, orden 5)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Rubro comercial", instanciar `Catálogo simple / Página` con el estado `Con datos` de la tabla**
- [ ] **Paso 3: Sobreescribir textos de `Header` (breadcrumb/título/descripción) y columnas de `FilaEncabezado`**
- [ ] **Paso 4: Poblar 5 filas con los datos de ejemplo listados arriba**
- [ ] **Paso 5: Instanciar `Modal / Crear` y `Modal / Editar` con los 4 campos listados arriba dentro de `CamposFormulario`, editar precargado con los datos de "FARMA"**
- [ ] **Paso 6: Verificar visualmente**

```
mcp__claude_ai_Figma__get_screenshot(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "<node de la página Rubro comercial>")
```
Criterio de aceptación: coincide con la fila "Rubro comercial" de la tabla de mapeo del spec (sección "Estructura en Figma"), 5 filas de datos reales, ambos modales completos.

---

## Task 5: Instancia — Condición de venta

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** mismos componentes que Task 4.

**Produce:** página "Condición de venta" con:
- Breadcrumb: "Catálogo / Condiciones de venta"; Título: "Condiciones de venta"; Descripción: "Administra las condiciones de venta del catálogo."
- Columnas: Código, Denominación, Requiere receta (sí/no), Requiere retención (sí/no), Fuente, Estado, Acciones
- Campos de formulario: Código (texto), Denominación (texto), Requiere receta (checkbox), Requiere retención (checkbox), Fuente (texto)
- 5 filas de ejemplo: `VENTA_LIBRE` (Venta libre, no requiere receta, no retiene, fuente DIGEMID), `RECETA_SIMPLE` (Venta con receta médica, sí, no, DIGEMID), `RECETA_RETENIDA` (Venta con receta retenida, sí, sí, DIGEMID), `RECETA_ESPECIAL` (Venta con receta especial controlada, sí, sí, DIGEMID), `HOSPITALARIO` (Uso exclusivo hospitalario, sí, no, DIGEMID)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Condición de venta", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 5 campos, editar precargado con "VENTA_LIBRE"**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Condición de venta" del spec**

---

## Task 6: Instancia — Forma farmacéutica

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Forma farmacéutica" con:
- Breadcrumb: "Catálogo / Formas farmacéuticas"; Título: "Formas farmacéuticas"; Descripción: "Administra las formas farmacéuticas del catálogo."
- Columnas: Código, Denominación, Fuente, Estado, Acciones
- Campos: Código (texto), Denominación (texto), Fuente (texto)
- 5 filas: `TAB` (Tableta, DIGEMID), `JAR` (Jarabe, DIGEMID), `CAP` (Cápsula, DIGEMID), `SUSP` (Suspensión oral, DIGEMID), `CREM` (Crema tópica, DIGEMID)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Forma farmacéutica", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 3 campos, editar precargado con "TAB"**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Forma farmacéutica" del spec**

---

## Task 7: Instancia — Vía de administración

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Vía de administración" con:
- Breadcrumb: "Catálogo / Vías de administración"; Título: "Vías de administración"; Descripción: "Administra las vías de administración del catálogo."
- Columnas: Código, Denominación, Estado, Acciones (sin columnas extra, según el spec)
- Campos: Código (texto), Denominación (texto)
- 5 filas: `ORAL` (Oral), `TOPICA` (Tópica), `IM` (Intramuscular), `IV` (Intravenosa), `OFTALMICA` (Oftálmica)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Vía de administración", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas (sin celdas extra)**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 2 campos, editar precargado con "ORAL"**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Vía de administración" del spec**

---

## Task 8: Instancia — Unidad de medida

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Unidad de medida" con:
- Breadcrumb: "Catálogo / Unidades de medida"; Título: "Unidades de medida"; Descripción: "Administra las unidades de medida del catálogo."
- Columnas: Código, Denominación, Símbolo, Permite decimal (sí/no), Estado, Acciones
- Campos: Código (texto), Símbolo (texto), Permite decimal (checkbox) — nota: el spec no lista "Denominación" como campo propio de este catálogo, se usa Código+Símbolo como identificación
- 5 filas: `NIU` (Unidad, "UND", no), `KGM` (Kilogramo, "KG", sí), `LTR` (Litro, "L", sí), `CJA` (Caja, "CJA", no), `FRA` (Frasco, "FRA", no)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Unidad de medida", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 3 campos, editar precargado con "NIU"**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Unidad de medida" del spec**

---

## Task 9: Instancia — Clasificación controlada

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Clasificación controlada" con:
- Breadcrumb: "Catálogo / Clasificaciones controladas"; Título: "Clasificaciones controladas"; Descripción: "Administra las clasificaciones controladas del catálogo."
- Columnas: Código, Denominación, Requiere receta especial (sí/no), Retiene receta (sí/no), Vigencia receta (días), Estado, Acciones
- Campos: Código (texto), Denominación (texto), Requiere receta especial (checkbox), Retiene receta (checkbox), Vigencia receta en días (numérico)
- 5 filas: `PSICOTROPICO_I` (Psicotrópico Lista I, sí, sí, 30 días), `PSICOTROPICO_II` (Psicotrópico Lista II, sí, sí, 30 días), `ESTUPEFACIENTE` (Estupefaciente, sí, sí, 15 días), `PRECURSOR` (Precursor de uso restringido, sí, no, 30 días), `ANTIBIOTICO_CONTROLADO` (Antibiótico de control especial, sí, no, 30 días)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Clasificación controlada", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 5 campos, editar precargado con "PSICOTROPICO_I"**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Clasificación controlada" del spec**

---

## Task 10: Instancia — Tipo de documento de identidad

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Tipo de documento de identidad" con:
- Breadcrumb: "Catálogo / Tipos de documento de identidad"; Título: "Tipos de documento de identidad"; Descripción: "Administra el catálogo SUNAT de tipos de documento de identidad."
- Columnas: Código, Denominación, Sigla, Longitud mín/máx, Estado, Acciones
- Campos: Código (texto), Sigla (texto), Denominación (texto), Longitud mínima (numérico), Longitud máxima (numérico)
- 5 filas: `1` (DNI, "DNI", 8, 8), `4` (Carné de extranjería, "CE", 9, 12), `6` (RUC, "RUC", 11, 11), `7` (Pasaporte, "PAS", 9, 12), `A` (Cédula diplomática de identidad, "CDI", 9, 12)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Tipo de documento de identidad", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 5 campos, editar precargado con "1" (DNI)**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Tipo de documento de identidad" del spec**

---

## Task 11: Instancia — Principio activo

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Principio activo" con:
- Breadcrumb: "Catálogo / Principios activos"; Título: "Principios activos"; Descripción: "Administra los principios activos (DCI) del catálogo."
- Columnas: Denominación, Nombre normalizado, Fuente, Estado, Acciones (sin columna "Código" — el spec indica que este catálogo se identifica por denominación, no por código corto)
- Campos: Denominación (texto), Nombre normalizado (texto), Fuente (texto)
- 5 filas: (Paracetamol, PARACETAMOL, DIGEMID), (Ibuprofeno, IBUPROFENO, DIGEMID), (Amoxicilina, AMOXICILINA, DIGEMID), (Loratadina, LORATADINA, DIGEMID), (Omeprazol, OMEPRAZOL, DIGEMID)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Principio activo", instanciar el maestro (usar variante de `Fila` sin `CeldaCódigo` o dejarla vacía si el componente no soporta ocultar celdas — decidir en el momento según lo que permita la instancia)**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 3 campos, editar precargado con "Paracetamol"**
- [ ] **Paso 6: Verificar visualmente con `get_screenshot`, comparar contra la fila "Principio activo" del spec**

---

## Task 12: Revisión final y cierre de la fase

**Herramientas:** `mcp__claude_ai_Figma__get_metadata`, `mcp__claude_ai_Figma__get_screenshot`

- [ ] **Paso 1: Listar todas las páginas del archivo para confirmar que existen las 9 nuevas (1 maestro + 8 instancias) además de "Login – Botica"**

```
mcp__claude_ai_Figma__get_metadata(fileKey: "dAGg5r6W2taj38y4MMH3xH")
```

- [ ] **Paso 2: Capturar screenshot de cada una de las 8 páginas de instancia para armar un resumen visual final**

- [ ] **Paso 3: Presentar el resumen al usuario con el link del archivo de Figma y pedir su revisión/aprobación antes de dar la Fase 1 por cerrada**

- [ ] **Paso 4: Si el usuario aprueba, actualizar el spec (`docs/superpowers/specs/2026-09-28-prototipo-figma-catalogos-simples-design.md`) cambiando `Estado: propuesto` a `Estado: implementado` y hacer commit**

```bash
git add docs/superpowers/specs/2026-09-28-prototipo-figma-catalogos-simples-design.md
git commit -m "docs(prototipo): marcar como implementado el spec de catalogos simples en Figma"
```

---

## Self-Review

**Cobertura del spec:** los 8 catálogos del spec tienen su Task dedicada (4-11), el componente maestro con sus 4 estados de tabla y 2 modales está en Tasks 2-3, la paleta de color y estilos de texto están en Task 1, el checkpoint de aprobación del maestro está en Task 3 paso 8, y el cierre con actualización del spec está en Task 12.

**Placeholders:** ningún paso usa "TBD"/"agregar validación genérica"/"similar a la tarea anterior sin detalle" — cada instancia (Tasks 4-11) repite explícitamente sus propios textos, columnas, campos y datos de ejemplo completos, ya que quien ejecute cada tarea puede no tener el contexto de las tareas anteriores.

**Consistencia de nombres:** los nombres de capa (`Header`, `Filtros`, `Tabla`, `Fila`, `CeldaCódigo`, etc.) definidos en Task 2 se reutilizan textualmente en Tasks 3-11 sin variación.
