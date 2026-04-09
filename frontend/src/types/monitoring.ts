export interface MonitoringSummary {
  todaySentCount: number;
  todayDeliveredCount: number;
  todayBounceCount: number;
  todayComplaintCount: number;
  deliveryRate: number;
  bounceRate: number;
  runningBatchCount: number;
  pendingBatchCount: number;
}

export interface HourlyStat {
  hour: number;
  sentCount: number;
}

export interface StatusStat {
  status: string;
  count: number;
}

export interface BounceItem {
  id: number;
  email: string;
  fromEmail: string;
  subject: string;
  status: string;
  regDtm: string;
  tenantId: string;
}

export interface BounceListResponse {
  list: BounceItem[];
  total: number;
  page: number;
  size: number;
}

export interface BatchItem {
  batchId: string;
  templateName: string;
  fromAddr: string;
  jobName: string;
  status: string;
  totalCount: number;
  startDateAt: string;
}

export interface RecentCampaign {
  batchId: string | null;
  templateName: string | null;
  startDateAt: string | null;
  status: string | null;
  complete: boolean;
  totalSent: number;
  delivered: number;
  deliveryRate: number;
  opens: number;
  openRate: number;
  clicks: number;
  clickRate: number;
  complaints: number;
  complaintRate: number;
  prevDeliveryRate: number | null;
  prevOpenRate: number | null;
  prevClickRate: number | null;
  prevComplaintRate: number | null;
}

export interface TrendItem {
  periodLabel: string;
  periodStart: string;
  periodEnd: string;
  sentCount: number;
  deliveredCount: number;
  bounceCount: number;
  complaintCount: number;
  openCount: number;
  clickCount: number;
  deliveryRate: number;
  openRate: number;
  clickRate: number;
  complaintRate: number;
}

export interface CostMonthlyItem {
  month: string;
  totalSent: number;
  eventCount: number;
  deliveredCount: number;
  bounceCount: number;
  complaintCount: number;
  sesCost: number;
  lambdaCost: number;
  dynamoCost: number;
  sqsCost: number;
  snsCost: number;
  apiGwCost: number;
  totalCost: number;
}

export interface CostServiceInfo {
  name: string;
  description: string;
  pricing: string;
  cost: number;
}

export interface CostEstimate {
  monthlyBreakdown: CostMonthlyItem[];
  services: CostServiceInfo[];
  totalSent: number;
  totalCost: number;
  currency: string;
  note: string;
}

export interface TenantReputation {
  tenantId: string;
  totalSent: number;
  delivered: number;
  bounced: number;
  complained: number;
  deliveryRate: number;
  bounceRate: number;
  complaintRate: number;
}

export interface SesQuota {
  maxSendRate: number;
  max24HourSend: number;
  sentLast24Hours: number;
  remaining24Hours: number;
  productionAccess: boolean;
}
