import apiClient from './client';
import type {
  BounceListResponse,
  BatchItem,
  CostEstimate,
  HourlyStat,
  MonitoringSummary,
  RecentCampaign,
  SesQuota,
  StatusStat,
  TenantReputation,
  TrendItem,
} from '@/types/monitoring';

export const monitoringApi = {
  getSummary: async () => {
    const { data } = await apiClient.get<MonitoringSummary>('/monitoring/summary');
    return data;
  },

  getHourlyStats: async (date?: string) => {
    const { data } = await apiClient.get<HourlyStat[]>('/monitoring/hourly', {
      params: date ? { date } : undefined,
    });
    return data;
  },

  getStatusSummary: async () => {
    const { data } = await apiClient.get<StatusStat[]>('/monitoring/status-summary');
    return data;
  },

  getBounceList: async (page = 1, size = 20) => {
    const { data } = await apiClient.get<BounceListResponse>('/monitoring/bounces', {
      params: { page, size },
    });
    return data;
  },

  getBatchList: async () => {
    const { data } = await apiClient.get<BatchItem[]>('/monitoring/batches');
    return data;
  },

  getRecentCampaign: async () => {
    const { data } = await apiClient.get<RecentCampaign>('/monitoring/recent-campaign');
    return data;
  },

  getTrend: async (period: 'weekly' | 'monthly' = 'weekly', count = 12) => {
    const { data } = await apiClient.get<TrendItem[]>('/monitoring/trend', {
      params: { period, count },
    });
    return data;
  },

  getCostEstimate: async (months = 6) => {
    const { data } = await apiClient.get<CostEstimate>('/monitoring/cost', {
      params: { months },
    });
    return data;
  },

  getTenantReputation: async () => {
    const { data } = await apiClient.get<TenantReputation[]>('/monitoring/tenant-reputation');
    return data;
  },

  getSesQuota: async () => {
    const { data } = await apiClient.get<SesQuota>('/monitoring/ses-quota');
    return data;
  },
};
