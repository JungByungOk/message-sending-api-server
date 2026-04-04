export interface TemplatedEmail {
  id?: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  templateParameters: Record<string, string>;
}

export interface ScheduleJobRequest {
  jobName: string;
  jobGroup?: string;
  description?: string;
  startDateAt?: string;
  templateName: string;
  from: string;
  templatedEmailList: TemplatedEmail[];
  tags?: Array<{ name: string; value: string }>;
}

export interface JobControlRequest {
  jobName: string;
  jobGroup?: string;
}

export interface JobInfo {
  jobName: string;
  groupName: string;
  jobStatus: 'RUNNING' | 'SCHEDULED' | 'PAUSED' | 'COMPLETE';
  scheduleTime: string;
  lastFiredTime: string | null;
  nextFireTime: string | null;
}

export interface AllJobsResponse {
  numOfAllJobs: number;
  numOfGroups: number;
  numOfRunningJobs: number;
  jobs: JobInfo[];
}

export interface ScheduleResponse {
  success: boolean;
  message: string;
}
