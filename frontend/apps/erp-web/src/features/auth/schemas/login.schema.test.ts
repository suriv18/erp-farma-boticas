import { describe, expect, it } from 'vitest';
import { loginSchema } from './login.schema';

const base = { password: 'Boticas2026!', remember: false };

describe('loginSchema', () => {
  it('acepta un nombre de usuario sin arroba', () => {
    expect(loginSchema.safeParse({ ...base, login: 'crit001.manana1' }).success).toBe(true);
  });

  it('acepta un correo válido', () => {
    expect(loginSchema.safeParse({ ...base, login: 'maria.rojas@boticas.pe' }).success).toBe(true);
  });

  it('rechaza un valor con arroba que no es un correo válido', () => {
    const resultado = loginSchema.safeParse({ ...base, login: 'maria@' });

    expect(resultado.success).toBe(false);
    expect(resultado.error?.issues[0]?.message).toBe('Ingresa un correo electrónico válido.');
  });

  it('rechaza un valor vacío', () => {
    const resultado = loginSchema.safeParse({ ...base, login: '   ' });

    expect(resultado.success).toBe(false);
    expect(resultado.error?.issues[0]?.message).toBe('Ingresa tu usuario o correo electrónico.');
  });

  it('exige una contraseña de al menos 8 caracteres', () => {
    expect(loginSchema.safeParse({ ...base, login: 'luis', password: 'corta' }).success).toBe(
      false
    );
  });
});
