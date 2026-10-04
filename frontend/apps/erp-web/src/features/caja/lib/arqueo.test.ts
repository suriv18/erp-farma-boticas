import { diferenciaArqueo } from './arqueo';

describe('diferenciaArqueo', () => {
  it('es declarado menos sistema redondeado a dos decimales', () => {
    expect(diferenciaArqueo(350, 348.5)).toBe(-1.5);
    expect(diferenciaArqueo(100, 100)).toBe(0);
    expect(diferenciaArqueo(0.1, 0.30000000000000004)).toBe(0.2);
  });
});
