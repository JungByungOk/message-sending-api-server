import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getSuppressions, removeSuppression } from '@/api/suppression';
import type { PageParams } from '@/types/api';

// 수신 거부 목록 조회 훅
export const useSuppressions = (tenantId: string, params?: PageParams) => {
  return useQuery({
    queryKey: ['suppressions', tenantId, params],
    queryFn: () => getSuppressions(tenantId, params),
    enabled: !!tenantId,
  });
};

// 수신 거부 삭제 뮤테이션 훅
export const useRemoveSuppression = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tenantId, email }: { tenantId: string; email: string }) =>
      removeSuppression(tenantId, email),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['suppressions'] });
    },
  });
};
