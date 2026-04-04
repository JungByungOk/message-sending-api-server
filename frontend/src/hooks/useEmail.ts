import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import {
  createTemplate,
  deleteTemplate,
  getTemplates,
  sendEmail,
  sendTemplatedEmail,
  updateTemplate,
} from '@/api/email';
import type { DeleteTemplateRequest, EmailTemplate, SendEmailRequest, SendTemplatedEmailRequest } from '@/types/email';

// 템플릿 목록 조회 훅
export const useTemplates = () => {
  return useQuery({
    queryKey: ['templates'],
    queryFn: getTemplates,
  });
};

// 템플릿 생성 뮤테이션 훅
export const useCreateTemplate = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: EmailTemplate) => createTemplate(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['templates'] });
      void message.success('템플릿이 생성되었습니다.');
    },
  });
};

// 템플릿 수정 뮤테이션 훅
export const useUpdateTemplate = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: EmailTemplate) => updateTemplate(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['templates'] });
      void message.success('템플릿이 수정되었습니다.');
    },
  });
};

// 템플릿 삭제 뮤테이션 훅
export const useDeleteTemplate = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: DeleteTemplateRequest) => deleteTemplate(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['templates'] });
      void message.success('템플릿이 삭제되었습니다.');
    },
  });
};

// 일반 이메일 발송 뮤테이션 훅
export const useSendEmail = () => {
  return useMutation({
    mutationFn: (payload: SendEmailRequest) => sendEmail(payload),
    onSuccess: () => {
      void message.success('이메일 발송 완료');
    },
  });
};

// 템플릿 이메일 발송 뮤테이션 훅
export const useSendTemplatedEmail = () => {
  return useMutation({
    mutationFn: (payload: SendTemplatedEmailRequest) => sendTemplatedEmail(payload),
    onSuccess: () => {
      void message.success('템플릿 이메일 발송 완료');
    },
  });
};
