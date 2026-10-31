export type QueryParams = Record<string, string | number | boolean | undefined>;

export function buildQueryString(params: QueryParams): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value));
  });
  return query.toString();
}

export function withQuery(path: string, params: QueryParams): string {
  const query = buildQueryString(params);
  return query ? `${path}?${query}` : path;
}
