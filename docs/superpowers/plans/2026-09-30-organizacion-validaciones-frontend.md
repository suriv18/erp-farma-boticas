# Organización: validaciones y confirmaciones del frontend (Plan B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que la interfaz de Organización impida los errores más costosos antes de llegar al servidor: RUC inválido, teléfono y sitio web mal escritos, series en minúscula, almacenes con temperaturas incoherentes, cambios de estado sin confirmar y altas bajo un padre no operativo.

**Architecture:** Las validaciones viven en los schemas de zod (`schemas/`) y en utilidades puras (`lib/`), sin tocar la capa de API. Los comportamientos de formulario (auto-marcar «Controla temperatura», series en mayúscula) se resuelven con react-hook-form y descriptores de `CamposTexto`. El aviso de padre no operativo es un espejo de la regla del backend (Plan A): el servidor sigue siendo la autoridad.

**Tech Stack:** React 19, react-hook-form + zod 4, TanStack Query, Vitest + Testing Library + MSW, Playwright (desktop/tablet/móvil), pnpm.

## Global Constraints

Tomadas del spec `docs/superpowers/specs/2026-09-30-organizacion-reglas-y-validaciones-design.md` (Plan B) y del backend ya implementado (Plan A):

- RUC: mismo algoritmo módulo 11 que el backend y mismo mensaje: `El RUC no es válido: el dígito verificador no coincide.` Se mantiene el mensaje de formato `El RUC debe tener 11 dígitos e iniciar con 10 o 20.`
- Teléfono: solo dígitos, espacios, `+`, `-` y paréntesis, entre 6 y 15 caracteres. Sitio web: URL `http(s)` válida. Aplica a empresa y establecimiento (el sitio web solo existe en empresa).
- En los detalles, correo, sitio web y teléfono se muestran como enlaces `mailto:`, `https:` y `tel:` cuando el valor es enlazable; si no, como texto.
- Cambio de estado: para `SUSPENDIDO`, `BLOQUEADO` y `CLAUSURADO` se muestra un aviso con lo que implica y se exige marcar `Entiendo las consecuencias` antes de habilitar `Guardar estado`. El botón sigue deshabilitado si el estado elegido es el actual. El título nombra el elemento.
- Almacén (mensajes idénticos a los del backend): `Un almacén refrigerado debe controlar temperatura.`, `Indica la temperatura mínima y máxima cuando el almacén controla temperatura.`, `La temperatura mínima no puede ser mayor que la máxima.` Al elegir `REFRIGERADO` se marca `Controla temperatura`; las temperaturas solo son editables si esa casilla está marcada.
- Series de boleta y factura: se convierten a mayúscula y se quitan los espacios.
- Padre no operativo (misma tabla que el backend): empresa `SUSPENDIDO`/`BLOQUEADO` no admite establecimientos nuevos; establecimiento `SUSPENDIDO`/`CLAUSURADO` no admite almacenes ni terminales POS nuevos; `REMODELACION` sí. La página muestra un aviso y los botones `Nuevo …` quedan deshabilitados con el motivo.
- Todo archivo **nuevo** del frontend debe alcanzar 100% de cobertura de líneas y ramas (gate por archivo de Vitest); no relajar umbrales ni tocar `coverage-baseline.txt`.
- Sin comentarios explicativos en el código; sin código duplicado; estructura por features (nada nuevo fuera de `features/organizacion/`, `test/` y `e2e/`).
- No reintroducir `forwardRef` ni patrones de React Router 8 bloqueados por ESLint.
- Mensajes en español.

## Convenciones de este plan

Rutas relativas a `frontend/`. `FE` = `apps/erp-web/src/features/organizacion`. Comandos en PowerShell desde `frontend`, con `$env:CI='true'` para que pnpm no pida confirmación sin terminal. Plantilla de ejecución con cobertura de archivos concretos (sustituir `<tests>` y `<archivos>`):

```powershell
$env:CI='true'; pnpm.cmd exec vitest run <tests> --project erp-web --coverage --coverage.include="<archivos>" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100
```

Tras crear o editar archivos, formatear con `pnpm.cmd exec prettier --write <archivos>`.

## Mapa de archivos

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `FE/lib/ruc.ts` (+test) | Crear | Formato y dígito verificador del RUC |
| `FE/schemas/empresa.schema.ts` | Modificar | Usar el RUC, teléfono y sitio web nuevos |
| `FE/schemas/campos.ts` | Modificar | `telefonoOpcional`, `sitioWebOpcional` |
| `FE/schemas/establecimiento.schema.ts` | Modificar | Usar `telefonoOpcional` |
| `FE/lib/contacto.ts` (+test) | Crear | `href` de correo, teléfono y sitio web |
| `FE/components/DatoContacto.tsx` (+test) | Crear | Dato del detalle con enlace o texto |
| `FE/lib/form-values.ts` | Modificar | `aMayusculasSinEspacios` |
| `FE/components/CamposTexto.tsx` | Modificar | Descriptor `mayusculas` |
| `FE/components/TerminalForm.tsx` | Modificar | Series con `mayusculas` |
| `FE/schemas/almacen.schema.ts` | Modificar | Reglas de temperatura entre campos |
| `FE/components/FormFields.tsx` | Modificar | `CheckboxField` con error |
| `FE/components/AlmacenForm.tsx` | Modificar | Auto-marcado y temperaturas editables |
| `FE/lib/consecuencias-estado.ts` | Crear | Textos de consecuencias por estado |
| `FE/components/CambiarEstadoDialog.tsx` | Modificar | Aviso y confirmación |
| `FE/lib/altas.ts` (+test) | Crear | Motivo por el que un padre no admite altas |
| `FE/components/Aviso.tsx` (+test) | Crear | Aviso de advertencia reutilizable |
| `FE/components/{Establecimientos,Almacenes,Terminales}Section.tsx` | Modificar | Botón `Nuevo …` deshabilitado con motivo |
| `FE/pages/{EmpresaDetail,EstablecimientoDetail}Page.tsx` | Modificar | Enlaces, título, consecuencias y aviso |
| `e2e/organizacion.spec.ts`, `e2e/support/organizacion-api.ts` | Modificar | Flujo completo en tres viewports |

---

### Task 1: RUC con dígito verificador

**Files:**
- Create: `FE/lib/ruc.ts`, `FE/lib/ruc.test.ts`
- Modify: `FE/schemas/empresa.schema.ts`, `FE/schemas/empresa.schema.test.ts`
- Modify (sustitución de RUC): todos los `.ts`/`.tsx` bajo `apps/` y `e2e/` que usan `20123456789`, `10123456789` o `20999999992`

**Interfaces:**
- Consumes: nada de tareas previas.
- Produces: `RUC_FORMATO: RegExp` y `tieneDigitoVerificadorValido(ruc: string): boolean` desde `FE/lib/ruc.ts`. RUC válidos de referencia para todas las tareas: `20123456786`, `10123456781`, `20999999990`, `20000000010`, `20000000061`.

- [ ] **Step 1: Sustituir los RUC inválidos que usan los tests y fixtures**

Desde `frontend` (Git Bash):

```bash
grep -rl "20123456789\|10123456789\|20999999992" --include=*.ts --include=*.tsx apps e2e | xargs sed -i 's/20123456789/20123456786/g; s/10123456789/10123456781/g; s/20999999992/20999999990/g'
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion --project erp-web`
Expected: PASS (los schemas todavía no validan el dígito, así que nada cambia).

- [ ] **Step 2: Escribir los tests que fallan**

`FE/lib/ruc.test.ts`:

```ts
import { RUC_FORMATO, tieneDigitoVerificadorValido } from './ruc';

describe('ruc', () => {
  it.each(['20123456786', '10123456781', '20000000010', '20000000061'])(
    'acepta el RUC %s con dígito verificador correcto',
    (ruc) => {
      expect(tieneDigitoVerificadorValido(ruc)).toBe(true);
    }
  );

  it.each(['20123456789', '20123456780', '10123456789'])(
    'rechaza el RUC %s con dígito verificador incorrecto',
    (ruc) => {
      expect(tieneDigitoVerificadorValido(ruc)).toBe(false);
    }
  );

  it('RUC_FORMATO exige 11 dígitos que inicien con 10 o 20', () => {
    expect(RUC_FORMATO.test('20123456786')).toBe(true);
    expect(RUC_FORMATO.test('30123456786')).toBe(false);
    expect(RUC_FORMATO.test('2012345678')).toBe(false);
  });
});
```

En `FE/schemas/empresa.schema.test.ts`, dentro del `describe`, añadir antes del `it.each`:

```ts
  it('rechaza un RUC con dígito verificador inválido', () => {
    expect(messages({ ruc: '20123456789' })).toEqual([
      'El RUC no es válido: el dígito verificador no coincide.'
    ]);
  });

  it('con formato inválido solo informa el formato, no el dígito verificador', () => {
    expect(messages({ ruc: '30123456789' })).toEqual([
      'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'
    ]);
  });
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib/ruc.test.ts apps/erp-web/src/features/organizacion/schemas/empresa.schema.test.ts --project erp-web`
Expected: FAIL (`./ruc` no existe y el schema acepta `20123456789`).

- [ ] **Step 3: Implementar**

`FE/lib/ruc.ts`:

```ts
export const RUC_FORMATO = /^(10|20)\d{9}$/;

const PESOS = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2] as const;

export function tieneDigitoVerificadorValido(ruc: string): boolean {
  const suma = PESOS.reduce((total, peso, indice) => total + Number(ruc[indice]) * peso, 0);
  return (11 - (suma % 11)) % 10 === Number(ruc[10]);
}
```

En `FE/schemas/empresa.schema.ts`, añadir el import:

```ts
import { RUC_FORMATO, tieneDigitoVerificadorValido } from '../lib/ruc';
```

y reemplazar la línea:

```ts
  ruc: z.string().regex(/^(10|20)\d{9}$/, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'),
```

por:

```ts
  ruc: z
    .string()
    .regex(RUC_FORMATO, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.')
    .refine(
      (ruc) => !RUC_FORMATO.test(ruc) || tieneDigitoVerificadorValido(ruc),
      'El RUC no es válido: el dígito verificador no coincide.'
    ),
```

- [ ] **Step 4: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion` (toda la feature, para detectar tests que usen RUC inválidos), `<archivos>` = `apps/erp-web/src/features/organizacion/lib/ruc.ts`.
Expected: todos PASS y 100% en `ruc.ts`.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps frontend/e2e
git commit -m "feat(organizacion): validar el digito verificador del RUC en el formulario

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Teléfono y sitio web válidos, y enlaces en los detalles

**Files:**
- Modify: `FE/schemas/campos.ts`, `FE/schemas/campos.test.ts`, `FE/schemas/empresa.schema.ts`, `FE/schemas/empresa.schema.test.ts`, `FE/schemas/establecimiento.schema.ts`, `FE/schemas/establecimiento.schema.test.ts`
- Create: `FE/lib/contacto.ts`, `FE/lib/contacto.test.ts`, `FE/components/DatoContacto.tsx`, `FE/components/DatoContacto.test.tsx`
- Modify: `FE/pages/EmpresaDetailPage.tsx`, `FE/pages/EstablecimientoDetailPage.tsx`, y sus tests

**Interfaces:**
- Consumes: nada de tareas previas.
- Produces: `telefonoOpcional`, `sitioWebOpcional` (schemas de zod); `type TipoContacto = 'correo' | 'telefono' | 'web'`; `hrefContacto(tipo, valor): string | null`; `DatoContacto({ label, tipo, valor })`.

- [ ] **Step 1: Escribir los tests que fallan**

En `FE/schemas/campos.test.ts`, cambiar el import a:

```ts
import {
  correoOpcional,
  numeroOpcional,
  sitioWebOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional
} from './campos';
```

y añadir dentro del `describe`:

```ts
  it('telefonoOpcional acepta vacío y teléfonos con números, espacios, +, - y paréntesis', () => {
    expect(messages(telefonoOpcional, '')).toEqual([]);
    expect(messages(telefonoOpcional, '014445566')).toEqual([]);
    expect(messages(telefonoOpcional, '+51 (1) 444-5566')).toEqual([]);
  });

  it('telefonoOpcional rechaza letras, textos cortos y textos largos', () => {
    const mensaje =
      'El teléfono debe tener entre 6 y 15 caracteres: números, espacios, +, - o paréntesis.';
    expect(messages(telefonoOpcional, 'abc')).toEqual([mensaje]);
    expect(messages(telefonoOpcional, '12345')).toEqual([mensaje]);
    expect(messages(telefonoOpcional, '1234567890123456')).toEqual([mensaje]);
  });

  it('sitioWebOpcional acepta vacío y URL http o https', () => {
    expect(messages(sitioWebOpcional, '')).toEqual([]);
    expect(messages(sitioWebOpcional, 'https://boticas.pe')).toEqual([]);
    expect(messages(sitioWebOpcional, 'http://boticas.pe/ayuda')).toEqual([]);
  });

  it('sitioWebOpcional rechaza texto que no es URL http(s) y textos largos', () => {
    const mensaje = 'El sitio web debe ser una URL que empiece con http:// o https://.';
    expect(messages(sitioWebOpcional, 'x')).toEqual([mensaje]);
    expect(messages(sitioWebOpcional, 'ftp://boticas.pe')).toEqual([mensaje]);
    expect(messages(sitioWebOpcional, `https://${'a'.repeat(300)}.pe`)).toContain(
      'El sitio web no debe exceder 300 caracteres.'
    );
  });
```

En `FE/schemas/empresa.schema.test.ts` reemplazar la fila:

```ts
    [{ telefono: 'x'.repeat(41) }, 'El teléfono no debe exceder 40 caracteres.'],
```

por:

```ts
    [
      { telefono: 'abc' },
      'El teléfono debe tener entre 6 y 15 caracteres: números, espacios, +, - o paréntesis.'
    ],
```

y la fila:

```ts
    [{ sitioWeb: 'x'.repeat(301) }, 'El sitio web no debe exceder 300 caracteres.'],
```

por:

```ts
    [{ sitioWeb: 'x' }, 'El sitio web debe ser una URL que empiece con http:// o https://.'],
    [{ sitioWeb: `https://${'x'.repeat(300)}.pe` }, 'El sitio web no debe exceder 300 caracteres.'],
```

En `FE/schemas/establecimiento.schema.test.ts` reemplazar la fila:

```ts
    [{ telefono: 'x'.repeat(41) }, 'El teléfono no debe exceder 40 caracteres.'],
```

por:

```ts
    [
      { telefono: 'abc' },
      'El teléfono debe tener entre 6 y 15 caracteres: números, espacios, +, - o paréntesis.'
    ],
```

`FE/lib/contacto.test.ts`:

```ts
import { hrefContacto } from './contacto';

describe('hrefContacto', () => {
  it('construye mailto para un correo', () => {
    expect(hrefContacto('correo', ' contacto@boticas.pe ')).toBe('mailto:contacto@boticas.pe');
  });

  it('no enlaza un correo sin arroba', () => {
    expect(hrefContacto('correo', 'no-es-correo')).toBeNull();
  });

  it('construye tel con solo dígitos y +', () => {
    expect(hrefContacto('telefono', '+51 (1) 444-5566')).toBe('tel:+5114445566');
  });

  it('no enlaza un teléfono con menos de 6 dígitos', () => {
    expect(hrefContacto('telefono', 'abc')).toBeNull();
  });

  it('enlaza un sitio web http o https tal cual', () => {
    expect(hrefContacto('web', 'https://boticas.pe')).toBe('https://boticas.pe');
    expect(hrefContacto('web', 'http://boticas.pe')).toBe('http://boticas.pe');
  });

  it('no enlaza un sitio web sin protocolo http(s)', () => {
    expect(hrefContacto('web', 'x')).toBeNull();
    expect(hrefContacto('web', 'javascript:alert(1)')).toBeNull();
  });
});
```

`FE/components/DatoContacto.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { DatoContacto } from './DatoContacto';

describe('DatoContacto', () => {
  it('muestra el correo como enlace mailto', () => {
    render(<DatoContacto label="Correo" tipo="correo" valor="contacto@boticas.pe" />);

    expect(screen.getByRole('link', { name: 'contacto@boticas.pe' })).toHaveAttribute(
      'href',
      'mailto:contacto@boticas.pe'
    );
  });

  it('muestra el teléfono como enlace tel', () => {
    render(<DatoContacto label="Teléfono" tipo="telefono" valor="014445566" />);

    expect(screen.getByRole('link', { name: '014445566' })).toHaveAttribute('href', 'tel:014445566');
  });

  it('abre el sitio web en una pestaña nueva de forma segura', () => {
    render(<DatoContacto label="Sitio web" tipo="web" valor="https://boticas.pe" />);

    const link = screen.getByRole('link', { name: 'https://boticas.pe' });
    expect(link).toHaveAttribute('href', 'https://boticas.pe');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('muestra texto plano cuando el valor no es enlazable', () => {
    render(<DatoContacto label="Sitio web" tipo="web" valor="x" />);

    expect(screen.getByText('x')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it.each([null, ''])('muestra un guion cuando el valor es %j', (valor) => {
    render(<DatoContacto label="Correo" tipo="correo" valor={valor} />);

    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
```

En `FE/pages/EmpresaDetailPage.test.tsx`, añadir dentro del `describe`:

```tsx
  it('muestra correo, teléfono y sitio web como enlaces', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({
          ...sampleEmpresa,
          email: 'contacto@boticas.pe',
          telefono: '014445566',
          sitioWeb: 'https://boticas.pe'
        })
      ),
      http.get('*/api/v1/organizacion/establecimientos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByRole('link', { name: 'contacto@boticas.pe' })).toHaveAttribute(
      'href',
      'mailto:contacto@boticas.pe'
    );
    expect(screen.getByRole('link', { name: '014445566' })).toHaveAttribute('href', 'tel:014445566');
    expect(screen.getByRole('link', { name: 'https://boticas.pe' })).toHaveAttribute(
      'href',
      'https://boticas.pe'
    );
  });
```

En `FE/pages/EstablecimientoDetailPage.test.tsx`, añadir dentro del `describe`:

```tsx
  it('muestra correo y teléfono como enlaces', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({
          ...sampleEstablecimiento,
          email: 'botica@boticas.pe',
          telefono: '014445566'
        })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByRole('link', { name: 'botica@boticas.pe' })).toHaveAttribute(
      'href',
      'mailto:botica@boticas.pe'
    );
    expect(screen.getByRole('link', { name: '014445566' })).toHaveAttribute('href', 'tel:014445566');
  });
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion --project erp-web`
Expected: FAIL (faltan `telefonoOpcional`, `sitioWebOpcional`, `contacto` y `DatoContacto`).

- [ ] **Step 2: Implementar los schemas**

En `FE/schemas/campos.ts`, añadir al final:

```ts
export const telefonoOpcional = z
  .string()
  .regex(
    /^([\d\s+\-()]{6,15})?$/,
    'El teléfono debe tener entre 6 y 15 caracteres: números, espacios, +, - o paréntesis.'
  );

export const sitioWebOpcional = z
  .string()
  .max(300, 'El sitio web no debe exceder 300 caracteres.')
  .refine(
    (valor) => valor === '' || (z.url().safeParse(valor).success && /^https?:\/\//i.test(valor)),
    'El sitio web debe ser una URL que empiece con http:// o https://.'
  );
```

En `FE/schemas/empresa.schema.ts`, cambiar el import de `./campos` a:

```ts
import {
  correoOpcional,
  sitioWebOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional,
  zonaHorariaRequerida
} from './campos';
```

y reemplazar `telefono: textoOpcional(40, 'El teléfono'),` por `telefono: telefonoOpcional,` y `sitioWeb: textoOpcional(300, 'El sitio web'),` por `sitioWeb: sitioWebOpcional,`.

En `FE/schemas/establecimiento.schema.ts`, añadir `telefonoOpcional` al import de `./campos` (manteniendo el orden alfabético) y reemplazar `telefono: textoOpcional(40, 'El teléfono'),` por `telefono: telefonoOpcional,`.

- [ ] **Step 3: Implementar los enlaces**

`FE/lib/contacto.ts`:

```ts
export type TipoContacto = 'correo' | 'telefono' | 'web';

const HREFS: Record<TipoContacto, (valor: string) => string | null> = {
  correo: (valor) => (valor.includes('@') ? `mailto:${valor}` : null),
  telefono: (valor) => {
    const numero = valor.replace(/[^\d+]/g, '');
    return numero.length >= 6 ? `tel:${numero}` : null;
  },
  web: (valor) => (/^https?:\/\//i.test(valor) ? valor : null)
};

export function hrefContacto(tipo: TipoContacto, valor: string): string | null {
  return HREFS[tipo](valor.trim());
}
```

`FE/components/DatoContacto.tsx`:

```tsx
import { hrefContacto, type TipoContacto } from '../lib/contacto';
import { valueOrDash } from '../lib/format';
import { DatoItem } from './DatoItem';

type DatoContactoProps = {
  label: string;
  tipo: TipoContacto;
  valor: string | null;
};

export function DatoContacto({ label, tipo, valor }: DatoContactoProps) {
  const href = valor === null || valor === '' ? null : hrefContacto(tipo, valor);
  return (
    <DatoItem label={label}>
      {href === null ? (
        valueOrDash(valor)
      ) : (
        <a
          className="text-primary-700 dark:text-primary-400 hover:underline"
          href={href}
          {...(tipo === 'web' ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
        >
          {valor}
        </a>
      )}
    </DatoItem>
  );
}
```

- [ ] **Step 4: Usar `DatoContacto` en los detalles**

En `FE/pages/EmpresaDetailPage.tsx`, añadir `import { DatoContacto } from '../components/DatoContacto';` junto a los demás imports de componentes y reemplazar las tres líneas:

```tsx
          <DatoItem label="Teléfono">{valueOrDash(empresa.telefono)}</DatoItem>
          <DatoItem label="Correo">{valueOrDash(empresa.email)}</DatoItem>
          <DatoItem label="Sitio web">{valueOrDash(empresa.sitioWeb)}</DatoItem>
```

por:

```tsx
          <DatoContacto label="Teléfono" tipo="telefono" valor={empresa.telefono} />
          <DatoContacto label="Correo" tipo="correo" valor={empresa.email} />
          <DatoContacto label="Sitio web" tipo="web" valor={empresa.sitioWeb} />
```

En `FE/pages/EstablecimientoDetailPage.tsx`, añadir el mismo import y reemplazar:

```tsx
          <DatoItem label="Teléfono">{valueOrDash(establecimiento.telefono)}</DatoItem>
          <DatoItem label="Correo">{valueOrDash(establecimiento.email)}</DatoItem>
```

por:

```tsx
          <DatoContacto label="Teléfono" tipo="telefono" valor={establecimiento.telefono} />
          <DatoContacto label="Correo" tipo="correo" valor={establecimiento.email} />
```

- [ ] **Step 5: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion`, `<archivos>` = `apps/erp-web/src/features/organizacion/{lib/contacto.ts,components/DatoContacto.tsx,schemas/campos.ts,pages/EmpresaDetailPage.tsx,pages/EstablecimientoDetailPage.tsx}`.
Expected: todos PASS y 100% en esos cinco archivos.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): validar telefono y sitio web y mostrarlos como enlaces

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Series de comprobantes en mayúscula

**Files:**
- Modify: `FE/lib/form-values.ts`, `FE/lib/form-values.test.ts`, `FE/components/CamposTexto.tsx`, `FE/components/CamposTexto.test.tsx`, `FE/components/TerminalForm.tsx`, `FE/components/TerminalForm.test.tsx`

**Interfaces:**
- Consumes: nada de tareas previas.
- Produces: `aMayusculasSinEspacios(value: string): string` (en `FE/lib/form-values.ts`) y la propiedad opcional `mayusculas?: boolean` en `CampoTexto`.

- [ ] **Step 1: Escribir los tests que fallan**

En `FE/lib/form-values.test.ts`, añadir `aMayusculasSinEspacios` al import desde `./form-values` y este test dentro del `describe` principal (o, si el archivo usa varios `describe`, en uno nuevo al final):

```ts
describe('aMayusculasSinEspacios', () => {
  it('convierte a mayúscula y elimina todos los espacios', () => {
    expect(aMayusculasSinEspacios(' b0 01 ')).toBe('B001');
    expect(aMayusculasSinEspacios('')).toBe('');
  });
});
```

En `FE/components/CamposTexto.test.tsx`: cambiar la firma de `Prueba`:

```tsx
function Prueba({ onSubmit }: { onSubmit: (v: Valores) => void }) {
```

por:

```tsx
function Prueba({
  onSubmit,
  campos = CAMPOS
}: {
  onSubmit: (v: Valores) => void;
  campos?: ReadonlyArray<CampoTexto<Valores>>;
}) {
```

cambiar `<CamposTexto fields={CAMPOS} register={register} errors={errors} />` por `<CamposTexto fields={campos} register={register} errors={errors} />` y añadir dentro del `describe`:

```tsx
  it('con mayusculas muestra el texto en mayúsculas y envía el valor normalizado', async () => {
    const onSubmit = vi.fn();
    const campos: ReadonlyArray<CampoTexto<Valores>> = [
      { name: 'monto', id: 'f-monto', label: 'Monto', mayusculas: true }
    ];
    render(<Prueba onSubmit={onSubmit} campos={campos} />);

    const monto = screen.getByLabelText('Monto');
    await userEvent.type(monto, ' b0 01 ');
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(monto).toHaveClass('uppercase');
    await vi.waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0]?.[0]).toEqual({ codigo: 'C-1', monto: 'B001' });
  });
```

En `FE/components/TerminalForm.test.tsx`, añadir dentro del `describe`:

```tsx
  it('convierte las series a mayúscula al enviar', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'POS003');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 3');
    await user.type(screen.getByLabelText('Serie de boleta'), 'b003');
    await user.type(screen.getByLabelText('Serie de factura'), 'f003');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ serieBoletaDefecto: 'B003', serieFacturaDefecto: 'F003' })
    );
  });
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib/form-values.test.ts apps/erp-web/src/features/organizacion/components/CamposTexto.test.tsx apps/erp-web/src/features/organizacion/components/TerminalForm.test.tsx --project erp-web`
Expected: FAIL (no existe `aMayusculasSinEspacios` ni la propiedad `mayusculas`).

- [ ] **Step 2: Implementar**

En `FE/lib/form-values.ts`, añadir al final:

```ts
export function aMayusculasSinEspacios(value: string): string {
  return value.replace(/\s/g, '').toUpperCase();
}
```

Reemplazar `FE/components/CamposTexto.tsx` por:

```tsx
import { get } from 'react-hook-form';
import type { FieldErrors, FieldValues, Path, UseFormRegister } from 'react-hook-form';
import { aMayusculasSinEspacios } from '../lib/form-values';
import { TextField } from './FormFields';

export type CampoTexto<T extends FieldValues> = {
  name: Path<T>;
  id: string;
  label: string;
  inputMode?: 'decimal' | 'numeric' | 'text';
  readOnly?: boolean;
  mayusculas?: boolean;
};

export type CamposTextoProps<T extends FieldValues> = {
  fields: ReadonlyArray<CampoTexto<T>>;
  register: UseFormRegister<T>;
  errors: FieldErrors<T>;
};

export function CamposTexto<T extends FieldValues>({
  fields,
  register,
  errors
}: CamposTextoProps<T>) {
  return fields.map(({ name, id, label, inputMode, readOnly, mayusculas }) => (
    <TextField
      key={id}
      id={id}
      label={label}
      error={(get(errors, name) as { message?: string } | undefined)?.message}
      {...(inputMode ? { inputMode } : {})}
      {...(readOnly ? { readOnly } : {})}
      {...(mayusculas ? { className: 'uppercase' } : {})}
      {...register(name, mayusculas ? { setValueAs: aMayusculasSinEspacios } : undefined)}
    />
  ));
}
```

En `FE/components/TerminalForm.tsx`, reemplazar las dos entradas de series de `CAMPOS_TERMINAL`:

```tsx
  { name: 'serieBoletaDefecto', id: 'terminal-serie-boleta', label: 'Serie de boleta' },
  { name: 'serieFacturaDefecto', id: 'terminal-serie-factura', label: 'Serie de factura' },
```

por:

```tsx
  {
    name: 'serieBoletaDefecto',
    id: 'terminal-serie-boleta',
    label: 'Serie de boleta',
    mayusculas: true
  },
  {
    name: 'serieFacturaDefecto',
    id: 'terminal-serie-factura',
    label: 'Serie de factura',
    mayusculas: true
  },
```

- [ ] **Step 3: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion/lib/form-values.test.ts apps/erp-web/src/features/organizacion/components`, `<archivos>` = `apps/erp-web/src/features/organizacion/{lib/form-values.ts,components/CamposTexto.tsx,components/TerminalForm.tsx}`.
Expected: todos PASS y 100% en los tres archivos. Si la prueba `con mayusculas ...` falla porque el resolver del test no recibe el valor normalizado, revisar que `setValueAs` se está pasando a `register` y no a la entrada DOM.

- [ ] **Step 4: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): convertir las series de comprobantes a mayuscula

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Almacén coherente en el formulario

**Files:**
- Modify: `FE/schemas/almacen.schema.ts`, `FE/schemas/almacen.schema.test.ts`, `FE/components/FormFields.tsx`, `FE/components/FormFields.test.tsx`, `FE/components/AlmacenForm.tsx`, `FE/components/AlmacenForm.test.tsx`

**Interfaces:**
- Consumes: nada de tareas previas (usa `CampoTexto.readOnly`, que ya existe).
- Produces: `almacenSchema` con reglas entre campos; `CheckboxField` acepta `error?: string | undefined`.

- [ ] **Step 1: Escribir los tests que fallan**

En `FE/schemas/almacen.schema.test.ts`, añadir debajo de la función `messages`:

```ts
const REQUIERE_AMBAS = 'Indica la temperatura mínima y máxima cuando el almacén controla temperatura.';

function issues(overrides: Record<string, unknown>) {
  const result = almacenSchema.safeParse({ ...valid, ...overrides });
  return result.success
    ? []
    : result.error.issues.map((issue) => `${issue.path.join('.')}: ${issue.message}`);
}
```

y dentro del `describe` añadir:

```ts
  it('rechaza un almacén refrigerado que no controla temperatura', () => {
    expect(issues({ tipo: 'REFRIGERADO', controlTemperatura: false })).toContain(
      'controlTemperatura: Un almacén refrigerado debe controlar temperatura.'
    );
  });

  it('exige ambas temperaturas cuando controla temperatura y marca el campo que falta', () => {
    expect(issues({ controlTemperatura: true, temperaturaMinC: '', temperaturaMaxC: '8' })).toEqual([
      `temperaturaMinC: ${REQUIERE_AMBAS}`
    ]);
    expect(issues({ controlTemperatura: true, temperaturaMinC: '2', temperaturaMaxC: '' })).toEqual([
      `temperaturaMaxC: ${REQUIERE_AMBAS}`
    ]);
    expect(issues({ controlTemperatura: true, temperaturaMinC: '', temperaturaMaxC: '' })).toEqual([
      `temperaturaMinC: ${REQUIERE_AMBAS}`,
      `temperaturaMaxC: ${REQUIERE_AMBAS}`
    ]);
  });

  it('rechaza una temperatura mínima mayor que la máxima y acepta valores iguales', () => {
    expect(issues({ temperaturaMinC: '8', temperaturaMaxC: '2' })).toEqual([
      'temperaturaMinC: La temperatura mínima no puede ser mayor que la máxima.'
    ]);
    expect(issues({ temperaturaMinC: '5', temperaturaMaxC: '5' })).toEqual([]);
  });

  it('acepta una sola temperatura cuando no controla temperatura', () => {
    expect(issues({ temperaturaMinC: '', temperaturaMaxC: '8' })).toEqual([]);
    expect(issues({ temperaturaMinC: '2', temperaturaMaxC: '' })).toEqual([]);
  });
```

En `FE/components/FormFields.test.tsx`, añadir dentro del `describe('CheckboxField', ...)` (si no existe ese `describe`, crear uno al final del archivo):

```tsx
  it('muestra el error debajo de la casilla', () => {
    render(<CheckboxField id="activo" label="Activo" error="Es obligatorio." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Es obligatorio.');
  });

  it('no muestra error cuando no se recibe', () => {
    render(<CheckboxField id="activo" label="Activo" />);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
```

Reemplazar `FE/components/AlmacenForm.test.tsx` por:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ALMACEN_FORM_VACIO } from '../lib/form-defaults';
import { AlmacenForm } from './AlmacenForm';

function renderForm(props: Partial<Parameters<typeof AlmacenForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<AlmacenForm onSubmit={onSubmit} submitLabel="Crear almacén" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('AlmacenForm', () => {
  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...ALMACEN_FORM_VACIO,
      codigo: 'ALM001',
      nombre: 'Almacén Central'
    });
  });

  it('al elegir refrigerado marca controlar temperatura y permite editar las temperaturas', async () => {
    const { onSubmit, user } = renderForm();

    expect(screen.getByLabelText('Temperatura mínima (°C)')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Temperatura máxima (°C)')).toHaveAttribute('readonly');
    await user.type(screen.getByLabelText('Código'), 'ALM002');
    await user.type(screen.getByLabelText('Nombre'), 'Cadena de frío');
    await user.selectOptions(screen.getByLabelText('Tipo de almacén'), 'REFRIGERADO');

    expect(screen.getByLabelText('Controla temperatura')).toBeChecked();
    expect(screen.getByLabelText('Temperatura mínima (°C)')).not.toHaveAttribute('readonly');
    await user.click(screen.getByLabelText('Permite venta'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), '2');
    await user.type(screen.getByLabelText('Temperatura máxima (°C)'), '8');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        tipo: 'REFRIGERADO',
        permiteVenta: true,
        controlTemperatura: true,
        temperaturaMinC: '2',
        temperaturaMaxC: '8'
      })
    );
  });

  it('deja las temperaturas de solo lectura cuando se desmarca controlar temperatura', async () => {
    const { user } = renderForm();

    await user.click(screen.getByLabelText('Controla temperatura'));
    expect(screen.getByLabelText('Temperatura mínima (°C)')).not.toHaveAttribute('readonly');
    await user.click(screen.getByLabelText('Controla temperatura'));

    expect(screen.getByLabelText('Temperatura mínima (°C)')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Temperatura máxima (°C)')).toHaveAttribute('readonly');
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), 'frio');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('El nombre debe tener al menos 2 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('La temperatura mínima debe ser un número.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error de refrigerado sin controlar temperatura junto a la casilla', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM003');
    await user.type(screen.getByLabelText('Nombre'), 'Cámara fría');
    await user.selectOptions(screen.getByLabelText('Tipo de almacén'), 'REFRIGERADO');
    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(
      await screen.findByText('Un almacén refrigerado debe controlar temperatura.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error cuando la mínima es mayor que la máxima', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM004');
    await user.type(screen.getByLabelText('Nombre'), 'Rango invertido');
    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), '8');
    await user.type(screen.getByLabelText('Temperatura máxima (°C)'), '2');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(
      await screen.findByText('La temperatura mínima no puede ser mayor que la máxima.')
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en creación no muestra el campo activo y el código es editable', () => {
    renderForm();

    expect(screen.queryByLabelText('Almacén activo')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('en edición muestra activo, precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...ALMACEN_FORM_VACIO, codigo: 'ALM001', nombre: 'Almacén Central' }
    });

    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    const activo = screen.getByLabelText('Almacén activo');
    expect(activo).toBeChecked();
    await user.click(activo);
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'ALM001', activo: false })
    );
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe un almacén con el código indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ya existe un almacén con el código indicado.'
    );
    expect(screen.getByRole('button', { name: 'Crear almacén' })).toBeDisabled();
  });
});
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/schemas/almacen.schema.test.ts apps/erp-web/src/features/organizacion/components/FormFields.test.tsx apps/erp-web/src/features/organizacion/components/AlmacenForm.test.tsx --project erp-web`
Expected: FAIL (el schema y el formulario no tienen estas reglas).

- [ ] **Step 2: Implementar el schema**

Reemplazar `FE/schemas/almacen.schema.ts` por:

```ts
import { z } from 'zod';
import { TIPOS_ALMACEN } from '../api/almacenes.types';
import { codigoRequerido, nombreRequerido, numeroOpcional } from './campos';

const REQUIERE_AMBAS_TEMPERATURAS =
  'Indica la temperatura mínima y máxima cuando el almacén controla temperatura.';

export const almacenSchema = z
  .object({
    codigo: codigoRequerido,
    nombre: nombreRequerido(150),
    tipo: z.enum(TIPOS_ALMACEN, { error: 'Selecciona un tipo de almacén.' }),
    permiteLotes: z.boolean(),
    permiteVencimiento: z.boolean(),
    permiteVenta: z.boolean(),
    permiteDespacho: z.boolean(),
    controlTemperatura: z.boolean(),
    temperaturaMinC: numeroOpcional('La temperatura mínima'),
    temperaturaMaxC: numeroOpcional('La temperatura máxima'),
    activo: z.boolean()
  })
  .refine((valores) => valores.tipo !== 'REFRIGERADO' || valores.controlTemperatura, {
    path: ['controlTemperatura'],
    error: 'Un almacén refrigerado debe controlar temperatura.'
  })
  .refine((valores) => !valores.controlTemperatura || valores.temperaturaMinC !== '', {
    path: ['temperaturaMinC'],
    error: REQUIERE_AMBAS_TEMPERATURAS
  })
  .refine((valores) => !valores.controlTemperatura || valores.temperaturaMaxC !== '', {
    path: ['temperaturaMaxC'],
    error: REQUIERE_AMBAS_TEMPERATURAS
  })
  .refine(
    (valores) =>
      valores.temperaturaMinC === '' ||
      valores.temperaturaMaxC === '' ||
      Number(valores.temperaturaMinC) <= Number(valores.temperaturaMaxC),
    {
      path: ['temperaturaMinC'],
      error: 'La temperatura mínima no puede ser mayor que la máxima.'
    }
  );

export type AlmacenFormValues = z.infer<typeof almacenSchema>;
```

- [ ] **Step 3: Implementar el error en `CheckboxField`**

En `FE/components/FormFields.tsx`, reemplazar el componente `CheckboxField` por:

```tsx
export function CheckboxField({
  id,
  label,
  error,
  ...props
}: {
  id: string;
  label: string;
  error?: string | undefined;
} & Omit<ComponentPropsWithRef<'input'>, 'id' | 'type'>) {
  return (
    <div>
      <label
        htmlFor={id}
        className="flex items-center gap-2 text-sm text-neutral-700 dark:text-neutral-200"
      >
        <input
          id={id}
          type="checkbox"
          aria-invalid={error ? true : undefined}
          className="text-primary-600 size-4 rounded border-neutral-300"
          {...props}
        />
        {label}
      </label>
      {error ? (
        <p role="alert" className="text-danger-600 dark:text-danger-400 mt-1 text-xs font-medium">
          {error}
        </p>
      ) : null}
    </div>
  );
}
```

- [ ] **Step 4: Implementar el formulario**

En `FE/components/AlmacenForm.tsx`:

1. Reemplazar la línea `const camposIdentificacion = (isEdit: boolean) =>` y su cuerpo para añadir, justo debajo, la función de temperaturas editables:

```tsx
const camposTemperatura = (editable: boolean) =>
  CAMPOS_TEMPERATURA.map((campo) => ({ ...campo, readOnly: !editable }));
```

2. Reemplazar la desestructuración de `useForm`:

```tsx
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<AlmacenFormValues>({
    defaultValues: defaultValues ?? ALMACEN_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(almacenSchema)
  });
```

por:

```tsx
  const {
    formState: { errors },
    handleSubmit,
    register,
    setValue,
    watch
  } = useForm<AlmacenFormValues>({
    defaultValues: defaultValues ?? ALMACEN_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(almacenSchema)
  });
  const controlTemperatura = watch('controlTemperatura');
```

3. Reemplazar el `SelectField` del tipo:

```tsx
        <SelectField
          id="almacen-tipo"
          label="Tipo de almacén"
          error={errors.tipo?.message}
          {...register('tipo')}
        >
```

por:

```tsx
        <SelectField
          id="almacen-tipo"
          label="Tipo de almacén"
          error={errors.tipo?.message}
          {...register('tipo', {
            onChange: (event: { target: { value: string } }) => {
              if (event.target.value === 'REFRIGERADO') {
                setValue('controlTemperatura', true, { shouldDirty: true });
              }
            }
          })}
        >
```

4. Reemplazar `<CamposTexto fields={CAMPOS_TEMPERATURA} register={register} errors={errors} />` por:

```tsx
        <CamposTexto
          fields={camposTemperatura(controlTemperatura)}
          register={register}
          errors={errors}
        />
```

5. Reemplazar el bucle de indicadores:

```tsx
        {INDICADORES.map(({ name, id, label }) => (
          <CheckboxField key={id} id={id} label={label} {...register(name)} />
        ))}
```

por:

```tsx
        {INDICADORES.map(({ name, id, label }) => (
          <CheckboxField
            key={id}
            id={id}
            label={label}
            error={errors[name]?.message}
            {...register(name)}
          />
        ))}
```

- [ ] **Step 5: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion`, `<archivos>` = `apps/erp-web/src/features/organizacion/{schemas/almacen.schema.ts,components/FormFields.tsx,components/AlmacenForm.tsx}`.
Expected: todos PASS y 100% en los tres archivos. Los tests de `AlmacenesSection` (crear y editar almacenes GENERAL) siguen pasando sin cambios.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): validar la coherencia de temperaturas del almacen en el formulario

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Cambio de estado con confirmación

**Files:**
- Create: `FE/lib/consecuencias-estado.ts`
- Modify: `FE/components/CambiarEstadoDialog.tsx`, `FE/components/CambiarEstadoDialog.test.tsx`, `FE/pages/EmpresaDetailPage.tsx`, `FE/pages/EmpresaDetailPage.test.tsx`, `FE/pages/EstablecimientoDetailPage.tsx`, `FE/pages/EstablecimientoDetailPage.test.tsx`

**Interfaces:**
- Consumes: `CheckboxField` (existente; en la Task 4 ganó la prop `error`, sin cambiar su uso aquí), `EstadoEmpresa`, `EstadoEstablecimiento`.
- Produces: `CONSECUENCIAS_ESTADO_EMPRESA` y `CONSECUENCIAS_ESTADO_ESTABLECIMIENTO` (`Partial<Record<Estado, string>>`); prop `consecuencias?: Partial<Record<T, string>> | undefined` en `CambiarEstadoDialog`.

- [ ] **Step 1: Escribir los tests que fallan**

En `FE/components/CambiarEstadoDialog.test.tsx`, añadir dentro del `describe` (el `renderDialog` existente acepta `overrides` parciales, incluida la nueva prop):

```tsx
  it('muestra el aviso de consecuencias y exige confirmarlas antes de guardar', async () => {
    const { onSubmit, user } = renderDialog({
      consecuencias: { SUSPENDIDO: 'No admitirá altas nuevas.' }
    });

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');

    expect(screen.getByRole('note')).toHaveTextContent('No admitirá altas nuevas.');
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeEnabled();
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(onSubmit).toHaveBeenCalledWith('SUSPENDIDO');
  });

  it('no pide confirmación para un estado sin consecuencias', async () => {
    const { user } = renderDialog({ consecuencias: { SUSPENDIDO: 'No admitirá altas nuevas.' } });

    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');

    expect(screen.queryByRole('note')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeEnabled();
  });

  it('reinicia la confirmación al cambiar de estado', async () => {
    const { user } = renderDialog({
      consecuencias: { SUSPENDIDO: 'Aviso uno.', BLOQUEADO: 'Aviso dos.' }
    });

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');

    expect(screen.getByRole('note')).toHaveTextContent('Aviso dos.');
    expect(screen.getByLabelText('Entiendo las consecuencias')).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('no muestra el aviso del estado actual', () => {
    renderDialog({ consecuencias: { ACTIVO: 'No debería verse.' } });

    expect(screen.queryByRole('note')).not.toBeInTheDocument();
  });
```

En `FE/pages/EmpresaDetailPage.test.tsx`, en los dos tests que cambian el estado, insertar la confirmación:
- En `cambia el estado de la empresa`, justo después de `await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');` añadir:

```tsx
    expect(screen.getByRole('note')).toHaveTextContent(
      'Mientras esté suspendida, la empresa no admite establecimientos nuevos.'
    );
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
```

- En `muestra el error al cambiar el estado y lo limpia al cerrar`, justo después de `await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');` añadir:

```tsx
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
```

y añadir este test:

```tsx
  it('nombra la empresa en el título del cambio de estado', async () => {
    mockDefaultHandlers();
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));

    expect(
      screen.getByRole('heading', { name: 'Cambiar estado de Boticas SAC' })
    ).toBeInTheDocument();
  });
```

En `FE/pages/EstablecimientoDetailPage.test.tsx`, en `cambia el estado del establecimiento`, justo después de `await user.selectOptions(screen.getByLabelText('Estado'), 'CLAUSURADO');` añadir:

```tsx
    expect(screen.getByRole('note')).toHaveTextContent(
      'Mientras esté clausurado, el establecimiento no admite almacenes ni terminales POS nuevos.'
    );
    await user.click(screen.getByLabelText('Entiendo las consecuencias'));
```

(el test `muestra el error al cambiar el estado...` usa `REMODELACION`, que no tiene consecuencias y no necesita confirmación), y añadir:

```tsx
  it('nombra el establecimiento en el título del cambio de estado', async () => {
    mockDefaultHandlers();
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));

    expect(
      screen.getByRole('heading', { name: 'Cambiar estado de Botica Central' })
    ).toBeInTheDocument();
  });
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/CambiarEstadoDialog.test.tsx apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.test.tsx apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.test.tsx --project erp-web`
Expected: FAIL.

- [ ] **Step 2: Implementar los textos**

`FE/lib/consecuencias-estado.ts`:

```ts
import type { EstadoEmpresa } from '../api/empresas.types';
import type { EstadoEstablecimiento } from '../api/establecimientos.types';

export const CONSECUENCIAS_ESTADO_EMPRESA: Partial<Record<EstadoEmpresa, string>> = {
  SUSPENDIDO:
    'Mientras esté suspendida, la empresa no admite establecimientos nuevos. Lo ya registrado no cambia de estado.',
  BLOQUEADO:
    'Mientras esté bloqueada, la empresa no admite establecimientos nuevos. Lo ya registrado no cambia de estado.'
};

export const CONSECUENCIAS_ESTADO_ESTABLECIMIENTO: Partial<
  Record<EstadoEstablecimiento, string>
> = {
  SUSPENDIDO:
    'Mientras esté suspendido, el establecimiento no admite almacenes ni terminales POS nuevos. Lo ya registrado no cambia de estado.',
  CLAUSURADO:
    'Mientras esté clausurado, el establecimiento no admite almacenes ni terminales POS nuevos. Lo ya registrado no cambia de estado.'
};
```

- [ ] **Step 3: Implementar el diálogo**

Reemplazar `FE/components/CambiarEstadoDialog.tsx` por:

```tsx
import { useState } from 'react';
import { Button, FormField, Modal, Select } from '@boticas/ui-web';
import { FormError } from './FormError';
import { CheckboxField } from './FormFields';

export type CambiarEstadoDialogProps<T extends string> = {
  title: string;
  estados: readonly T[];
  current: T;
  consecuencias?: Partial<Record<T, string>> | undefined;
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (estado: T) => void;
  onClose: () => void;
};

export function CambiarEstadoDialog<T extends string>({
  title,
  estados,
  current,
  consecuencias,
  isSubmitting,
  error,
  onSubmit,
  onClose
}: CambiarEstadoDialogProps<T>) {
  const [estado, setEstado] = useState<T>(current);
  const [confirmado, setConfirmado] = useState(false);
  const consecuencia = estado === current ? undefined : consecuencias?.[estado];
  const faltaConfirmar = consecuencia !== undefined && !confirmado;

  return (
    <Modal open onClose={onClose} title={title}>
      <form
        className="space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit(estado);
        }}
      >
        <FormField label="Estado" htmlFor="cambiar-estado">
          <Select
            id="cambiar-estado"
            value={estado}
            onChange={(event) => {
              setEstado(event.target.value as T);
              setConfirmado(false);
            }}
          >
            {estados.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </Select>
        </FormField>
        {consecuencia ? (
          <div
            role="note"
            className="border-warning-200 bg-warning-50 text-warning-800 dark:border-warning-800 dark:bg-warning-900/30 dark:text-warning-300 space-y-3 rounded-lg border px-3 py-3 text-sm"
          >
            <p>{consecuencia}</p>
            <CheckboxField
              id="confirmar-consecuencias"
              label="Entiendo las consecuencias"
              checked={confirmado}
              onChange={(event) => setConfirmado(event.target.checked)}
            />
          </div>
        ) : null}
        {error ? <FormError message={error} /> : null}
        <Button type="submit" disabled={isSubmitting || estado === current || faltaConfirmar}>
          Guardar estado
        </Button>
      </form>
    </Modal>
  );
}
```

- [ ] **Step 4: Usarlo en las páginas**

En `FE/pages/EmpresaDetailPage.tsx`, añadir `import { CONSECUENCIAS_ESTADO_EMPRESA } from '../lib/consecuencias-estado';` con los imports de `lib/` y reemplazar:

```tsx
          title="Cambiar estado de la empresa"
          estados={ESTADOS_EMPRESA}
          current={empresa.estado}
```

por:

```tsx
          title={`Cambiar estado de ${empresa.razonSocial}`}
          estados={ESTADOS_EMPRESA}
          current={empresa.estado}
          consecuencias={CONSECUENCIAS_ESTADO_EMPRESA}
```

En `FE/pages/EstablecimientoDetailPage.tsx`, añadir `import { CONSECUENCIAS_ESTADO_ESTABLECIMIENTO } from '../lib/consecuencias-estado';` y reemplazar:

```tsx
          title="Cambiar estado del establecimiento"
          estados={ESTADOS_ESTABLECIMIENTO}
          current={establecimiento.estadoOperativo}
```

por:

```tsx
          title={`Cambiar estado de ${establecimiento.nombre}`}
          estados={ESTADOS_ESTABLECIMIENTO}
          current={establecimiento.estadoOperativo}
          consecuencias={CONSECUENCIAS_ESTADO_ESTABLECIMIENTO}
```

- [ ] **Step 5: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion`, `<archivos>` = `apps/erp-web/src/features/organizacion/{components/CambiarEstadoDialog.tsx,lib/consecuencias-estado.ts,pages/EmpresaDetailPage.tsx,pages/EstablecimientoDetailPage.tsx}`.
Expected: todos PASS y 100% en los cuatro archivos.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): pedir confirmacion al cambiar a un estado que bloquea altas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Aviso y botones deshabilitados con el padre no operativo

**Files:**
- Create: `FE/lib/altas.ts`, `FE/lib/altas.test.ts`, `FE/components/Aviso.tsx`, `FE/components/Aviso.test.tsx`
- Modify: `FE/components/EstablecimientosSection.tsx`, `AlmacenesSection.tsx`, `TerminalesSection.tsx` y sus tests, `FE/pages/EmpresaDetailPage.tsx`, `EstablecimientoDetailPage.tsx` y sus tests

**Interfaces:**
- Consumes: `EstadoEmpresa`, `EstadoEstablecimiento`.
- Produces: `motivoSinAltasEmpresa(estado): string | null`, `motivoSinAltasEstablecimiento(estado): string | null`, `Aviso({ children })` (con `role="status"`) y la prop `motivoSinAltas?: string | null` en las tres secciones.

- [ ] **Step 1: Escribir los tests que fallan**

`FE/lib/altas.test.ts`:

```ts
import { motivoSinAltasEmpresa, motivoSinAltasEstablecimiento } from './altas';

describe('motivoSinAltasEmpresa', () => {
  it('no hay motivo cuando la empresa está activa', () => {
    expect(motivoSinAltasEmpresa('ACTIVO')).toBeNull();
  });

  it.each(['SUSPENDIDO', 'BLOQUEADO'] as const)('explica el bloqueo con la empresa %s', (estado) => {
    expect(motivoSinAltasEmpresa(estado)).toBe(
      `La empresa está ${estado}; no admite establecimientos nuevos.`
    );
  });
});

describe('motivoSinAltasEstablecimiento', () => {
  it.each(['ACTIVO', 'REMODELACION'] as const)('no hay motivo con el establecimiento %s', (estado) => {
    expect(motivoSinAltasEstablecimiento(estado)).toBeNull();
  });

  it.each(['SUSPENDIDO', 'CLAUSURADO'] as const)(
    'explica el bloqueo con el establecimiento %s',
    (estado) => {
      expect(motivoSinAltasEstablecimiento(estado)).toBe(
        `El establecimiento está ${estado}; no admite almacenes ni terminales POS nuevos.`
      );
    }
  );
});
```

`FE/components/Aviso.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { Aviso } from './Aviso';

describe('Aviso', () => {
  it('muestra el mensaje como estado accesible', () => {
    render(<Aviso>La empresa está SUSPENDIDO.</Aviso>);

    expect(screen.getByRole('status')).toHaveTextContent('La empresa está SUSPENDIDO.');
  });
});
```

En `FE/components/EstablecimientosSection.test.tsx`, añadir dentro del `describe` (el `renderSection` actual no recibe props, así que se añade un helper):

```tsx
  it('deshabilita el alta y explica el motivo cuando la empresa no admite altas', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    renderRoute(
      '/empresa',
      () => (
        <EstablecimientosSection
          empresaId="empresa-1"
          motivoSinAltas="La empresa está SUSPENDIDO; no admite establecimientos nuevos."
        />
      ),
      '/empresa'
    );
    await screen.findByText('Esta empresa aún no tiene establecimientos.');

    const boton = screen.getByRole('button', { name: 'Nuevo establecimiento' });
    expect(boton).toBeDisabled();
    expect(boton).toHaveAttribute(
      'title',
      'La empresa está SUSPENDIDO; no admite establecimientos nuevos.'
    );
  });
```

En `FE/components/AlmacenesSection.test.tsx`:

```tsx
  it('deshabilita el alta y explica el motivo cuando el establecimiento no admite altas', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    renderRoute(
      '/establecimiento',
      () => (
        <AlmacenesSection
          establecimientoId="est-1"
          motivoSinAltas="El establecimiento está CLAUSURADO; no admite almacenes ni terminales POS nuevos."
        />
      ),
      '/establecimiento'
    );
    await screen.findByText('Este establecimiento aún no tiene almacenes.');

    const boton = screen.getByRole('button', { name: 'Nuevo almacén' });
    expect(boton).toBeDisabled();
    expect(boton).toHaveAttribute(
      'title',
      'El establecimiento está CLAUSURADO; no admite almacenes ni terminales POS nuevos.'
    );
  });
```

En `FE/components/TerminalesSection.test.tsx`:

```tsx
  it('deshabilita el alta y explica el motivo cuando el establecimiento no admite altas', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    renderRoute(
      '/establecimiento',
      () => (
        <TerminalesSection
          establecimientoId="est-1"
          motivoSinAltas="El establecimiento está SUSPENDIDO; no admite almacenes ni terminales POS nuevos."
        />
      ),
      '/establecimiento'
    );
    await screen.findByText('Este establecimiento aún no tiene terminales.');

    const boton = screen.getByRole('button', { name: 'Nuevo terminal' });
    expect(boton).toBeDisabled();
    expect(boton).toHaveAttribute(
      'title',
      'El establecimiento está SUSPENDIDO; no admite almacenes ni terminales POS nuevos.'
    );
  });
```

En `FE/pages/EmpresaDetailPage.test.tsx`:

```tsx
  it('avisa y bloquea el alta de establecimientos cuando la empresa está suspendida', async () => {
    server.use(
      http.get(detailUrl, () => HttpResponse.json({ ...sampleEmpresa, estado: 'SUSPENDIDO' })),
      http.get('*/api/v1/organizacion/establecimientos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByRole('status')).toHaveTextContent(
      'La empresa está SUSPENDIDO; no admite establecimientos nuevos.'
    );
    expect(screen.getByRole('button', { name: 'Nuevo establecimiento' })).toBeDisabled();
  });

  it('no muestra aviso ni bloquea el alta cuando la empresa está activa', async () => {
    mockDefaultHandlers();

    renderPage();

    await screen.findByRole('heading', { name: 'Boticas SAC' });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo establecimiento' })).toBeEnabled();
  });
```

En `FE/pages/EstablecimientoDetailPage.test.tsx`:

```tsx
  it('avisa y bloquea las altas cuando el establecimiento está clausurado', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'CLAUSURADO' })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByRole('status')).toHaveTextContent(
      'El establecimiento está CLAUSURADO; no admite almacenes ni terminales POS nuevos.'
    );
    expect(screen.getByRole('button', { name: 'Nuevo almacén' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Nuevo terminal' })).toBeDisabled();
  });

  it('permite las altas en remodelación y no muestra aviso', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'REMODELACION' })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    await screen.findByRole('heading', { name: 'Botica Central' });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo almacén' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Nuevo terminal' })).toBeEnabled();
  });
```

Run: `$env:CI='true'; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion --project erp-web`
Expected: FAIL (faltan `altas`, `Aviso` y la prop `motivoSinAltas`).

- [ ] **Step 2: Implementar las utilidades y el aviso**

`FE/lib/altas.ts`:

```ts
import type { EstadoEmpresa } from '../api/empresas.types';
import type { EstadoEstablecimiento } from '../api/establecimientos.types';

export function motivoSinAltasEmpresa(estado: EstadoEmpresa): string | null {
  return estado === 'ACTIVO'
    ? null
    : `La empresa está ${estado}; no admite establecimientos nuevos.`;
}

export function motivoSinAltasEstablecimiento(estado: EstadoEstablecimiento): string | null {
  return estado === 'SUSPENDIDO' || estado === 'CLAUSURADO'
    ? `El establecimiento está ${estado}; no admite almacenes ni terminales POS nuevos.`
    : null;
}
```

`FE/components/Aviso.tsx`:

```tsx
import type { PropsWithChildren } from 'react';

export function Aviso({ children }: PropsWithChildren) {
  return (
    <p
      role="status"
      className="border-warning-200 bg-warning-50 text-warning-800 dark:border-warning-800 dark:bg-warning-900/30 dark:text-warning-300 mt-6 rounded-lg border px-4 py-3 text-sm"
    >
      {children}
    </p>
  );
}
```

- [ ] **Step 3: Implementar las secciones**

En `FE/components/EstablecimientosSection.tsx`, reemplazar:

```tsx
export function EstablecimientosSection({ empresaId }: { empresaId: string }) {
```

por:

```tsx
export function EstablecimientosSection({
  empresaId,
  motivoSinAltas = null
}: {
  empresaId: string;
  motivoSinAltas?: string | null;
}) {
```

y reemplazar:

```tsx
        <Button onClick={() => setCreateOpen(true)}>Nuevo establecimiento</Button>
```

por:

```tsx
        <Button
          disabled={motivoSinAltas !== null}
          title={motivoSinAltas ?? undefined}
          onClick={() => setCreateOpen(true)}
        >
          Nuevo establecimiento
        </Button>
```

En `FE/components/AlmacenesSection.tsx`, reemplazar `export function AlmacenesSection({ establecimientoId }: { establecimientoId: string }) {` por:

```tsx
export function AlmacenesSection({
  establecimientoId,
  motivoSinAltas = null
}: {
  establecimientoId: string;
  motivoSinAltas?: string | null;
}) {
```

y `<Button onClick={() => setCreateOpen(true)}>Nuevo almacén</Button>` por:

```tsx
        <Button
          disabled={motivoSinAltas !== null}
          title={motivoSinAltas ?? undefined}
          onClick={() => setCreateOpen(true)}
        >
          Nuevo almacén
        </Button>
```

En `FE/components/TerminalesSection.tsx`, reemplazar `export function TerminalesSection({ establecimientoId }: { establecimientoId: string }) {` por:

```tsx
export function TerminalesSection({
  establecimientoId,
  motivoSinAltas = null
}: {
  establecimientoId: string;
  motivoSinAltas?: string | null;
}) {
```

y `<Button onClick={() => setCreateOpen(true)}>Nuevo terminal</Button>` por:

```tsx
        <Button
          disabled={motivoSinAltas !== null}
          title={motivoSinAltas ?? undefined}
          onClick={() => setCreateOpen(true)}
        >
          Nuevo terminal
        </Button>
```

- [ ] **Step 4: Implementar las páginas**

En `FE/pages/EmpresaDetailPage.tsx`: añadir `import { Aviso } from '../components/Aviso';` y `import { motivoSinAltasEmpresa } from '../lib/altas';`; justo debajo de `const empresa = result.data;` añadir `const motivoSinAltas = motivoSinAltasEmpresa(empresa.estado);`; reemplazar `<Card className="mt-6 p-6">` por:

```tsx
      {motivoSinAltas ? <Aviso>{motivoSinAltas}</Aviso> : null}

      <Card className="mt-6 p-6">
```

y `<EstablecimientosSection empresaId={empresaId} />` por `<EstablecimientosSection empresaId={empresaId} motivoSinAltas={motivoSinAltas} />`.

En `FE/pages/EstablecimientoDetailPage.tsx`: añadir `import { Aviso } from '../components/Aviso';` y `import { motivoSinAltasEstablecimiento } from '../lib/altas';`; justo debajo de `const establecimiento = result.data;` añadir `const motivoSinAltas = motivoSinAltasEstablecimiento(establecimiento.estadoOperativo);`; reemplazar `<Card className="mt-6 p-6">` por:

```tsx
      {motivoSinAltas ? <Aviso>{motivoSinAltas}</Aviso> : null}

      <Card className="mt-6 p-6">
```

y las dos secciones por:

```tsx
      <AlmacenesSection establecimientoId={establecimientoId} motivoSinAltas={motivoSinAltas} />
      <TerminalesSection establecimientoId={establecimientoId} motivoSinAltas={motivoSinAltas} />
```

- [ ] **Step 5: Ejecutar con cobertura completa**

Run (plantilla): `<tests>` = `apps/erp-web/src/features/organizacion`, `<archivos>` = `apps/erp-web/src/features/organizacion/{lib/altas.ts,components/Aviso.tsx,components/EstablecimientosSection.tsx,components/AlmacenesSection.tsx,components/TerminalesSection.tsx,pages/EmpresaDetailPage.tsx,pages/EstablecimientoDetailPage.tsx}`.
Expected: todos PASS y 100% en esos siete archivos.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps
git commit -m "feat(organizacion): avisar y bloquear las altas bajo un padre no operativo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Playwright en tres viewports y verificación final

**Files:**
- Modify: `e2e/organizacion.spec.ts`, `e2e/support/organizacion-api.ts`

**Interfaces:**
- Consumes: todo lo producido por las Tasks 1 a 6. Los RUC del e2e ya son válidos tras la Task 1. `abrirSesionEn`, `crearEmpresa` y `expectNoHorizontalOverflow` ya existen en el spec.
- Produces: cobertura e2e de RUC inválido, confirmación de estado, aviso con padre no operativo, refrigerado auto-marcado y serie repetida (`409` simulado), en desktop, tablet y móvil.

- [ ] **Step 1: Simular el `409` de series en el backend falso**

En `e2e/support/organizacion-api.ts`:

1. Añadir a `type Collection` la propiedad opcional:

```ts
  conflict?: (body: Row, rows: Record<string, Row[]>) => string | null;
```

2. En la entrada `'terminales-pos'` de `collections`, añadir:

```ts
    conflict: (body, rows) => {
      const empresaDe = (establecimientoId: unknown) =>
        rows.establecimientos?.find((establecimiento) => establecimiento.id === establecimientoId)
          ?.empresaId;
      const empresaId = empresaDe(body.establecimientoId);
      const delaEmpresa = (rows['terminales-pos'] ?? []).filter(
        (terminal) => empresaDe(terminal.establecimientoId) === empresaId
      );
      const repetida = [body.serieBoletaDefecto, body.serieFacturaDefecto].find(
        (serie) =>
          Boolean(serie) &&
          delaEmpresa.some(
            (terminal) => terminal.serieBoletaDefecto === serie || terminal.serieFacturaDefecto === serie
          )
      );
      return repetida ? `La serie ${String(repetida)} ya está asignada a otra caja de esta empresa.` : null;
    }
```

3. En el manejador `POST`, justo después del bloque que devuelve `409` por clave única, añadir:

```ts
      const conflicto = collection.conflict?.(body, rows);
      if (conflicto) return json(route, 409, { title: 'Conflict', detail: conflicto });
```

- [ ] **Step 2: Actualizar y ampliar el spec**

En `e2e/organizacion.spec.ts`:

1. Añadir este test dentro del `describe('Organización', ...)`, después de `cancela la creación de una empresa sin guardarla`:

```ts
  test('rechaza un RUC con dígito verificador inválido sin cerrar el formulario', async ({
    page
  }) => {
    await abrirSesionEn(page, '/organizacion/empresas');

    await crearEmpresa(page, '20123456789', 'Empresa con RUC inválido');

    await expect(page.getByRole('dialog').getByRole('alert')).toContainText(
      'El RUC no es válido: el dígito verificador no coincide.'
    );
    await expect(page.getByRole('dialog')).toBeVisible();
  });
```

2. En el flujo completo (`gestiona empresa, establecimiento, almacén y terminal de punta a punta`), reemplazar el bloque de cambio de estado de la empresa:

```ts
    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('SUSPENDIDO');
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('SUSPENDIDO', { exact: true })).toBeVisible();
```

por:

```ts
    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('SUSPENDIDO');
    await expect(page.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
    await page.getByLabel('Entiendo las consecuencias').check();
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByText('SUSPENDIDO', { exact: true })).toBeVisible();
    await expect(page.getByRole('status')).toContainText('no admite establecimientos nuevos');
    await expect(page.getByRole('button', { name: 'Nuevo establecimiento' })).toBeDisabled();

    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByRole('dialog').getByLabel('Estado').selectOption('ACTIVO');
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(page.getByRole('status')).toBeHidden();
```

3. En el mismo flujo, después de `await nuevoAlmacen.getByLabel('Tipo de almacén').selectOption('REFRIGERADO');` añadir:

```ts
    await expect(nuevoAlmacen.getByLabel('Controla temperatura')).toBeChecked();
```

4. Al final del flujo, justo antes del último `await expectNoHorizontalOverflow(page);`, añadir:

```ts
    await page.getByRole('button', { name: 'Nuevo terminal' }).click();
    const terminalRepetido = page.getByRole('dialog');
    await terminalRepetido.getByLabel('Código', { exact: true }).fill('UI-POS2');
    await terminalRepetido.getByLabel('Nombre', { exact: true }).fill('Caja UI 2');
    await terminalRepetido.getByLabel('Serie de boleta').fill('b001');
    await terminalRepetido.getByLabel('Serie de factura').fill('f002');
    await terminalRepetido.getByRole('button', { name: 'Crear terminal' }).click();
    await expect(terminalRepetido.getByRole('alert')).toContainText(
      'La serie B001 ya está asignada a otra caja de esta empresa.'
    );
    await terminalRepetido.getByRole('button', { name: 'Cerrar' }).click();
    await expect(page.getByRole('dialog')).toBeHidden();
```

- [ ] **Step 3: Ejecutar Playwright en los tres viewports**

Puerto 3000 libre (Playwright levanta su propio servidor en modo `http`; si hay otro servidor en ese puerto, detenerlo antes).

Run: `Remove-Item Env:CI -ErrorAction SilentlyContinue; pnpm.cmd exec playwright test --reporter=line`
Expected: todos los casos PASS en `desktop`, `tablet` y `mobile` (los de `profile-dropdown` incluidos). El flujo completo tiene un timeout propio de 90 s.

- [ ] **Step 4: Gate completo del frontend**

Run: `$env:CI='true'; pnpm.cmd lint; pnpm.cmd typecheck; pnpm.cmd build`
Expected: lint sin errores (las advertencias existentes no cuentan), typecheck y build sin errores.

Run: `$env:CI='true'; pnpm.cmd exec vitest run --maxWorkers=2 --coverage`
Expected: todos los archivos de test PASS con el gate de cobertura por archivo. Con concurrencia por defecto algunos tests de otras features (`catalogo`, `seguridad`) pueden agotar los 5 s de timeout por la carga de la máquina; eso ya ocurría antes de este plan y no es un fallo de estos cambios — se comprueba repitiendo con `--maxWorkers=2`.

- [ ] **Step 5: Commit**

```bash
git add frontend/e2e
git commit -m "test(e2e): cubrir RUC invalido, confirmacion de estado, avisos y series repetidas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 6: Verificación manual en el navegador real (solo después de desplegar el backend del Plan A)**

Requiere que el backend ya tenga las reglas del Plan A desplegadas y que la base no tenga series duplicadas (ver Task 5, paso 7 del Plan A). Con Vite en `http://localhost:3000` contra el backend real, recorrer: crear empresa con RUC `20123456789` (debe rechazarse en el formulario), crear otra con `20123456786`, suspenderla (aviso y confirmación, botón `Nuevo establecimiento` deshabilitado), volver a activarla, crear un establecimiento, crear un almacén refrigerado (la casilla se marca sola), crear dos cajas con la misma serie (la segunda debe mostrar el `409` con el mensaje de serie repetida) y clausurar el establecimiento (aviso y botones `Nuevo …` deshabilitados). Anotar cualquier diferencia con lo esperado.

---

## Self-review

**Cobertura del spec (Plan B):**
- B1 RUC → Task 1.
- B2 teléfono y sitio web, y enlaces → Task 2.
- B3 cambio de estado con confirmación y título con el nombre → Task 5 (el botón ya se deshabilitaba si el estado no cambia; se conserva y se prueba).
- B4 almacén coherente → Task 4.
- B5 series en mayúscula → Task 3.
- B6 padre no operativo → Task 6.
- Pruebas (unitarias al 100% por archivo nuevo, Playwright en tres viewports, verificación en navegador real) → Tasks 1 a 7.

**Consistencia de tipos y nombres:** `motivoSinAltas` es la misma prop (`string | null`) en las tres secciones y se calcula con `motivoSinAltasEmpresa` / `motivoSinAltasEstablecimiento`; `CAMPO_TEXTO.mayusculas` se usa igual en `CamposTexto` y `TerminalForm`; los mensajes del almacén son idénticos a los del backend (Plan A); `role="status"` del `Aviso` es el que consultan los tests y el e2e.

**Riesgos conocidos:**
- Las pruebas que dependen del comportamiento de react-hook-form con `setValueAs` (Task 3) y de las refinaciones entre campos de zod 4 (Task 4) se verifican con los tests del propio plan; si alguno falla por una diferencia de versión, corregir la implementación, no relajar el test.
- La Task 7, paso 6, depende de desplegar antes el backend del Plan A y de limpiar las series duplicadas de la base local, con confirmación del usuario.
