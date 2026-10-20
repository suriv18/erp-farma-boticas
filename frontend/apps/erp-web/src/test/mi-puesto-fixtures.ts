import { http, HttpResponse } from 'msw';
import type { MiPuesto } from '../features/organizacion/api/mi-puesto.types';

export const sampleMiPuesto: MiPuesto = {
  establecimientos: [
    {
      id: 'est-1',
      codigo: 'EST001',
      nombre: 'Botica Central',
      almacenes: [
        { id: 'alm-1', codigo: 'ALM001', nombre: 'Almacén Central', tipo: 'GENERAL', permiteVenta: true },
        { id: 'alm-2', codigo: 'ALM002', nombre: 'Almacén Frío', tipo: 'REFRIGERADO', permiteVenta: true },
        { id: 'alm-9', codigo: 'ALM009', nombre: 'Almacén Merma', tipo: 'MERMA', permiteVenta: false }
      ],
      terminales: [
        { id: 'term-1', codigo: 'POS001', nombre: 'Caja 1', estado: 'ACTIVO', operable: true },
        { id: 'term-2', codigo: 'POS002', nombre: 'Caja 2', estado: 'ACTIVO', operable: true }
      ]
    },
    {
      id: 'est-2',
      codigo: 'EST002',
      nombre: 'Botica Norte',
      almacenes: [
        { id: 'alm-3', codigo: 'ALM003', nombre: 'Almacén Norte', tipo: 'GENERAL', permiteVenta: true }
      ],
      terminales: [
        { id: 'term-3', codigo: 'POS003', nombre: 'Caja 3', estado: 'MANTENIMIENTO', operable: false }
      ]
    }
  ]
};

export const miPuestoHandler = (puesto: MiPuesto = sampleMiPuesto) =>
  http.get('*/api/v1/organizacion/mi-puesto', () => HttpResponse.json(puesto));
