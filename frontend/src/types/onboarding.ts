import type { Tenant } from './tenant';

export interface OnboardingStartRequest {
  tenantName: string;
  domain: string;
  contactEmail?: string;
}

export interface DkimRecord {
  name: string;
  type: string;
  value: string;
}

export interface DkimRecordsDTO {
  domain: string;
  verificationStatus: string;
  dkimRecords: DkimRecord[];
}

export interface OnboardingResult {
  tenant: Tenant;
  dkimRecords: DkimRecordsDTO | null;
}

export interface OnboardingStep {
  step: number;
  name: string;
  status: 'COMPLETED' | 'WAITING' | 'PENDING';
}

export interface OnboardingStatus {
  tenantId: string;
  domain: string;
  steps: OnboardingStep[];
  verificationStatus: string;
  tenantStatus: string;
}
