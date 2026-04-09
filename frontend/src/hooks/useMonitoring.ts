import { useQuery } from '@tanstack/react-query';
import { monitoringApi } from '@/api/monitoring';

export function useMonitoringSummary() {
  return useQuery({
    queryKey: ['monitoring', 'summary'],
    queryFn: monitoringApi.getSummary,
    refetchInterval: 30_000,
  });
}

export function useHourlyStats(date?: string) {
  return useQuery({
    queryKey: ['monitoring', 'hourly', date],
    queryFn: () => monitoringApi.getHourlyStats(date),
    refetchInterval: 60_000,
  });
}

export function useStatusSummary() {
  return useQuery({
    queryKey: ['monitoring', 'status-summary'],
    queryFn: monitoringApi.getStatusSummary,
    refetchInterval: 30_000,
  });
}

export function useBounceList(page: number, size: number) {
  return useQuery({
    queryKey: ['monitoring', 'bounces', page, size],
    queryFn: () => monitoringApi.getBounceList(page, size),
  });
}

export function useBatchList() {
  return useQuery({
    queryKey: ['monitoring', 'batches'],
    queryFn: monitoringApi.getBatchList,
    refetchInterval: 15_000,
  });
}

export function useTrend(period: 'weekly' | 'monthly' = 'weekly', count = 12) {
  return useQuery({
    queryKey: ['monitoring', 'trend', period, count],
    queryFn: () => monitoringApi.getTrend(period, count),
    refetchInterval: 60_000,
  });
}

export function useRecentCampaign() {
  return useQuery({
    queryKey: ['monitoring', 'recent-campaign'],
    queryFn: monitoringApi.getRecentCampaign,
    refetchInterval: 30_000,
  });
}

export function useCostEstimate(months = 6) {
  return useQuery({
    queryKey: ['monitoring', 'cost', months],
    queryFn: () => monitoringApi.getCostEstimate(months),
  });
}

export function useTenantReputation() {
  return useQuery({
    queryKey: ['monitoring', 'tenant-reputation'],
    queryFn: monitoringApi.getTenantReputation,
    refetchInterval: 60_000,
  });
}

export function useSesQuota() {
  return useQuery({
    queryKey: ['monitoring', 'ses-quota'],
    queryFn: monitoringApi.getSesQuota,
    refetchInterval: 30_000,
  });
}
