import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getQuotaUsage, updateQuota } from '@/api/quota';

// 쿼타 사용량 조회 훅
export const useQuotaUsage = (tenantId: string) => {
  return useQuery({
    queryKey: ['quota', tenantId],
    queryFn: () => getQuotaUsage(tenantId),
    enabled: !!tenantId,
  });
};

// 쿼타 수정 뮤테이션 훅
export const useUpdateQuota = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      tenantId,
      data,
    }: {
      tenantId: string;
      data: Partial<{ quotaDaily: number; quotaMonthly: number }>;
    }) => updateQuota(tenantId, data),
    onSuccess: (_result, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['quota', variables.tenantId] });
    },
  });
};
