export interface Tenant {
  tenantId: string;
  tenantName: string;
  domain: string;
  apiKey: string;
  configSetName: string | null;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'FAILED';
  quotaDaily: number;
  quotaMonthly: number;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
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
