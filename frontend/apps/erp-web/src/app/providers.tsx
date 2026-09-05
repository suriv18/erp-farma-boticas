import { QueryClientProvider } from '@tanstack/react-query';
import { useState, type PropsWithChildren } from 'react';
import { AuthSessionProvider } from '../features/auth';
import { createAppQueryClient } from './query-client';

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(createAppQueryClient);
  return (
    <AuthSessionProvider>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </AuthSessionProvider>
  );
}
