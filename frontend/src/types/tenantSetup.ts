import type { Tenant } from './tenant';

export interface TenantSetupStartRequest {
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

export interface TenantSetupResult {
  tenant: Tenant;
  dkimRecords: DkimRecordsDTO | null;
}

export interface TenantSetupStep {
  step: number;
  name: string;
  status: 'COMPLETED' | 'WAITING' | 'PENDING';
}

export interface EmailVerificationStatus {
  email: string;
  verificationStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
}

export interface TenantSetupStatus {
  tenantId: string;
  domain: string;
  steps: TenantSetupStep[];
  verificationStatus: string;
  tenantStatus: string;
}
