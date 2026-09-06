export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessExpiresAt: string;
  refreshExpiresAt: string;
  tenantId: string;
  userId: string;
  sessionId: string;
  passwordChangeRequired: boolean;
};

export type LoginRequest = {
  tenantId: string;
  login: string;
  password: string;
};
