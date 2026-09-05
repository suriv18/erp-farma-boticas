import { queryOptions } from '@tanstack/react-query';
import { apiClient } from '../../../app/api';

export type OrganizationalNode = {
  id: string;
  code: string;
  name: string;
  status: 'ACTIVE' | 'INACTIVE';
};

export type EstablishmentStructure = {
  id: string;
  code: string;
  name: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  timeZone: string;
  warehouses: OrganizationalNode[];
  cashRegisters: OrganizationalNode[];
};

export type CompanyStructure = {
  id: string;
  legalName: string;
  tradeName: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  establishments: EstablishmentStructure[];
};

export type CorporateStructure = {
  asOf: string;
  companies: CompanyStructure[];
};

export const corporateStructureQuery = queryOptions({
  queryKey: ['organization', 'corporate-structure'],
  queryFn: () => apiClient.get<CorporateStructure>('/estructura-corporativa')
});
