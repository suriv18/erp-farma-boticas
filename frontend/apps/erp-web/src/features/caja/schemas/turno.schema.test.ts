import { abrirTurnoSchema, cerrarTurnoSchema } from './turno.schema';

const mensajes = (resultado: { success: boolean; error?: { issues: { message: string }[] } }) =>
  resultado.success ? [] : (resultado.error?.issues.map(({ message }) => message) ?? []);

describe('abrirTurnoSchema', () => {
  it.each(['0', '100', '100.5', '100.50'])('acepta el fondo %s', (fondoInicial) => {
    expect(abrirTurnoSchema.safeParse({ fondoInicial }).success).toBe(true);
  });

  it.each(['', '-1', '1.234', 'abc'])('rechaza el fondo "%s"', (fondoInicial) => {
    expect(mensajes(abrirTurnoSchema.safeParse({ fondoInicial }))).toEqual([
      'El fondo inicial debe ser un monto mayor o igual a cero con hasta 2 decimales.'
    ]);
  });
});

describe('cerrarTurnoSchema', () => {
  it('acepta un total declarado válido con o sin observación', () => {
    expect(cerrarTurnoSchema.safeParse({ totalDeclarado: '348.50', observacion: '' }).success).toBe(
      true
    );
    expect(
      cerrarTurnoSchema.safeParse({ totalDeclarado: '0', observacion: 'Todo cuadra' }).success
    ).toBe(true);
  });

  it('rechaza un total declarado inválido', () => {
    expect(mensajes(cerrarTurnoSchema.safeParse({ totalDeclarado: '', observacion: '' }))).toEqual([
      'El total declarado debe ser un monto mayor o igual a cero con hasta 2 decimales.'
    ]);
  });

  it('rechaza una observación de más de 1000 caracteres', () => {
    expect(
      mensajes(cerrarTurnoSchema.safeParse({ totalDeclarado: '1', observacion: 'x'.repeat(1001) }))
    ).toEqual(['La observación no debe exceder 1000 caracteres.']);
  });
});
