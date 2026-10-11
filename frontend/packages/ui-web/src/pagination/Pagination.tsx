import { useEffect } from 'react';
import { Button } from '../button/Button';

export type PaginationProps = {
  page: number;
  size: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onSizeChange?: (size: number) => void;
  disabled?: boolean;
};

/** Controlled, zero-based pagination. The consumer owns fetching. */
export function Pagination({
  page,
  size,
  totalElements,
  onPageChange,
  onSizeChange,
  disabled = false
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const currentPage = Math.min(Math.max(0, page), totalPages - 1);
  useEffect(() => {
    if (!disabled && currentPage !== page) onPageChange(currentPage);
  }, [currentPage, disabled, onPageChange, page]);
  const from = totalElements === 0 ? 0 : currentPage * size + 1;
  const to = Math.min(totalElements, (currentPage + 1) * size);
  return (
    <nav
      aria-label="Paginación"
      className="flex flex-wrap items-center justify-between gap-3 border-t border-neutral-200 px-4 py-3 text-sm text-neutral-600 dark:border-neutral-800 dark:text-neutral-300"
    >
      <div className="flex flex-wrap items-center gap-4">
        {onSizeChange ? (
          <label className="flex items-center gap-2">
            Filas por página
            <select
              aria-label="Filas por página"
              value={size}
              disabled={disabled}
              onChange={(event) => {
                onSizeChange(Number(event.target.value));
                onPageChange(0);
              }}
              className="focus-visible:outline-primary-600 h-11 rounded-lg border border-neutral-200 bg-white px-2 focus-visible:outline-2 dark:border-neutral-700 dark:bg-neutral-900"
            >
              {[...new Set([20, 50, 100, size])]
                .sort((a, b) => a - b)
                .map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
            </select>
          </label>
        ) : null}
        <span role="status">
          {from}–{to} de {totalElements}
        </span>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <span>
          Página {currentPage + 1} de {totalPages}
        </span>
        <Button
          variant="secondary"
          disabled={disabled || currentPage <= 0}
          onClick={() => onPageChange(currentPage - 1)}
        >
          Anterior
        </Button>
        <Button
          variant="secondary"
          disabled={disabled || currentPage + 1 >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Siguiente
        </Button>
      </div>
    </nav>
  );
}
