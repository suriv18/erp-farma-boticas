import { renderHook } from '@testing-library/react';
import { useClaveIdempotencia } from './use-clave-idempotencia';

describe('useClaveIdempotencia', () => {
  it('reutiliza la clave para un payload idéntico', () => {
    const { result } = renderHook(() => useClaveIdempotencia());

    const primera = result.current({ cantidad: 1 });

    expect(result.current({ cantidad: 1 })).toBe(primera);
  });

  it('genera otra clave cuando el payload cambia', () => {
    const { result } = renderHook(() => useClaveIdempotencia());

    const primera = result.current({ cantidad: 1 });

    expect(result.current({ cantidad: 2 })).not.toBe(primera);
  });

  it('al volver al primer payload tras un cambio genera una clave nueva', () => {
    const { result } = renderHook(() => useClaveIdempotencia());

    const primera = result.current({ cantidad: 1 });
    result.current({ cantidad: 2 });

    expect(result.current({ cantidad: 1 })).not.toBe(primera);
  });
});
