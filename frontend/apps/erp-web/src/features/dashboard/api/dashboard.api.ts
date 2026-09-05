import { queryOptions } from '@tanstack/react-query';
import { apiClient } from '../../../app/api';

export type DashboardSummary = {
  salesToday: number;
  transactionsToday: number;
  stockUnits: number;
  lowStockProducts: number;
  expiringLots: number;
  activeCustomers: number;
  asOf: string;
};

export const dashboardSummaryQuery = queryOptions({
  queryKey: ['dashboard', 'summary'],
  queryFn: () => apiClient.get<DashboardSummary>('/dashboard/summary')
});
