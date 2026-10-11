export function FormError({ message }: { message: string }) {
  return (
    <p
      role="alert"
      className="border-danger-200 bg-danger-50 text-danger-800 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300 rounded-lg border px-3 py-2 text-sm"
    >
      {message}
    </p>
  );
}
