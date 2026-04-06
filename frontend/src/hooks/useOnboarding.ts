import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  activateTenant,
  getDkimRecords,
  getEmailVerificationStatus,
  getOnboardingStatus,
  resendVerification,
  startOnboarding,
  verifyEmail,
} from '@/api/onboarding';
import type { OnboardingStartRequest } from '@/types/onboarding';

// 온보딩 상태 조회 훅 (PENDING 상태일 때 10초마다 자동 갱신)
export const useOnboardingStatus = (tenantId: string) => {
  return useQuery({
    queryKey: ['onboarding', 'status', tenantId],
    queryFn: () => getOnboardingStatus(tenantId),
    enabled: !!tenantId,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.verificationStatus === 'PENDING') return 10000;
      return false;
    },
  });
};

// DKIM 레코드 조회 훅
export const useDkimRecords = (tenantId: string) => {
  return useQuery({
    queryKey: ['onboarding', 'dkim', tenantId],
    queryFn: () => getDkimRecords(tenantId),
    enabled: !!tenantId,
  });
};

// 온보딩 시작 뮤테이션 훅
export const useStartOnboarding = () => {
  return useMutation({
    mutationFn: (data: OnboardingStartRequest) => startOnboarding(data),
  });
};

// 테넌트 활성화 뮤테이션 훅
export const useActivateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tenantId: string) => activateTenant(tenantId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

// 이메일 인증 요청 뮤테이션 훅
export const useVerifyEmail = () => {
  return useMutation({
    mutationFn: ({ tenantId, email }: { tenantId: string; email: string }) =>
      verifyEmail(tenantId, email),
  });
};

// 이메일 인증 상태 조회 훅 (PENDING 상태일 때 5초마다 자동 갱신)
export const useEmailVerificationStatus = (tenantId: string, email: string) => {
  return useQuery({
    queryKey: ['onboarding', 'email-status', tenantId, email],
    queryFn: () => getEmailVerificationStatus(tenantId, email),
    enabled: !!tenantId && !!email,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.verificationStatus === 'PENDING') return 5000;
      return false;
    },
  });
};

// 인증 이메일 재발송 뮤테이션 훅
export const useResendVerification = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tenantId, email }: { tenantId: string; email: string }) =>
      resendVerification(tenantId, email),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({
        queryKey: ['onboarding', 'email-status', variables.tenantId, variables.email],
      });
    },
  });
};
