import { changePasswordSchema } from './change-password.schema';

const valid = { currentPassword: 'Actual2026!', newPassword: 'Nueva2026!', confirmPassword: 'Nueva2026!' };

describe('changePasswordSchema', () => {
  it('acepta una nueva contraseña confirmada y distinta de la actual', () => {
    expect(changePasswordSchema.safeParse(valid).success).toBe(true);
  });

  it('exige la contraseña actual', () => {
    const result = changePasswordSchema.safeParse({ ...valid, currentPassword: '' });
    expect(result.error?.issues[0]?.message).toBe('Ingresa tu contraseña actual.');
  });

  it('exige un minimo de 8 caracteres', () => {
    const result = changePasswordSchema.safeParse({ ...valid, newPassword: 'corta', confirmPassword: 'corta' });
    expect(result.error?.issues[0]?.message).toBe('La nueva contraseña debe tener al menos 8 caracteres.');
  });

  it('rechaza una confirmacion distinta', () => {
    const result = changePasswordSchema.safeParse({ ...valid, confirmPassword: 'Otra2026!' });
    expect(result.error?.issues[0]).toMatchObject({ message: 'Las contraseñas no coinciden.', path: ['confirmPassword'] });
  });

  it('rechaza una nueva contraseña igual a la actual', () => {
    const result = changePasswordSchema.safeParse({
      currentPassword: 'Misma2026!',
      newPassword: 'Misma2026!',
      confirmPassword: 'Misma2026!'
    });
    expect(result.error?.issues[0]).toMatchObject({
      message: 'La nueva contraseña debe ser distinta de la actual.',
      path: ['newPassword']
    });
  });
});
