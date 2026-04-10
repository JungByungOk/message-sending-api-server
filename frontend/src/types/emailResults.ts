export type EmailResultStatus =
  | 'Queued'      // 발송 큐 진입
  | 'Sending'     // SES API 호출 완료
  | 'Delayed'     // 일시적 전달 지연
  | 'Delivered'   // 수신 MTA 전달 확인
  | 'Bounced'     // 반송
  | 'Complained'  // 수신자 스팸 신고
  | 'Rejected'    // SES 발송 거부
  | 'Error'       // 시스템 오류
  | 'Blocked'     // 내부 차단
  | 'Timeout';    // 타임아웃

export interface EmailResult {
  emailSendDtlSeq: number;
  emailSendSeq: number;
  sendStsCd: EmailResultStatus;
  sendRsltTypCd: string | null;
  rcvEmailAddr: string;
  sendEmailAddr: string;
  emailTitle: string | null;
  emailTmpletId: string | null;
  correlationId: string | null;
  sesMessageId: string | null;
  sesRealSendDt: string | null;
  stmFirRegDt: string;
  stmLastUpdDt: string;
}

export interface EmailResultMaster {
  emailSendSeq: number;
  emailTypCd: string | null;
  emailClsCd: string | null;
  sendDivCd: string | null;
  rsvSendDt: string | null;
  tenantId: string;
  stmFirRegDt: string;
  stmLastUpdDt: string;
  details: EmailResult[];
}

export interface EmailResultsParams {
  tenantId?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface EmailResultsResponse {
  totalCount: number;
  results: EmailResult[];
}
