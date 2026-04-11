import { Tag } from 'antd';
import {
  CheckCircleFilled,
  ClockCircleFilled,
  CloseCircleFilled,
  ExclamationCircleFilled,
  MinusCircleFilled,
  PlayCircleFilled,
  PauseCircleFilled,
  StopFilled,
} from '@ant-design/icons';
import type { CSSProperties } from 'react';
import type { EmailResultStatus } from '@/types/emailResults';

// ─── Adobe Spectrum 스타일 색상 팔레트 ─────────────────────────────────────────
const palette = {
  positive: { bg: '#ECFDF3', border: '#ABEFC6', text: '#067647', icon: '#12B76A' },
  info:     { bg: '#EFF8FF', border: '#B2DDFF', text: '#175CD3', icon: '#2E90FA' },
  warning:  { bg: '#FFFAEB', border: '#FEDF89', text: '#B54708', icon: '#F79009' },
  negative: { bg: '#FEF3F2', border: '#FECDCA', text: '#B42318', icon: '#F04438' },
  neutral:  { bg: '#F2F4F7', border: '#E4E7EC', text: '#344054', icon: '#667085' },
} as const;

type PaletteKey = keyof typeof palette;

// ─── 테넌트 상태 ──────────────────────────────────────────────────────────────
type TenantStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PAUSED';
type VerificationStatus = 'SUCCESS' | 'PENDING' | 'FAILED';
type JobStatus = 'RUNNING' | 'SCHEDULED' | 'PAUSED' | 'COMPLETE';
type SuppressionReason = 'BOUNCE' | 'COMPLAINT';
interface StatusTagProps {
  type: 'tenant' | 'verification' | 'job' | 'suppression' | 'emailResult';
  status: string;
  style?: CSSProperties;
}

interface TagConfig {
  palette: PaletteKey;
  label: string;
  icon: React.ReactNode;
}

const tenantConfig: Record<TenantStatus, TagConfig> = {
  ACTIVE:    { palette: 'positive', label: '활성',    icon: <CheckCircleFilled /> },
  INACTIVE:  { palette: 'neutral',  label: '비활성',  icon: <MinusCircleFilled /> },
  SUSPENDED: { palette: 'negative', label: '정지',    icon: <StopFilled /> },
  PAUSED:    { palette: 'warning',  label: '일시정지', icon: <PauseCircleFilled /> },
};

const verificationConfig: Record<VerificationStatus, TagConfig> = {
  SUCCESS: { palette: 'positive', label: '인증 완료', icon: <CheckCircleFilled /> },
  PENDING: { palette: 'info',     label: '대기 중',  icon: <ClockCircleFilled /> },
  FAILED:  { palette: 'negative', label: '인증 실패', icon: <CloseCircleFilled /> },
};

const jobConfig: Record<JobStatus, TagConfig> = {
  RUNNING:   { palette: 'info',     label: '실행 중',   icon: <PlayCircleFilled /> },
  SCHEDULED: { palette: 'info',     label: '예약됨',    icon: <ClockCircleFilled /> },
  PAUSED:    { palette: 'warning',  label: '일시 정지', icon: <PauseCircleFilled /> },
  COMPLETE:  { palette: 'neutral',  label: '완료',      icon: <CheckCircleFilled /> },
};

const suppressionConfig: Record<SuppressionReason, TagConfig> = {
  BOUNCE:    { palette: 'negative', label: '반송',    icon: <ExclamationCircleFilled /> },
  COMPLAINT: { palette: 'warning',  label: '스팸 신고', icon: <CloseCircleFilled /> },
};

const emailResultConfig: Record<EmailResultStatus, TagConfig> = {
  Queued:     { palette: 'neutral',  label: '대기',      icon: <ClockCircleFilled /> },
  Sending:    { palette: 'info',     label: '발송중',    icon: <PlayCircleFilled /> },
  Delayed:    { palette: 'warning',  label: '지연',      icon: <PauseCircleFilled /> },
  Delivered:  { palette: 'positive', label: '전달완료',  icon: <CheckCircleFilled /> },
  Bounced:    { palette: 'warning',  label: '반송',      icon: <ExclamationCircleFilled /> },
  Complained: { palette: 'negative', label: '수신거부',  icon: <CloseCircleFilled /> },
  Rejected:   { palette: 'negative', label: '발송거부',  icon: <StopFilled /> },
  Error:      { palette: 'negative', label: '오류',      icon: <CloseCircleFilled /> },
  Blocked:    { palette: 'negative', label: '차단',      icon: <StopFilled /> },
  Timeout:    { palette: 'warning',  label: '타임아웃',  icon: <ExclamationCircleFilled /> },
};

export default function StatusTag({ type, status, style }: StatusTagProps) {
  let config: TagConfig | undefined;

  if (type === 'tenant') {
    config = tenantConfig[status as TenantStatus];
  } else if (type === 'verification') {
    config = verificationConfig[status as VerificationStatus];
  } else if (type === 'job') {
    config = jobConfig[status as JobStatus];
  } else if (type === 'suppression') {
    config = suppressionConfig[status as SuppressionReason];
  } else if (type === 'emailResult') {
    config = emailResultConfig[status as EmailResultStatus];
  }

  if (!config) {
    return <Tag style={style}>{status}</Tag>;
  }

  const colors = palette[config.palette];

  return (
    <Tag
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        fontWeight: 500,
        fontSize: 13,
        color: colors.text,
        backgroundColor: colors.bg,
        borderColor: colors.border,
        ...style,
      }}
    >
      <span style={{ color: colors.icon, display: 'inline-flex', fontSize: 12 }}>{config.icon}</span>
      {config.label}
    </Tag>
  );
}
