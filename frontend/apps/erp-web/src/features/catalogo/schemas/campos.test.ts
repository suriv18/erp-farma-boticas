import { decimal, fechaOpcional, textoMax, textoRango } from './campos';

describe('campos', () => {
  it('textoMax valida la longitud maxima con el mensaje de la etiqueta', () => {
    const schema = textoMax('La fuente', 3);

    expect(schema.safeParse('abc').success).toBe(true);
    expect(schema.safeParse('abcd').error?.issues[0]?.message).toBe(
      'La fuente no debe exceder 3 caracteres.'
    );
  });

  it('textoRango valida minimo y maximo despues de recortar', () => {
    const schema = textoRango('El nombre', 2, 4);

    expect(schema.safeParse(' ab ').success).toBe(true);
    expect(schema.safeParse(' a ').error?.issues[0]?.message).toBe(
      'El nombre debe tener al menos 2 caracteres.'
    );
    expect(schema.safeParse('abcde').error?.issues[0]?.message).toBe(
      'El nombre no debe exceder 4 caracteres.'
    );
  });

  it('fechaOpcional acepta vacio o formato ISO', () => {
    expect(fechaOpcional.safeParse('').success).toBe(true);
    expect(fechaOpcional.safeParse('2026-10-01').success).toBe(true);
    expect(fechaOpcional.safeParse('01/10/2026').error?.issues[0]?.message).toBe(
      'Ingresa una fecha válida.'
    );
  });

  it('decimal opcional positivo respeta enteros, decimales y exclusion de cero', () => {
    const schema = decimal({
      etiqueta: 'El peso',
      enteros: 2,
      decimales: 1,
      requerido: false,
      permiteCero: false
    });

    expect(schema.safeParse('').success).toBe(true);
    expect(schema.safeParse('12.5').success).toBe(true);
    expect(schema.safeParse('0').error?.issues[0]?.message).toBe(
      'El peso debe ser un número mayor que cero con hasta 1 decimales.'
    );
    expect(schema.safeParse('123').success).toBe(false);
    expect(schema.safeParse('1.25').success).toBe(false);
    expect(schema.safeParse('abc').success).toBe(false);
  });

  it('decimal requerido que permite cero rechaza vacio y acepta cero', () => {
    const schema = decimal({
      etiqueta: 'El stock mínimo',
      enteros: 3,
      decimales: 2,
      requerido: true,
      permiteCero: true
    });

    expect(schema.safeParse('0').success).toBe(true);
    expect(schema.safeParse('').error?.issues[0]?.message).toBe(
      'El stock mínimo debe ser un número mayor o igual a cero con hasta 2 decimales.'
    );
  });
});
