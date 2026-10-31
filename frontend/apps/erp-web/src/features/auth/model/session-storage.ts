const REFRESH_TOKEN_KEY = 'erp-botica.refreshToken';

export function saveRefreshToken(token: string): void {
  try {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
  } catch {
    // sessionStorage puede no estar disponible (modo privado, cuota excedida).
    // La sesion seguira funcionando en memoria durante esta pestana.
  }
}

export function readRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function clearRefreshToken(): void {
  try {
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  } catch {
    // Ignorado: si no se pudo leer/escribir, tampoco hay nada que limpiar de forma fiable.
  }
}
