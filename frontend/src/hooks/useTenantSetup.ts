import { useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  activateTenant,
  getDkimRecords,
  getEmailVerificationStatus,
  getSetupStatus,
  resendVerification,
  startSetup,
  verifyEmail,
} from '@/api/tenantSetup';
import type { TenantSetupStartRequest } from '@/types/tenantSetup';

// 설정 상태 조회 훅 (PENDING 상태일 때 10초마다 자동 갱신)
export const useSetupStatus = (tenantId: string) => {
  return useQuery({
    queryKey: ['tenant-setup', 'status', tenantId],
    queryFn: () => getSetupStatus(tenantId),
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
    queryKey: ['tenant-setup', 'dkim', tenantId],
    queryFn: () => getDkimRecords(tenantId),
    enabled: !!tenantId,
  });
};

// 테넌트 초기 설정 시작 뮤테이션 훅
export const useStartSetup = () => {
  return useMutation({
    mutationFn: (data: TenantSetupStartRequest) => startSetup(data),
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

// 이메일 인증 상태 조회 훅 (PENDING 상태일 때 5초마다 자동 갱신, 5분 타임아웃)
const EMAIL_VERIFICATION_TIMEOUT_MS = 5 * 60 * 1000;

export const useEmailVerificationStatus = (tenantId: string, email: string) => {
  const pollingStartRef = useRef<number | null>(null);
  const isTimedOutRef = useRef(false);

  // email이 변경되면 타이머 리셋
  useEffect(() => {
    pollingStartRef.current = null;
    isTimedOutRef.current = false;
  }, [tenantId, email]);

  const query = useQuery({
    queryKey: ['tenant-setup', 'email-status', tenantId, email],
    queryFn: () => getEmailVerificationStatus(tenantId, email),
    enabled: !!tenantId && !!email,
    retry: 2,
    refetchInterval: (q) => {
      const data = q.state.data;
      if (data?.verificationStatus === 'PENDING') {
        if (!pollingStartRef.current) {
          pollingStartRef.current = Date.now();
        }
        if (Date.now() - pollingStartRef.current > EMAIL_VERIFICATION_TIMEOUT_MS) {
          isTimedOutRef.current = true;
          return false;
        }
        return 5000;
      }
      return false;
    },
  });

  return {
    ...query,
    isTimedOut: isTimedOutRef.current && query.data?.verificationStatus === 'PENDING',
    resetTimeout: () => {
      pollingStartRef.current = null;
      isTimedOutRef.current = false;
    },
  };
};

// 인증 이메일 재발송 뮤테이션 훅
export const useResendVerification = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ tenantId, email }: { tenantId: string; email: string }) =>
      resendVerification(tenantId, email),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({
        queryKey: ['tenant-setup', 'email-status', variables.tenantId, variables.email],
      });
    },
  });
};
