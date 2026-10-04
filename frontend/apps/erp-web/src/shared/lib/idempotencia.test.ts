import { nuevaClaveIdempotencia } from './idempotencia';

describe('nuevaClaveIdempotencia', () => {
  it('genera un UUID distinto en cada llamada', () => {
    const primera = nuevaClaveIdempotencia();
    const segunda = nuevaClaveIdempotencia();

    expect(primera).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    expect(primera).not.toBe(segunda);
  });
});
