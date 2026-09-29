# Diseño: Rediseño visual del login según prototipo Figma "FarmaVita"

Fecha: 2026-09-28
Estado: propuesto

## Contexto

El archivo de Figma `dAGg5r6W2taj38y4MMH3xH` (compartido por el usuario al inicio del prototipado del ERP) contiene, dentro de la página "Login – Botica", un frame `01 · Login` con un diseño visual completo bajo la marca "FarmaVita": panel izquierdo de branding en verde-teal con degradado, y panel derecho con el formulario de acceso. El login actual (`frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx` + `features/auth/components/LoginForm.tsx`) usa una paleta azul/amarillo (tokens `primary`/`warning`) y un layout de contenido distinto (beneficios en lista, métricas de sucursales/unidades/disponibilidad).

El login **ya está conectado a un backend real**: `useAuthSession`/`auth.api.ts` hacen login JWT contra el módulo `security` (el más maduro del backend), y `LoginForm.tsx` valida con `loginSchema` (zod: `email`, `password`, `remember`) vía react-hook-form. Este spec cubre únicamente el cambio visual — ninguna lógica de autenticación, validación, ni el contrato de `LoginCredentials` se modifica.

El prototipo de Figma incluye elementos que no existen en el flujo real de autenticación actual: un selector de "Sucursal", botones "Código QR" y "PIN de caja" como métodos alternativos de acceso, y una topbar con "Ayuda"/selector de idioma. Ninguno tiene soporte de backend (el `loginSchema` real no incluye `sucursal`, y no existen endpoints de login por QR o PIN). Se decidió incluirlos como UI puramente visual/decorativa, sin lógica real, para mantener fidelidad al prototipo sin inventar funcionalidad de backend inexistente.

Las pantallas `02 · Login – Error` y `03 · Recuperar contraseña` del mismo archivo de Figma no se pudieron inspeccionar en detalle (cuota de llamadas MCP de Figma agotada durante esta sesión) — quedan fuera de alcance de este spec, para una fase posterior.

## Alcance

Rediseño visual de:
- `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`
- `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`

**Cambios de contenido/estructura:**
1. **Panel izquierdo** (branding, oculto en mobile, visible en `lg:`): fondo verde-teal (paleta `success`, no `primary`), logo "FarmaVita" con ícono de cruz/botica, badge "Plataforma para boticas y farmacias", título "Gestiona tu botica con precisión y confianza.", párrafo descriptivo, 3 bloques de feature con ícono (Inventario inteligente / Ventas y comprobantes / Reportes en tiempo real, cada uno con su descripción corta tomada del prototipo), 2 tarjetas de métricas estáticas de ejemplo (Ventas de hoy, Por vencer), footer con nota de seguridad ("Datos cifrados de extremo a extremo · Respaldo diario automático").
2. **Topbar del panel derecho**: "Ayuda" y selector de idioma ("Español"), ambos decorativos (sin acción real — `onClick` vacío o ausente, no bloquean ni redirigen).
3. **Formulario** (`LoginForm.tsx`):
   - Título "Bienvenido de nuevo 👋" y subtítulo, reemplazando "Ingresa a tu cuenta".
   - Nuevo campo **"Sucursal"**: `<select>` puramente visual con opciones estáticas de ejemplo (ej. "Botica Central – Huamanga"). **No se registra en `useForm`, no se valida contra `loginSchema`, no se incluye en `onAuthenticate`** — el schema real no lo soporta y esto no cambia.
   - Campos "Usuario o correo electrónico" y "Contraseña" (con toggle mostrar/ocultar ya existente) se mantienen funcionalmente idénticos, solo restilizados.
   - Checkbox "Recordar mi correo en este equipo" + link "¿Olvidaste tu contraseña?" se mantienen funcionalmente idénticos (mismo estado `recoveryVisible`), solo restilizados.
   - Botón "Iniciar sesión" restilizado, mismo `type="submit"` y mismo comportamiento de `isSubmitting`.
   - Nuevo separador "o continúa con" + botones "Código QR" y "PIN de caja": **puramente decorativos**, sin `onClick` funcional (o un `onClick` que no hace nada — se decide en el plan si se omite el atributo o se deja un no-op explícito, ninguna de las dos opciones implica navegación ni llamada a API).
   - Aviso de seguridad inferior se mantiene (ya existe, solo restilizado).
4. **Paleta de color:** tokens `success-*` ya definidos en `frontend/apps/erp-web/src/styles.css` (mismo criterio ya acordado para el login: excepción consciente a la regla "success no decorativo" del spec `2026-09-20-rediseno-paleta-colores-frontend-design.md`, sin modificar ese spec ni el token `primary` global). No se introduce una tercera paleta.

**Fuera de alcance:**
- Cualquier cambio a `useAuthSession`, `auth.api.ts`, `login.schema.ts`, o el flujo real de autenticación.
- Las pantallas `02 · Login – Error` y `03 · Recuperar contraseña` del prototipo — fase separada, pendiente de inspección visual completa.
- Funcionalidad real de login por sucursal, QR o PIN de caja — no existe backend para esto; si en el futuro se implementa, es un proyecto aparte con su propio spec.
- El componente compartido `packages/ui-web/src/button/Button.tsx` — su variante `primary` no cambia; el botón "Iniciar sesión" se ajusta con `className` override local, igual que se había decidido para el spec de color anterior.
- Tests de otras features — solo se tocan los tests existentes de `auth` si aseveran texto/clases que cambian.

## Verificación

- `pnpm lint` y `pnpm typecheck` en `frontend/` sin nuevos errores.
- Revisar `LoginPage.test.tsx` y cualquier test de `LoginForm`: ajustar únicamente si aseveran contenido textual o clases CSS que cambian de nombre por este rediseño (no se espera que la lógica de submit/validación se vea afectada).
- Revisión visual manual con `pnpm dev` en `/login`, comparando contra el screenshot de Figma del frame `01 · Login`, en modo claro y oscuro.
- Confirmar manualmente que el campo "Sucursal", "Código QR" y "PIN de caja" no interfieren con el submit real del formulario (el payload enviado a `authenticate()` sigue siendo exactamente `{ email, password, remember }`).

## Riesgos / decisiones abiertas

- El dark mode del nuevo diseño (pares `dark:`) se resuelve en el plan de implementación siguiendo el mismo patrón de opacidad/peldaño que ya usa el archivo actual, sustituyendo el hue de `primary`/`warning` por `success`/blanco — no hay un mockup de Figma para modo oscuro específico.
- Si el usuario pide más adelante dar funcionalidad real a sucursal/QR/PIN de caja, requiere su propio ciclo de brainstorming (nuevo endpoint de backend, contrato de `loginSchema` extendido) — no se anticipa aquí.
- Las pantallas de error y recuperación de contraseña del prototipo quedan pendientes; cuando la cuota de Figma se libere, se inspeccionan y se decide si ameritan un spec propio o se agregan como extensión de este.
