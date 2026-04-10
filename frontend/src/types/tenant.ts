export interface Tenant {
  tenantId: string;
  tenantName: string;
  domain: string;
  apiKey: string;
  configSetName: string | null;
  sesTenantName: string | null;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'FAILED';
  quotaDaily: number;
  quotaMonthly: number;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PAUSED';
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantRequest {
  tenantName: string;
  domain: string;
  quotaDaily?: number;
  quotaMonthly?: number;
}

export interface UpdateTenantRequest {
  tenantName?: string;
  quotaDaily?: number;
  quotaMonthly?: number;
}

export interface TenantListResponse {
  totalCount: number;
  tenants: Tenant[];
}

export interface TenantSender {
  id: number;
  tenantId: string;
  email: string;
  displayName: string | null;
  isDefault: boolean;
  createdAt: string;
}
