import type { FieldErrors } from 'react-hook-form';

export function FormErrorSummary({ errors, prefix }: { errors: FieldErrors; prefix: string }) {
  const entries = Object.entries(errors).flatMap(([name, error]) =>
    typeof error?.message === 'string' ? [[name, error.message] as const] : []
  );
  if (!entries.length) return null;
  return (
    <div
      role="alert"
      className="border-danger-300 text-danger-700 dark:text-danger-300 rounded-xl border p-4 text-sm"
    >
      <p className="font-semibold">Revisa los siguientes campos antes de guardar:</p>
      <ul className="mt-2 list-inside list-disc">
        {entries.map(([name, message]) => (
          <li key={name}>
            <a
              className="underline"
              href={`#${prefix}-${name}`}
              onClick={(event) => {
                event.preventDefault();
                document.getElementById(`${prefix}-${name}`)?.focus();
              }}
            >
              {message}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
