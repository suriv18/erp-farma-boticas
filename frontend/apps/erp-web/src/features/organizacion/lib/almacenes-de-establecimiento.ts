import type { EstablishmentStructure, OrganizationalNode } from '../api/organization.api';

export const almacenesDeEstablecimiento = (
  establecimientos: EstablishmentStructure[],
  establecimientoId: string
): OrganizationalNode[] =>
  establecimientos.find(({ id }) => id === establecimientoId)?.warehouses ?? [];
