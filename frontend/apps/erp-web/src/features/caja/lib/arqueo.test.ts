import { claseDiferencia, diferenciaArqueo } from './arqueo';

describe('diferenciaArqueo', () => {
  it('es declarado menos sistema redondeado a dos decimales', () => {
    expect(diferenciaArqueo(350, 348.5)).toBe(-1.5);
    expect(diferenciaArqueo(100, 100)).toBe(0);
    expect(diferenciaArqueo(0.1, 0.30000000000000004)).toBe(0.2);
  });
});

describe('claseDiferencia', () => {
  it('rojo cuando falta dinero, ámbar cuando sobra, verde cuando cuadra y neutro sin dato', () => {
    expect(claseDiferencia(-1.5)).toContain('text-danger-600');
    expect(claseDiferencia(2)).toContain('text-warning-700');
    expect(claseDiferencia(0)).toContain('text-success-700');
    expect(claseDiferencia(null)).toContain('text-neutral-900');
  });
});
