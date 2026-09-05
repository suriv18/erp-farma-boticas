# SEC-FAR-002 — Roles, Permisos y Ámbitos

## 1. Modelo

```text
Usuario
 + Rol(es)
 + Permisos
 + Ámbito organizacional
 + Contexto de tienda/terminal
 + Competencia profesional
 = decisión de autorización
```

## 2. Roles candidatos

- Administrador de plataforma;
- Administrador corporativo;
- Compras;
- Almacén/recepción;
- Jefe de tienda;
- Cajero;
- Químico Farmacéutico / Director Técnico;
- Personal técnico;
- Finanzas/Contabilidad;
- Auditor;
- Farmacovigilancia;
- Soporte técnico.

Son **roles candidatos**, no nombres normativos universales.

## 3. Distinción esencial

Un permiso informático no sustituye competencia profesional:

```text
permission = dispensacion.confirmar
          +
actor profesional competente
          +
asignación vigente en establecimiento
          +
producto/tipo permitido
          ↓
AUTORIZAR
```

## 4. Scope

Ámbitos posibles:

- cadena/tenant;
- empresa;
- establecimiento;
- almacén;
- terminal/caja;
- dominio funcional.

Un cajero de Local A no accede por defecto a caja/ventas del Local B.
