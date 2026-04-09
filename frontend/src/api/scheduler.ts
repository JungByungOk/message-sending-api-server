import type {
  AllJobsResponse,
  JobControlRequest,
  ScheduleJobRequest,
  ScheduleResponse,
} from '@/types/scheduler';
import apiClient from './client';

// 스케줄 작업 생성
export const createJob = async (
  payload: ScheduleJobRequest,
  tenantApiKey?: string,
): Promise<ScheduleResponse> => {
  const headers = tenantApiKey ? { Authorization: `Bearer ${tenantApiKey}` } : undefined;
  const { data } = await apiClient.post<ScheduleResponse>('/scheduler/job', payload, { headers });
  return data;
};

// 전체 작업 목록 조회
export const getJobs = async (): Promise<AllJobsResponse> => {
  const { data } = await apiClient.get<AllJobsResponse>('/scheduler/jobs');
  return data;
};

// 작업 일시 정지
export const pauseJob = async (payload: JobControlRequest): Promise<ScheduleResponse> => {
  const { data } = await apiClient.put<ScheduleResponse>('/scheduler/job/pause', payload);
  return data;
};

// 작업 재개
export const resumeJob = async (payload: JobControlRequest): Promise<ScheduleResponse> => {
  const { data } = await apiClient.put<ScheduleResponse>('/scheduler/job/resume', payload);
  return data;
};

// 작업 중지
export const stopJob = async (payload: JobControlRequest): Promise<ScheduleResponse> => {
  const { data } = await apiClient.put<ScheduleResponse>('/scheduler/job/stop', payload);
  return data;
};

// 작업 삭제
export const deleteJob = async (payload: JobControlRequest): Promise<ScheduleResponse> => {
  const { data } = await apiClient.delete<ScheduleResponse>('/scheduler/job', { data: payload });
  return data;
};

// 전체 작업 삭제
export const deleteAllJobs = async (): Promise<ScheduleResponse> => {
  const { data } = await apiClient.delete<ScheduleResponse>('/scheduler/job/all');
  return data;
};
