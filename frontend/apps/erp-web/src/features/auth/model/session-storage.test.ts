import { clearRefreshToken, readRefreshToken, saveRefreshToken } from './session-storage';

describe('session-storage', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('guarda y lee el refresh token', () => {
    saveRefreshToken('token-abc');
    expect(readRefreshToken()).toBe('token-abc');
  });

  it('devuelve null cuando no hay token guardado', () => {
    expect(readRefreshToken()).toBeNull();
  });

  it('limpia el token guardado', () => {
    saveRefreshToken('token-abc');
    clearRefreshToken();
    expect(readRefreshToken()).toBeNull();
  });

  it('no lanza si sessionStorage.setItem falla', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });
    expect(() => saveRefreshToken('token-abc')).not.toThrow();
    spy.mockRestore();
  });
});
