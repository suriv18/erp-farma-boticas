export type Sesion = {
  id: string;
  userId: string;
  provider: string | null;
  authMethod: string | null;
  channel: string;
  ipAddress: string | null;
  userAgent: string | null;
  deviceId: string | null;
  loginAt: string;
  lastUsedAt: string | null;
  expiresAt: string | null;
  logoutAt: string | null;
  revokedAt: string | null;
  revocationReason: string | null;
  status: string;
};
