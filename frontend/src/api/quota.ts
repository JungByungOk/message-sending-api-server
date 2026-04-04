import type { QuotaInfo } from '@/types/quota';
import apiClient from './client';

// 쿼타 사용량 조회
export const getQuotaUsage = async (tenantId: string): Promise<QuotaInfo> => {
  const { data } = await apiClient.get<QuotaInfo>(`/tenant/${tenantId}/quota`);
  return data;
};

// 쿼타 수정
export const updateQuota = async (
  tenantId: string,
  data: Partial<{ daily: number; monthly: number }>,
): Promise<QuotaInfo> => {
  const { data: result } = await apiClient.patch<QuotaInfo>(`/tenant/${tenantId}/quota`, data);
  return result;
};
