export type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};
