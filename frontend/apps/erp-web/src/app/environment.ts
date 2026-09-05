import { z } from 'zod';

const environmentSchema = z.object({
  VITE_API_BASE_URL: z.string().min(1).default('/api/v1'),
  VITE_API_MODE: z.enum(['mock', 'http']).default('http')
});

export const environment = environmentSchema.parse(import.meta.env);
