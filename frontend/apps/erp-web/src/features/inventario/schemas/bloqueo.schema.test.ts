import { bloqueoSchema } from './bloqueo.schema';

describe('bloqueoSchema', () => {
  it('acepta un motivo válido', () => {
    expect(bloqueoSchema.safeParse({ motivo: 'Control de calidad' }).success).toBe(true);
  });

  it('exige motivo', () => {
    const result = bloqueoSchema.safeParse({ motivo: ' ' });

    expect(result.success ? [] : result.error.issues.map(({ message }) => message)).toEqual([
      'El motivo es obligatorio.'
    ]);
  });

  it('limita el motivo a 1000 caracteres', () => {
    expect(bloqueoSchema.safeParse({ motivo: 'x'.repeat(1001) }).success).toBe(false);
  });
});
