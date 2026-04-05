import type { QuotaInfo } from '@/types/quota';
import type { Tenant } from '@/types/tenant';
import apiClient from './client';

// 쿼타 사용량 조회
export const getQuotaUsage = async (tenantId: string): Promise<QuotaInfo> => {
  const { data } = await apiClient.get<QuotaInfo>(`/tenant/${tenantId}/quota`);
  return data;
};

// 쿼타 수정 (응답은 테넌트 정보)
export const updateQuota = async (
  tenantId: string,
  data: Partial<{ quotaDaily: number; quotaMonthly: number }>,
): Promise<Tenant> => {
  const { data: result } = await apiClient.patch<Tenant>(`/tenant/${tenantId}/quota`, data);
  return result;
};
