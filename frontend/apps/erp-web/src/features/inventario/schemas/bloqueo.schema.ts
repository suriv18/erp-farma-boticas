import { z } from 'zod';
import { motivoRequerido } from './campos';

export const bloqueoSchema = z.object({ motivo: motivoRequerido });

export type BloqueoFormValues = z.infer<typeof bloqueoSchema>;
