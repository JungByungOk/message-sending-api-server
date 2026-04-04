import type {
  DeleteTemplateRequest,
  EmailTemplate,
  SendEmailRequest,
  SendEmailResponse,
  SendTemplatedEmailRequest,
  TemplateMetadata,
  TemplateResponse,
} from '@/types/email';
import apiClient from './client';

// 일반 이메일 발송
export const sendEmail = async (payload: SendEmailRequest): Promise<SendEmailResponse> => {
  const { data } = await apiClient.post<SendEmailResponse>('/ses/text-mail', payload);
  return data;
};

// 템플릿 이메일 발송
export const sendTemplatedEmail = async (
  payload: SendTemplatedEmailRequest,
): Promise<SendEmailResponse> => {
  const { data } = await apiClient.post<SendEmailResponse>('/ses/templated-mail', payload);
  return data;
};

// 템플릿 목록 조회
export const getTemplates = async (): Promise<TemplateMetadata[]> => {
  const { data } = await apiClient.get<TemplateMetadata[]>('/ses/templates');
  return data;
};

// 템플릿 생성
export const createTemplate = async (payload: EmailTemplate): Promise<TemplateResponse> => {
  const { data } = await apiClient.post<TemplateResponse>('/ses/template', payload);
  return data;
};

// 템플릿 수정
export const updateTemplate = async (payload: EmailTemplate): Promise<TemplateResponse> => {
  const { data } = await apiClient.patch<TemplateResponse>('/ses/template', payload);
  return data;
};

// 템플릿 삭제
export const deleteTemplate = async (payload: DeleteTemplateRequest): Promise<void> => {
  await apiClient.delete('/ses/template', { data: payload });
};
