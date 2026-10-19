export type AuthHooks = {
  getAccessToken: () => string | null;
  onUnauthorized: () => Promise<string | null>;
};
