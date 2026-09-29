# Prototipo Figma de catálogos maestros simples — Plan de trabajo

> **Para quien ejecute este plan:** este NO es un plan de código. Es un plan de trabajo de diseño en Figma usando las herramientas MCP `mcp__claude_ai_Figma__*`. No hay tests unitarios ni TDD — la verificación de cada tarea es visual, vía `get_screenshot`, comparando contra los criterios de aceptación listados en cada tarea. Antes de cualquier llamada a `use_figma`, se debe invocar la skill `figma-use` (y `figma-generate-library` para las tareas de fundaciones de estilos/componentes) — es un prerequisito obligatorio, no opcional.
>
> **Actualización tras inspección real del archivo (importante, léase antes de ejecutar):** el archivo de Figma `dAGg5r6W2taj38y4MMH3xH` NO contiene solo la página de login. La página `0:1` ("Login – Botica") ya tiene 4 frames de 1440×900: `01 · Login` (`2:2`), `02 · Login – Error` (`4:2`), `03 · Recuperar contraseña` (`4:206`) y **`04 · Panel principal`** (`3:2`) — un shell de ERP completo con `Sidebar` (`3:3`: logo, nav Inicio/Ventas/Inventario/Compras/Clientes/Reportes/Configuración, perfil de usuario) y `Contenido` (`3:61`: `Topbar` en `3:62` con buscador+notificaciones+botón acción, más el área de página con KPIs/gráfico/alertas específicos del dashboard). Tipografía confirmada: **Plus Jakarta Sans** (pesos Regular/Medium/SemiBold/Bold/ExtraBold, ya con variable font `wght`). No existen variables de color ni estilos de texto creados todavía (`getLocalVariableCollectionsAsync()` y `getLocalTextStyles()` devuelven vacío) — los colores del archivo están todos como fills sólidos directos, no como variables.
>
> Esto cambia el enfoque original (que planeaba construir fundaciones/componente maestro aislado desde cero): en su lugar, se **extrae el `Sidebar` + `Topbar` de `04 · Panel principal` como shell reutilizable**, y cada pantalla de catálogo se construye reemplazando únicamente el área de contenido específico del dashboard (KPIs/gráfico/alertas) por el header+filtros+tabla+modales de catálogo. Las Tasks 1-3 de este plan reflejan ya el enfoque corregido.

**Goal:** Construir en el archivo de Figma `dAGg5r6W2taj38y4MMH3xH` un componente maestro reutilizable "Catálogo simple" (que reusa el shell Sidebar+Topbar ya existente en `04 · Panel principal`) y sus 8 instancias (rubro comercial, condición de venta, forma farmacéutica, vía de administración, unidad de medida, clasificación controlada, tipo de documento de identidad, principio activo), siguiendo el spec `docs/superpowers/specs/2026-09-28-prototipo-figma-catalogos-simples-design.md`.

**Architecture:** Un componente `Shell / Sidebar` y un componente `Shell / Topbar` extraídos del frame `04 · Panel principal` existente. Un componente maestro "Catálogo simple / Página" que instancia ambos + un bloque de contenido propio (header, filtros, tabla con 4 variantes de estado, 2 modales). Cada uno de los 8 catálogos es una página nueva del archivo de Figma que instancia el maestro y sobreescribe el contenido (columnas, campos de formulario, datos de ejemplo), heredando el nav activo correcto (todas marcan "Configuración" o un ítem de catálogo si se decide agregar uno al nav — ver Task 2).

**Tech Stack:** Figma (vía MCP `mcp__claude_ai_Figma__*` + skills `figma-use`, `figma-generate-library`), archivo `dAGg5r6W2taj38y4MMH3xH`. Tipografía: Plus Jakarta Sans (Regular 400, Medium 500, SemiBold 600, Bold 700, ExtraBold 800).

## Global Constraints

- Paleta de color: reutilizar la escala `success` ya usada en el login — `#ecfdf5, #d1fae5, #a7f3d0, #6ee7b7, #34d399, #10b981, #059669, #047857, #065f46, #064e3b, #022c22` (50→950) — más blanco para acentos. No introducir un tercer color base.
- Tipografía: Plus Jakarta Sans exclusivamente, reusando los pares tamaño/peso ya usados en el archivo (ver Task 1 para el mapeo exacto extraído del shell).
- No tocar código de `frontend/` ni `service-botica/` en este plan — es solo diseño en Figma.
- No modificar los frames existentes `01 · Login`, `02 · Login – Error`, `03 · Recuperar contraseña`, ni el `04 · Panel principal` original — se leen/duplican, nunca se editan in-place.
- Los datos de ejemplo deben ser reales/plausibles del dominio farmacéutico peruano, nunca "Lorem ipsum" ni "Item 1/2/3".
- Antes de cualquier `use_figma`, invocar la skill `figma-use`; antes de crear componentes/variantes con fundaciones de estilo, invocar también `figma-generate-library`.
- Trabajar en pasos pequeños (máx. ~10 operaciones lógicas por llamada `use_figma`), validar con `get_metadata`/`get_screenshot` después de cada paso, y devolver siempre los IDs de nodos creados/mutados en el `return` del script, como exige la skill `figma-use`.

---

## Task 1: Fundaciones de estilo — variables de color y estilos de texto

**Herramientas:** `figma-use` + `figma-generate-library` skills, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_variable_defs`

**Consumido por:** Tasks 2-11 referencian estas variables/estilos por nombre al construir el shell extraído, el componente maestro y las 8 instancias.

**Produce:**
- Colección de variables `Catálogos — Color` con 11 variables `success/50` … `success/950` (scope `FRAME_FILL, SHAPE_FILL` para los pasos usados como fondo, `TEXT_FILL` para los usados como color de texto — crear cada variable con el scope que corresponda a su uso real, no `ALL_SCOPES`).
- Estilos de texto, todos en Plus Jakarta Sans, replicando los pares tamaño/peso ya usados en el shell (confirmados por inspección real, no asumidos):
  - `Heading/Page` — 26px, ExtraBold (800) — igual que "Buen día, Juan" (`3:78`)
  - `Body/Description` — 14px, Medium (500) — igual que la línea bajo el saludo (`3:79`)
  - `Label/Field` — 13px, SemiBold (600) — igual que los labels del login (`2:127`, `2:139`, `2:147`)
  - `Body/Table` — 14px, Regular (400) — igual que el placeholder del buscador (`3:67`)
  - `Label/Nav` — 14px, Medium (500) / Bold (700) para el ítem activo — igual que los ítems del sidebar (`3:20`, `3:14`)

- [ ] **Paso 1: Cargar la skill `figma-use` y `figma-generate-library`** (ya cargadas en esta sesión si se continúa el mismo hilo de trabajo; si se retoma en una sesión nueva, cargarlas de nuevo antes de cualquier `use_figma`).

- [ ] **Paso 2: Crear la colección de variables de color**

```js
// skillNames: "figma-use,figma-generate-library"
const collection = figma.variables.createVariableCollection('Catálogos — Color');
const modeId = collection.modes[0].modeId;

const steps = {
  '50': '#ecfdf5', '100': '#d1fae5', '200': '#a7f3d0', '300': '#6ee7b7',
  '400': '#34d399', '500': '#10b981', '600': '#059669', '700': '#047857',
  '800': '#065f46', '900': '#064e3b', '950': '#022c22'
};

function hexToRgb(hex) {
  const n = parseInt(hex.slice(1), 16);
  return { r: ((n >> 16) & 255) / 255, g: ((n >> 8) & 255) / 255, b: (n & 255) / 255 };
}

const createdIds = [];
for (const [step, hex] of Object.entries(steps)) {
  const v = figma.variables.createVariable(`success/${step}`, collection, 'COLOR');
  v.setValueForMode(modeId, hexToRgb(hex));
  v.scopes = ['FRAME_FILL', 'SHAPE_FILL', 'TEXT_FILL', 'STROKE_COLOR'];
  createdIds.push(v.id);
}

return { collectionId: collection.id, modeId, createdVariableIds: createdIds };
```

- [ ] **Paso 3: Verificar con `get_variable_defs`**

```
mcp__claude_ai_Figma__get_variable_defs(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "0:1")
```
Criterio de aceptación: las 11 variables `success/50`…`success/950` existen con los valores hex correctos.

- [ ] **Paso 4: Crear los 5 estilos de texto usando `figma.loadFontAsync` antes de cada uno (recipe: load font → await → crear estilo → return ids)**

```js
// skillNames: "figma-use,figma-generate-library"
const specs = [
  { name: 'Heading/Page', family: 'Plus Jakarta Sans', style: 'Extra Bold', size: 26 },
  { name: 'Body/Description', family: 'Plus Jakarta Sans', style: 'Medium', size: 14 },
  { name: 'Label/Field', family: 'Plus Jakarta Sans', style: 'Semi Bold', size: 13 },
  { name: 'Body/Table', family: 'Plus Jakarta Sans', style: 'Regular', size: 14 },
  { name: 'Label/Nav', family: 'Plus Jakarta Sans', style: 'Medium', size: 14 }
];

const createdStyleIds = [];
for (const spec of specs) {
  await figma.loadFontAsync({ family: spec.family, style: spec.style });
  const style = figma.createTextStyle();
  style.name = spec.name;
  style.fontName = { family: spec.family, style: spec.style };
  style.fontSize = spec.size;
  createdStyleIds.push(style.id);
}

return { createdStyleIds };
```

Nota: verificar primero con `await figma.listAvailableFontsAsync()` que el string de estilo exacto es `"Extra Bold"`/`"Semi Bold"` (con espacio) y no `"ExtraBold"`/`"SemiBold"` (sin espacio) — los nodos existentes reportaron `style: "ExtraBold"` vía `fontName`, pero la convención de `loadFontAsync` puede requerir el nombre con espacio tal como lo expone la fuente variable; si el paso 4 falla por fuente no encontrada, correr `listAvailableFontsAsync()` filtrando por `family === 'Plus Jakarta Sans'` y usar el string exacto devuelto.

- [ ] **Paso 5: Verificar con lectura de `getLocalTextStyles()`**

```js
// skillNames: "figma-use,figma-generate-library"
return figma.getLocalTextStyles().map(s => ({ name: s.name, id: s.id, fontSize: s.fontSize, fontName: s.fontName }));
```
Criterio de aceptación: los 5 estilos existen con los tamaños/pesos especificados.

---

## Task 2: Extraer el shell (Sidebar + Topbar) como componentes reutilizables

**Herramientas:** `figma-use` + `figma-generate-library` skills, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** ninguna variable nueva todavía (el shell ya usa fills sólidos existentes; rebind a variables `success/*` es opcional, ver Paso 4).

**Produce:** dos componentes de Figma:
- `Shell / Sidebar` (252×900+38 de alto extra por el ítem nuevo, o re-distribuido para mantener 900 de alto) — clon del nodo `3:3`, con un ítem de nav nuevo **"Catálogo"** insertado entre "Inventario" y "Compras" (mismo patrón visual que `3:21` Nav/Inventario: ícono + texto, sin badge numérico), y una propiedad de componente `NavActivo` de tipo `VARIANT` con **8 opciones** (`Inicio`, `Ventas`, `Inventario`, `Catálogo`, `Compras`, `Clientes`, `Reportes`, `Configuración`) que controla qué ítem de nav se muestra resaltado (fondo `success/50` + texto `success/700`, replicando el estilo ya usado en "Inicio" dentro de `3:11`). Las 8 pantallas de catálogo (Tasks 4-11) usan la variante `NavActivo=Catálogo`.
- `Shell / Topbar` (1116×100) — clon del nodo `3:62`, con un slot de texto `Título` (vacío/oculto por defecto) para casos donde se requiera un breadcrumb distinto al buscador — si no se necesita en la práctica al construir Task 3, se simplifica a un clon directo sin modificar.

- [ ] **Paso 1: Cargar las skills `figma-use` y `figma-generate-library`**

- [ ] **Paso 2: Crear una página nueva "02 · Shell (componentes)" y clonar `3:3` (Sidebar) y `3:62` (Topbar) dentro de ella, posicionados uno al lado del otro**

```js
// skillNames: "figma-use,figma-generate-library"
const shellPage = figma.createPage();
shellPage.name = '02 · Shell (componentes)';
await figma.setCurrentPageAsync(shellPage);

const sourcePage = await figma.getNodeByIdAsync('0:1');
const sidebarSource = await figma.getNodeByIdAsync('3:3');
const topbarSource = await figma.getNodeByIdAsync('3:62');

const sidebarClone = sidebarSource.clone();
sidebarClone.x = 0;
sidebarClone.y = 0;
shellPage.appendChild(sidebarClone);

const topbarClone = topbarSource.clone();
topbarClone.x = 350;
topbarClone.y = 0;
shellPage.appendChild(topbarClone);

return { shellPageId: shellPage.id, sidebarCloneId: sidebarClone.id, topbarCloneId: topbarClone.id };
```

- [ ] **Paso 3: Verificar visualmente el clon antes de convertir a componente**

```
mcp__claude_ai_Figma__get_screenshot(fileKey: "dAGg5r6W2taj38y4MMH3xH", nodeId: "<sidebarCloneId>")
```
Criterio de aceptación: el clon se ve idéntico al sidebar original de `04 · Panel principal`.

- [ ] **Paso 4: Convertir el clon del sidebar en componente y crear la propiedad de variante `NavActivo`**

Usar `figma.combineAsVariants` sobre 7 copias del sidebar clonado (una por cada ítem de nav activo), cada una con el fondo/texto del ítem correspondiente cambiado al estilo "activo" (fondo `success/50`, texto `success/700`, siguiendo el patrón que ya tiene "Inicio" en el original) y los demás en estilo inactivo (texto gris, sin fondo). Nombrar cada variante `NavActivo=Inicio`, `NavActivo=Ventas`, etc. Igual patrón que indica `figma-generate-library` para variant sets (combineAsVariants + grid layout manual después, ya que las variantes se apilan en (0,0) tras combinar).

- [ ] **Paso 5: Convertir el clon del topbar en componente simple (sin variantes, ya que su contenido es igual en todas las pantallas de catálogo salvo el placeholder del buscador que puede quedar genérico)**

- [ ] **Paso 6: Verificar con `get_metadata` que ambos componentes existen con los nombres `Shell / Sidebar` y `Shell / Topbar`, y con `get_screenshot` que las 7 variantes del sidebar son visualmente distintas solo en el ítem resaltado**

- [ ] **Paso 7: Checkpoint — mostrar el screenshot al usuario y confirmar antes de construir el componente maestro de catálogo sobre este shell**

---

## Task 3: Componente maestro "Catálogo simple / Página" (header + filtros + tabla + modales)

**Herramientas:** `figma-use` + `figma-generate-library` skills, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** `Shell / Sidebar` (variante `NavActivo=Catálogo`, ítem agregado en Task 2), `Shell / Topbar`, variables `success/*` y estilos de texto de Task 1.

**Produce:** un componente `Catálogo simple / Página` (1440×900, Sidebar a la izquierda + Contenido a la derecha) con estas capas nombradas exactamente así (las Tasks 4-11 dependen de estos nombres para hacer overrides):
- `Header` → `Breadcrumb` (texto, estilo `Body/Description`), `Título` (texto, estilo `Heading/Page`), `Descripción` (texto, estilo `Body/Description`), `BotónNuevo` (fondo `success/600`, texto blanco, estilo `Label/Field`)
- `Filtros` → `CampoBúsqueda` (con ícono de lupa + placeholder), `SelectEstado` (Todos/Activo/Inactivo)
- `Tabla` (componente con variante `Estado`: `Con datos` | `Vacío` | `Cargando` | `Error`) → `FilaEncabezado` (slots de columna como textos), `FilasDatos` (lista de instancias de `Fila`)
- `Fila` (componente separado) → `CeldaCódigo`, `CeldaDenominación`, `CeldaExtra1`, `CeldaExtra2`, `CeldaExtra3` (slots de texto genéricos, ocultables si el catálogo tiene menos columnas), `CeldaEstado` (badge: fondo `success/100` + texto `success/800` para "Activo"; fondo gris + texto gris para "Inactivo"), `CeldaAcciones` (ícono lápiz editar + ícono power activar/desactivar)

Además: `Modal / Crear` y `Modal / Editar` (overlay semitransparente + tarjeta blanca centrada + título + slot `CamposFormulario` + botones Cancelar/Acción primaria en `success/600`), y los componentes de campo `Campo / Texto`, `Campo / Select`, `Campo / Checkbox`, `Campo / Numérico` (label arriba con estilo `Label/Field`, borde con focus ring `success` igual al patrón ya usado en los inputs del login `2:127`-`2:152`).

- [ ] **Paso 1: Cargar las skills `figma-use` y `figma-generate-library`**

- [ ] **Paso 2: Crear página "03 · Catálogo simple (maestro)". Instanciar `Shell / Sidebar` (variante `NavActivo=Catálogo`) en x=0 e instanciar `Shell / Topbar` en x=252,y=0**

- [ ] **Paso 3: Construir `Header` debajo del Topbar (x=288, y=152, siguiendo el mismo margen que usa `3:77` "Buen día, Juan" en el dashboard original) con los 4 elementos listados arriba, usando placeholders genéricos ("Catálogo / {Recurso}", "{Recurso}", "Administra los {recurso}s del catálogo.", "+ Nuevo {Recurso}") que se sobreescriben en cada instancia**

- [ ] **Paso 4: Construir `Filtros` debajo del header**

- [ ] **Paso 5: Construir el componente `Fila` con las celdas nombradas como se especifica, usando `Body/Table` para el texto de celdas**

- [ ] **Paso 6: Ensamblar `Tabla` con `FilaEncabezado` + 3 instancias de `Fila` con datos placeholder genéricos, como variante `Con datos` de un componente con propiedad `Estado`**

- [ ] **Paso 7: Crear las variantes `Vacío`, `Cargando`, `Error` de `Tabla`** (mensajes exactos: `"No se encontraron registros."`, 5 filas skeleton grises, `"No se pudo cargar el listado de {recurso}s."` + botón "Reintentar" — tomados literalmente de `SupportCatalogPage.tsx` en el frontend)

- [ ] **Paso 8: Construir `Campo / Texto`, `Campo / Select`, `Campo / Checkbox`, `Campo / Numérico`**

- [ ] **Paso 9: Construir `Modal / Crear` y `Modal / Editar`**

- [ ] **Paso 10: Ensamblar todo en el componente `Catálogo simple / Página` y verificar con `get_screenshot`**

Criterio de aceptación: el layout combina el shell real del dashboard (sidebar+topbar idénticos a `04 · Panel principal`, con "Catálogo" resaltado en el nav) con el patrón de `SupportCatalogPage.tsx` (header con acción a la derecha, filtros debajo, tabla con columna Estado y Acciones al final), colores usan la paleta `success`, tipografía Plus Jakarta Sans consistente con el resto del archivo.

- [ ] **Paso 11: Checkpoint — confirmar con el usuario que el componente maestro completo (Tasks 1-3) se ve correcto antes de replicarlo 8 veces**

---

## Task 4: Instancia — Rubro comercial

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Consume:** componente `Catálogo simple / Página` de Task 3.

**Produce:** página "Rubro comercial" instanciando el maestro con:
- Breadcrumb: "Catálogo / Rubros comerciales"; Título: "Rubros comerciales"; Descripción: "Administra los rubros comerciales del catálogo."; BotónNuevo: "+ Nuevo rubro"
- Columnas: Código, Denominación, Es farmacéutico (sí/no), Orden, Estado, Acciones
- Campos de formulario: Código (Campo/Texto), Denominación (Campo/Texto), Es farmacéutico (Campo/Checkbox), Orden (Campo/Numérico)
- 5 filas: `FARMA` (Farmacéutico, sí, 1), `BEBE` (Bebé y cuidado infantil, no, 2), `COSMETICA` (Cosmética y cuidado personal, no, 3), `CONVENIENCIA` (Conveniencia y snacks, no, 4), `CUIDADO_PERSONAL` (Cuidado personal e higiene, no, 5)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Rubro comercial", instanciar `Catálogo simple / Página` con el estado `Con datos`**
- [ ] **Paso 3: Sobreescribir textos de `Header` y columnas de `FilaEncabezado`**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar `Modal / Crear` y `Modal / Editar` con los 4 campos, editar precargado con "FARMA"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Rubro comercial" de la tabla de mapeo del spec**

---

## Task 5: Instancia — Condición de venta

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Condición de venta" con:
- Breadcrumb: "Catálogo / Condiciones de venta"; Título: "Condiciones de venta"; Descripción: "Administra las condiciones de venta del catálogo."; BotónNuevo: "+ Nueva condición"
- Columnas: Código, Denominación, Requiere receta (sí/no), Requiere retención (sí/no), Fuente, Estado, Acciones
- Campos: Código, Denominación, Requiere receta (checkbox), Requiere retención (checkbox), Fuente
- 5 filas: `VENTA_LIBRE` (Venta libre, no, no, DIGEMID), `RECETA_SIMPLE` (Venta con receta médica, sí, no, DIGEMID), `RECETA_RETENIDA` (Venta con receta retenida, sí, sí, DIGEMID), `RECETA_ESPECIAL` (Venta con receta especial controlada, sí, sí, DIGEMID), `HOSPITALARIO` (Uso exclusivo hospitalario, sí, no, DIGEMID)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Condición de venta", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 5 campos, editar precargado con "VENTA_LIBRE"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Condición de venta" del spec**

---

## Task 6: Instancia — Forma farmacéutica

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Forma farmacéutica" con:
- Breadcrumb: "Catálogo / Formas farmacéuticas"; Título: "Formas farmacéuticas"; Descripción: "Administra las formas farmacéuticas del catálogo."; BotónNuevo: "+ Nueva forma"
- Columnas: Código, Denominación, Fuente, Estado, Acciones
- Campos: Código, Denominación, Fuente
- 5 filas: `TAB` (Tableta, DIGEMID), `JAR` (Jarabe, DIGEMID), `CAP` (Cápsula, DIGEMID), `SUSP` (Suspensión oral, DIGEMID), `CREM` (Crema tópica, DIGEMID)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Forma farmacéutica", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 3 campos, editar precargado con "TAB"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Forma farmacéutica" del spec**

---

## Task 7: Instancia — Vía de administración

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Vía de administración" con:
- Breadcrumb: "Catálogo / Vías de administración"; Título: "Vías de administración"; Descripción: "Administra las vías de administración del catálogo."; BotónNuevo: "+ Nueva vía"
- Columnas: Código, Denominación, Estado, Acciones (sin columnas extra)
- Campos: Código, Denominación
- 5 filas: `ORAL` (Oral), `TOPICA` (Tópica), `IM` (Intramuscular), `IV` (Intravenosa), `OFTALMICA` (Oftálmica)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Vía de administración", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas (sin celdas extra — ocultar `CeldaExtra1/2/3`)**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 2 campos, editar precargado con "ORAL"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Vía de administración" del spec**

---

## Task 8: Instancia — Unidad de medida

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Unidad de medida" con:
- Breadcrumb: "Catálogo / Unidades de medida"; Título: "Unidades de medida"; Descripción: "Administra las unidades de medida del catálogo."; BotónNuevo: "+ Nueva unidad"
- Columnas: Código, Símbolo, Permite decimal (sí/no), Estado, Acciones
- Campos: Código, Símbolo, Permite decimal (checkbox)
- 5 filas: `NIU` ("UND", no), `KGM` ("KG", sí), `LTR` ("L", sí), `CJA` ("CJA", no), `FRA` ("FRA", no)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Unidad de medida", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas (usar `CeldaDenominación` para mostrar el Símbolo, ya que este catálogo no tiene columna Denominación propia según el spec)**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 3 campos, editar precargado con "NIU"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Unidad de medida" del spec**

---

## Task 9: Instancia — Clasificación controlada

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Clasificación controlada" con:
- Breadcrumb: "Catálogo / Clasificaciones controladas"; Título: "Clasificaciones controladas"; Descripción: "Administra las clasificaciones controladas del catálogo."; BotónNuevo: "+ Nueva clasificación"
- Columnas: Código, Denominación, Requiere receta especial (sí/no), Retiene receta (sí/no), Vigencia receta (días), Estado, Acciones
- Campos: Código, Denominación, Requiere receta especial (checkbox), Retiene receta (checkbox), Vigencia receta en días (numérico)
- 5 filas: `PSICOTROPICO_I` (Psicotrópico Lista I, sí, sí, 30), `PSICOTROPICO_II` (Psicotrópico Lista II, sí, sí, 30), `ESTUPEFACIENTE` (Estupefaciente, sí, sí, 15), `PRECURSOR` (Precursor de uso restringido, sí, no, 30), `ANTIBIOTICO_CONTROLADO` (Antibiótico de control especial, sí, no, 30)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Clasificación controlada", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 5 campos, editar precargado con "PSICOTROPICO_I"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Clasificación controlada" del spec**

---

## Task 10: Instancia — Tipo de documento de identidad

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Tipo de documento de identidad" con:
- Breadcrumb: "Catálogo / Tipos de documento de identidad"; Título: "Tipos de documento de identidad"; Descripción: "Administra el catálogo SUNAT de tipos de documento de identidad."; BotónNuevo: "+ Nuevo tipo"
- Columnas: Código, Denominación, Sigla, Longitud mín/máx, Estado, Acciones
- Campos: Código, Sigla, Denominación, Longitud mínima (numérico), Longitud máxima (numérico)
- 5 filas: `1` (DNI, "DNI", 8, 8), `4` (Carné de extranjería, "CE", 9, 12), `6` (RUC, "RUC", 11, 11), `7` (Pasaporte, "PAS", 9, 12), `A` (Cédula diplomática de identidad, "CDI", 9, 12)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Tipo de documento de identidad", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 5 campos, editar precargado con "1" (DNI)**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Tipo de documento de identidad" del spec**

---

## Task 11: Instancia — Principio activo

**Herramientas:** `figma-use` skill, `mcp__claude_ai_Figma__use_figma`, `mcp__claude_ai_Figma__get_screenshot`

**Produce:** página "Principio activo" con:
- Breadcrumb: "Catálogo / Principios activos"; Título: "Principios activos"; Descripción: "Administra los principios activos (DCI) del catálogo."; BotónNuevo: "+ Nuevo principio activo"
- Columnas: Denominación, Nombre normalizado, Fuente, Estado, Acciones (sin columna Código — usar `CeldaDenominación` como identificador principal y ocultar `CeldaCódigo`)
- Campos: Denominación, Nombre normalizado, Fuente
- 5 filas: (Paracetamol, PARACETAMOL, DIGEMID), (Ibuprofeno, IBUPROFENO, DIGEMID), (Amoxicilina, AMOXICILINA, DIGEMID), (Loratadina, LORATADINA, DIGEMID), (Omeprazol, OMEPRAZOL, DIGEMID)

- [ ] **Paso 1: Cargar la skill `figma-use`**
- [ ] **Paso 2: Crear página "Principio activo", instanciar el maestro**
- [ ] **Paso 3: Sobreescribir header y columnas, ocultar `CeldaCódigo`**
- [ ] **Paso 4: Poblar 5 filas con los datos listados**
- [ ] **Paso 5: Instanciar modales con los 3 campos, editar precargado con "Paracetamol"**
- [ ] **Paso 6: Verificar con `get_screenshot`, comparar contra la fila "Principio activo" del spec**

---

## Task 12: Revisión final y cierre de la fase

**Herramientas:** `mcp__claude_ai_Figma__get_metadata`, `mcp__claude_ai_Figma__get_screenshot`

- [ ] **Paso 1: Listar todas las páginas del archivo para confirmar que existen las 11 nuevas (1 shell + 1 maestro + 8 instancias) además de "Login – Botica"**

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

**Cobertura del spec:** los 8 catálogos del spec tienen su Task dedicada (4-11), el componente maestro con sus 4 estados de tabla y 2 modales está en Task 3, la paleta de color y estilos de texto están en Task 1, la extracción del shell real (hallazgo post-spec) está en Task 2, el checkpoint de aprobación del maestro está en Task 3 paso 11, y el cierre con actualización del spec está en Task 12.

**Placeholders:** ningún paso usa "TBD"/"agregar validación genérica" — cada instancia (Tasks 4-11) repite explícitamente sus propios textos, columnas, campos y datos de ejemplo completos. La única decisión explícitamente abierta y señalada como bloqueante es el ítem de nav activo en Task 3 (no se asume, se pregunta al usuario antes de proceder).

**Consistencia de nombres:** los nombres de capa (`Header`, `Filtros`, `Tabla`, `Fila`, `CeldaCódigo`, etc.) definidos en Task 3 se reutilizan textualmente en Tasks 4-11 sin variación. Los IDs de nodo reales del archivo (`3:3`, `3:62`, `2:2`, etc.) confirmados por inspección se usan literalmente en Tasks 1-2 en vez de placeholders.
