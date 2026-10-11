import { anularOrdenSchema } from './anular-orden.schema';

const mensajeDe = (motivo: string) => {
  const resultado = anularOrdenSchema.safeParse({ motivo });
  return resultado.success ? undefined : resultado.error.issues[0]?.message;
};

describe('anularOrdenSchema', () => {
  it('exige un motivo', () => {
    expect(mensajeDe('   ')).toBe('El motivo es obligatorio.');
  });

  it('limita el motivo a 300 caracteres', () => {
    expect(mensajeDe('a'.repeat(301))).toBe('El motivo no debe exceder 300 caracteres.');
    expect(mensajeDe('Error de digitación')).toBeUndefined();
  });
});
