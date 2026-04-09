import { useEffect, useState } from 'react';
import { Button, Card, Col, Progress, Row, Statistic, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ArrowRightOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  FundOutlined,
  MailOutlined,
  RocketOutlined,
  TeamOutlined,
  ThunderboltFilled,
  WarningOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { useTenants } from '@/hooks/useTenants';
import { useJobs } from '@/hooks/useScheduler';
import { useMonitoringSummary, useSesQuota } from '@/hooks/useMonitoring';
import { useEmailResults } from '@/hooks/useEmailResults';
import { useRecentCampaign, useTenantReputation } from '@/hooks/useMonitoring';
import type { EmailResult } from '@/types/emailResults';

const { Title, Text } = Typography;

// ─── 통계 카드 ────────────────────────────────────────────────────────────────
interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
  bgColor: string;
  suffix?: string;
  warn?: boolean;
  tooltip?: string;
}

function StatCard({ title, value, icon, color, bgColor, suffix, warn, tooltip }: StatCardProps) {
  const card = (
    <Card
      style={{
        borderRadius: 12,
        border: warn && value > 0 ? '1px solid #fec84b' : '1px solid #e5e8ed',
        overflow: 'hidden',
        cursor: tooltip ? 'help' : 'default',
      }}
      styles={{ body: { padding: '20px 24px' } }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <Text style={{ fontSize: 13, color: '#6b7280', fontWeight: 500, display: 'block', marginBottom: 8 }}>
            {title}
          </Text>
          <Statistic
            value={value}
            suffix={suffix}
            valueStyle={{
              fontSize: 28,
              fontWeight: 700,
              color: warn && value > 0 ? '#f79009' : '#111827',
              lineHeight: 1.2,
            }}
          />
        </div>
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: 10,
            background: bgColor,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 20,
            color,
            flexShrink: 0,
          }}
        >
          {icon}
        </div>
      </div>
    </Card>
  );
  return tooltip ? <Tooltip title={tooltip}><div>{card}</div></Tooltip> : card;
}

// ─── 최근 이메일 발송 컬럼 ───────────────────────────────────────────────────
const STATUS_COLOR: Record<string, { color: string; bg: string }> = {
  Delivered: { color: '#0d7a3a', bg: '#e6f7ed' },
  Sending:   { color: '#0958d9', bg: '#e6f0ff' },
  Queued:    { color: '#6b7280', bg: '#f3f4f6' },
  Bounce:    { color: '#b45309', bg: '#fef3c7' },
  Complaint: { color: '#dc2626', bg: '#fee2e2' },
  Error:     { color: '#dc2626', bg: '#fee2e2' },
};

const recentEmailColumns: ColumnsType<EmailResult> = [
  {
    title: '수신자',
    dataIndex: 'rcvEmailAddr',
    ellipsis: true,
    width: 180,
    render: (v: string) => <Text style={{ fontSize: 13, color: '#374151' }}>{v}</Text>,
  },
  {
    title: '제목',
    dataIndex: 'emailTitle',
    ellipsis: true,
    render: (v: string) => <Text style={{ fontWeight: 500, fontSize: 13 }}>{v || '-'}</Text>,
  },
  {
    title: '유형',
    dataIndex: 'emailTmpletId',
    width: 80,
    render: (v: string | null) => (
      <Tag color={v ? 'blue' : 'default'} style={{ borderRadius: 6, fontSize: 11 }}>
        {v ? '템플릿' : '텍스트'}
      </Tag>
    ),
  },
  {
    title: '상태',
    dataIndex: 'sendStsCd',
    width: 90,
    render: (v: string) => {
      const s = STATUS_COLOR[v] ?? { color: '#6b7280', bg: '#f3f4f6' };
      return (
        <Tag style={{ borderRadius: 6, fontSize: 11, color: s.color, background: s.bg, border: 'none', fontWeight: 600 }}>
          {v}
        </Tag>
      );
    },
  },
  {
    title: '발송시각',
    dataIndex: 'stmFirRegDt',
    width: 120,
    render: (v: string) => (
      <Text style={{ fontSize: 12, color: '#9ca3af', fontFamily: 'monospace' }}>
        {dayjs.utc(v).local().format('MM/DD HH:mm')}
      </Text>
    ),
  },
];

// ─── 대시보드 페이지 ──────────────────────────────────────────────────────────
export default function DashboardPage() {
  const navigate = useNavigate();
  const { data: tenantsData } = useTenants();
  const { data: jobsData } = useJobs();
  const { data: monitoringData } = useMonitoringSummary();
  const { data: emailResultsData, isLoading: isEmailLoading } = useEmailResults({ page: 0, size: 10 });
  const { data: campaign } = useRecentCampaign();
  const { data: tenantReputation } = useTenantReputation();
  const { data: sesQuota } = useSesQuota();

  const activeTenantCount = tenantsData?.tenants.filter((t) => t.status === 'ACTIVE').length ?? 0;
  const totalTenantCount = tenantsData?.tenants.length ?? 0;
  const scheduledJobCount = jobsData?.numOfAllJobs ?? 0;
  const runningJobCount = jobsData?.numOfRunningJobs ?? 0;

  const recentEmails: EmailResult[] = emailResultsData?.results ?? [];

  const [now, setNow] = useState(dayjs());
  useEffect(() => {
    const timer = setInterval(() => setNow(dayjs()), 60_000);
    return () => clearInterval(timer);
  }, []);
  const DAYS_KO = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
  const dayName = DAYS_KO[now.day()];

  return (
    <div style={{ padding: 24 }}>

      {/* ─── 환영 배너 ─── */}
      <Card
        style={{
          marginBottom: 20,
          background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)',
          border: 'none',
          borderRadius: 14,
          overflow: 'hidden',
        }}
        styles={{ body: { padding: '20px 28px' } }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
              <ThunderboltFilled style={{ color: 'rgba(255,255,255,0.7)', fontSize: 14 }} />
              <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 13 }}>
                Joins EMS 관리 콘솔
              </Text>
            </div>
            <Title level={3} style={{ color: '#fff', margin: 0, fontWeight: 700, letterSpacing: '-0.3px' }}>
              {now.format('YYYY년 MM월 DD일')}
              <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: 15, fontWeight: 400, marginLeft: 12 }}>
                {dayName} {now.format('HH:mm')} 현재
              </Text>
            </Title>
          </div>
          <div style={{ display: 'flex', gap: 24, textAlign: 'center' }}>
            <div>
              <div style={{ fontSize: 22, fontWeight: 700, color: '#fff' }}>
                {monitoringData?.todaySentCount?.toLocaleString() ?? 0}
              </div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)' }}>오늘 발송</div>
            </div>
            <div style={{ width: 1, background: 'rgba(255,255,255,0.2)' }} />
            <div>
              <div style={{ fontSize: 22, fontWeight: 700, color: '#fff' }}>
                {monitoringData?.deliveryRate ?? 0}%
              </div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)' }}>전달률</div>
            </div>
            <div style={{ width: 1, background: 'rgba(255,255,255,0.2)' }} />
            <div>
              <div style={{ fontSize: 22, fontWeight: 700, color: (monitoringData?.bounceRate ?? 0) > 2 ? '#fec84b' : '#fff' }}>
                {monitoringData?.bounceRate ?? 0}%
              </div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)' }}>반송율</div>
            </div>
          </div>
        </div>
      </Card>

      {/* ─── KPI 카드 4개 ─── */}
      <Row gutter={[14, 14]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="활성 테넌트"
            value={activeTenantCount}
            suffix={`/ ${totalTenantCount}`}
            icon={<TeamOutlined />}
            color="#12b76a"
            bgColor="rgba(18,183,106,0.1)"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="예약 작업"
            value={scheduledJobCount}
            icon={<CalendarOutlined />}
            color="#f79009"
            bgColor="rgba(247,144,9,0.1)"
            suffix={runningJobCount > 0 ? `(실행중 ${runningJobCount})` : undefined}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="반송율 (오늘)"
            value={monitoringData?.bounceRate ?? 0}
            suffix="%"
            icon={<WarningOutlined />}
            color="#f79009"
            bgColor="rgba(247,144,9,0.1)"
            warn
            tooltip="반송율 5% 초과 시 SES 계정이 제한될 수 있습니다."
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="실행중 배치"
            value={(monitoringData?.runningBatchCount ?? 0) + (monitoringData?.pendingBatchCount ?? 0)}
            suffix="건"
            icon={<FundOutlined />}
            color="#7c3aed"
            bgColor="rgba(124,58,237,0.1)"
          />
        </Col>
      </Row>

      {/* ─── SES 발송 한도 ─── */}
      {sesQuota && (
        <Card
          size="small"
          style={{ marginBottom: 20, borderRadius: 10 }}
          bodyStyle={{ padding: '12px 20px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <ThunderboltFilled style={{ color: sesQuota.remaining24Hours < 20 ? '#f5222d' : '#1677ff', fontSize: 16 }} />
              <Text strong style={{ fontSize: 13 }}>SES 발송 한도</Text>
              {!sesQuota.productionAccess && (
                <Tag color="orange" style={{ fontSize: 11, borderRadius: 4, marginLeft: 4 }}>Sandbox</Tag>
              )}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 24, flexWrap: 'wrap' }}>
              <div style={{ fontSize: 13 }}>
                <Text type="secondary">초당 발송: </Text>
                <Text strong>{sesQuota.maxSendRate}건/초</Text>
              </div>
              <div style={{ fontSize: 13 }}>
                <Text type="secondary">24시간: </Text>
                <Text strong>{sesQuota.sentLast24Hours?.toFixed(0) ?? 0}</Text>
                <Text type="secondary"> / {sesQuota.max24HourSend?.toFixed(0) ?? 0}건</Text>
              </div>
              <div style={{ width: 150 }}>
                <Progress
                  percent={sesQuota.max24HourSend ? Math.round((sesQuota.sentLast24Hours / sesQuota.max24HourSend) * 100) : 0}
                  size="small"
                  strokeColor={
                    sesQuota.max24HourSend && (sesQuota.sentLast24Hours / sesQuota.max24HourSend) > 0.8
                      ? '#f5222d'
                      : sesQuota.max24HourSend && (sesQuota.sentLast24Hours / sesQuota.max24HourSend) > 0.5
                        ? '#faad14'
                        : '#52c41a'
                  }
                />
              </div>
              <div style={{ fontSize: 13 }}>
                <Text type="secondary">잔여: </Text>
                <Text strong style={{ color: sesQuota.remaining24Hours < 20 ? '#f5222d' : undefined }}>
                  {sesQuota.remaining24Hours?.toFixed(0) ?? 0}건
                </Text>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* ─── 하단 2컬럼 ─── */}
      <Row gutter={[14, 14]}>

        {/* 최근 이메일 발송 */}
        <Col xs={24} lg={17}>
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <MailOutlined style={{ color: '#1677ff' }} />
                <span style={{ fontWeight: 600 }}>최근 이메일 발송</span>
              </div>
            }
            extra={
              <Button
                type="link"
                size="small"
                icon={<ArrowRightOutlined />}
                onClick={() => navigate('/email/results')}
                style={{ padding: 0, color: '#6b7280', fontSize: 13 }}
              >
                전체 보기
              </Button>
            }
            styles={{ body: { padding: '0 16px' } }}
            style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
          >
            <Table<EmailResult>
              dataSource={recentEmails}
              columns={recentEmailColumns}
              rowKey="emailSendDtlSeq"
              pagination={false}
              loading={isEmailLoading}
              size="small"
              scroll={{ x: 500 }}
              locale={{ emptyText: '발송 내역이 없습니다.' }}
            />
          </Card>
        </Col>

        {/* 우측: 빠른 실행 + 스케줄러 현황 */}
        <Col xs={24} lg={7}>

          {/* 빠른 실행 */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <RocketOutlined style={{ color: '#7c3aed' }} />
                <span style={{ fontWeight: 600 }}>빠른 실행</span>
              </div>
            }
            style={{ marginBottom: 14, borderRadius: 12, border: '1px solid #e5e8ed' }}
            styles={{ body: { padding: 14 } }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                { label: '이메일 발송', path: '/email/send', color: '#1677ff', bg: 'rgba(22,119,255,0.08)', icon: <MailOutlined /> },
                { label: '발송 결과 조회', path: '/email/results', color: '#12b76a', bg: 'rgba(18,183,106,0.08)', icon: <CheckCircleFilled /> },
                { label: '모니터링', path: '/monitoring', color: '#f79009', bg: 'rgba(247,144,9,0.08)', icon: <FundOutlined /> },
              ].map((item) => (
                <Button
                  key={item.path}
                  block
                  icon={item.icon}
                  onClick={() => navigate(item.path)}
                  style={{
                    textAlign: 'left',
                    height: 40,
                    borderRadius: 8,
                    border: 'none',
                    background: item.bg,
                    color: item.color,
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                  }}
                >
                  {item.label}
                </Button>
              ))}
            </div>
          </Card>

          {/* 스케줄러 현황 */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <ClockCircleOutlined style={{ color: '#f79009' }} />
                <span style={{ fontWeight: 600 }}>스케줄러 현황</span>
              </div>
            }
            extra={
              <Button
                type="link"
                size="small"
                icon={<ArrowRightOutlined />}
                onClick={() => navigate('/scheduler')}
                style={{ padding: 0, color: '#6b7280', fontSize: 13 }}
              >
                관리
              </Button>
            }
            style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
            styles={{ body: { padding: '16px 20px' } }}
          >
            <Row gutter={12}>
              <Col span={12}>
                <div style={{ textAlign: 'center', padding: '8px 0' }}>
                  <div style={{ fontSize: 26, fontWeight: 700, color: '#111827' }}>{scheduledJobCount}</div>
                  <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 2 }}>전체 작업</div>
                </div>
              </Col>
              <Col span={12}>
                <div style={{ textAlign: 'center', padding: '8px 0', borderLeft: '1px solid #f0f0f0' }}>
                  <div style={{ fontSize: 26, fontWeight: 700, color: runningJobCount > 0 ? '#1677ff' : '#111827' }}>
                    {runningJobCount}
                  </div>
                  <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 2 }}>실행 중</div>
                </div>
              </Col>
            </Row>
            {scheduledJobCount === 0 && (
              <div style={{ textAlign: 'center', padding: '8px 0 4px', color: '#9ca3af', fontSize: 13 }}>
                등록된 예약 작업이 없습니다.
              </div>
            )}
          </Card>
        </Col>
      </Row>
      {/* ─── 최근 캠페인 통계 ─── */}
      {campaign?.batchId && (
        <Row gutter={[14, 14]} style={{ marginTop: 14 }}>
          <Col span={24}>
            <Card
              style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
              styles={{ body: { padding: '16px 24px' } }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <div>
                  <Text style={{ fontWeight: 600, fontSize: 14 }}>최근 발송한 이메일</Text>
                  {campaign.templateName && (
                    <Tag style={{ marginLeft: 8, borderRadius: 6 }}>{campaign.templateName}</Tag>
                  )}
                  {campaign.startDateAt && (
                    <Text style={{ fontSize: 12, color: '#9ca3af', marginLeft: 8 }}>
                      {dayjs.utc(campaign.startDateAt).local().format('YYYY-MM-DD HH:mm')}
                    </Text>
                  )}
                </div>
                {!campaign.complete && (
                  <Tag color="processing" style={{ borderRadius: 6 }}>발송 진행중</Tag>
                )}
              </div>
              {!campaign.complete && (
                <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 12 }}>
                  발송 성공, 발송 성공률, 오픈율, 클릭률, 수신거부율 통계는 발송이 모두 완료된 후에 반영됩니다.
                </div>
              )}
              <Row gutter={[16, 0]}>
                {[
                  {
                    label: '발송 성공',
                    count: campaign.delivered,
                    rate: campaign.deliveryRate,
                    prev: campaign.prevDeliveryRate,
                    color: '#12b76a',
                  },
                  {
                    label: '오픈',
                    count: campaign.opens,
                    rate: campaign.openRate,
                    prev: campaign.prevOpenRate,
                    color: '#1677ff',
                  },
                  {
                    label: '클릭',
                    count: campaign.clicks,
                    rate: campaign.clickRate,
                    prev: campaign.prevClickRate,
                    color: '#7c3aed',
                  },
                  {
                    label: '수신거부',
                    count: campaign.complaints,
                    rate: campaign.complaintRate,
                    prev: campaign.prevComplaintRate,
                    color: '#f04438',
                  },
                ].map((item) => {
                  const diff = item.prev != null ? Math.round((item.rate - item.prev) * 10) / 10 : null;
                  return (
                    <Col key={item.label} xs={12} sm={6}>
                      <div style={{ padding: '8px 0', borderRight: '1px solid #f0f0f0' }}>
                        <Text style={{ fontSize: 12, color: '#6b7280', display: 'block', marginBottom: 4 }}>
                          {item.label}
                        </Text>
                        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                          <span style={{ fontSize: 22, fontWeight: 700, color: '#111827' }}>
                            {(item.count ?? 0).toLocaleString()}
                          </span>
                          <span style={{ fontSize: 14, color: item.color, fontWeight: 600 }}>
                            {item.rate}%
                          </span>
                        </div>
                        <Text style={{ fontSize: 11, color: '#9ca3af', display: 'block', marginTop: 2 }}>
                          {diff == null ? '지난 이메일 없음' : diff === 0
                            ? '지난 이메일보다 변화 없음'
                            : diff > 0
                              ? `지난 이메일보다 +${diff}%p`
                              : `지난 이메일보다 ${diff}%p`}
                        </Text>
                      </div>
                    </Col>
                  );
                })}
              </Row>
            </Card>
          </Col>
        </Row>
      )}

      {/* ─── 테넌트별 평판 모니터링 ─── */}
      {(tenantReputation?.length ?? 0) > 0 && (
        <Row gutter={[14, 14]} style={{ marginTop: 14 }}>
          <Col span={24}>
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <WarningOutlined style={{ color: '#f79009' }} />
                  <span style={{ fontWeight: 600 }}>테넌트별 평판 모니터링 (오늘)</span>
                </div>
              }
              style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
              styles={{ body: { padding: '0 16px' } }}
            >
              <Table
                rowKey="tenantId"
                dataSource={tenantReputation}
                pagination={false}
                size="small"
                columns={[
                  {
                    title: '테넌트명',
                    dataIndex: 'tenantId',
                    render: (v: string) => {
                      const t = tenantsData?.tenants.find((t) => t.tenantId === v);
                      return <Text style={{ fontWeight: 600 }}>{t?.tenantName ?? '-'}</Text>;
                    },
                  },
                  {
                    title: '테넌트 ID',
                    dataIndex: 'tenantId',
                    render: (v: string) => <Text style={{ fontSize: 12, fontFamily: 'monospace', color: '#6b7280' }}>{v}</Text>,
                  },
                  {
                    title: '오늘 발송',
                    dataIndex: 'totalSent',
                    align: 'right' as const,
                    render: (v: number) => (v ?? 0).toLocaleString(),
                  },
                  {
                    title: '전달률',
                    dataIndex: 'deliveryRate',
                    align: 'right' as const,
                    render: (v: number) => {
                      const s = v >= 95 ? { color: '#0d7a3a', bg: '#e6f7ed' } : v >= 85 ? { color: '#b45309', bg: '#fef3c7' } : { color: '#dc2626', bg: '#fee2e2' };
                      return <Tag style={{ borderRadius: 6, color: s.color, background: s.bg, border: 'none', fontWeight: 600 }}>{v}%</Tag>;
                    },
                  },
                  {
                    title: '반송율',
                    dataIndex: 'bounceRate',
                    align: 'right' as const,
                    render: (v: number) => {
                      const s = v <= 2 ? { color: '#0d7a3a', bg: '#e6f7ed' } : v <= 5 ? { color: '#b45309', bg: '#fef3c7' } : { color: '#dc2626', bg: '#fee2e2' };
                      return <Tag style={{ borderRadius: 6, color: s.color, background: s.bg, border: 'none', fontWeight: 600 }}>{v}%</Tag>;
                    },
                  },
                  {
                    title: '수신거부율',
                    dataIndex: 'complaintRate',
                    align: 'right' as const,
                    render: (v: number) => {
                      const s = v <= 0.1 ? { color: '#0d7a3a', bg: '#e6f7ed' } : v <= 0.5 ? { color: '#b45309', bg: '#fef3c7' } : { color: '#dc2626', bg: '#fee2e2' };
                      return <Tag style={{ borderRadius: 6, color: s.color, background: s.bg, border: 'none', fontWeight: 600 }}>{v}%</Tag>;
                    },
                  },
                ]}
              />
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}
