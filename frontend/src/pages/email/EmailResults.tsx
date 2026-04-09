import { useEffect, useRef, useMemo, useState } from 'react';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Row,
  Segmented,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleFilled,
  CopyOutlined,
  ExclamationCircleFilled,
  StopOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { useQueryClient } from '@tanstack/react-query';
import { useEmailResultDetail, useEmailResults } from '@/hooks/useEmailResults';
import { useTenants } from '@/hooks/useTenants';
import type { EmailResult } from '@/types/emailResults';
import StatusTag from '@/components/StatusTag';
import PageHeader from '@/components/PageHeader';

const { Text } = Typography;
const { RangePicker } = DatePicker;

// ─── 상태 필터 매핑 ──────────────────────────────────────────────────────────
const STATUS_SEGMENTS = [
  { label: '전체', value: 'ALL' },
  { label: '발송중', value: 'Sending' },
  { label: '전달완료', value: 'Delivered' },
  { label: '반송', value: 'Bounced' },
  { label: '수신거부', value: 'Complained' },
  { label: '실패', value: 'Error' },
] as const;

// ─── 결과 유형 한글 레이블 ───────────────────────────────────────────────────
const RESULT_TYPE_LABELS: Record<string, string> = {
  Send: '발송 수락',
  Delivery: '전달 성공',
  Open: '열람',
  Click: '클릭',
  Bounce: '반송',
  Complaint: '스팸 신고',
  Reject: '발송 거부',
  RenderingFailure: '렌더링 오류',
  DeliveryDelay: '전달 지연',
  Blacklist: '블랙리스트 차단',
  SESFail: 'SES 호출 실패',
  QuartzFail: '스케줄러 실패',
};

function resultTypeLabel(code: string | null): string {
  if (!code) return '-';
  return RESULT_TYPE_LABELS[code] ?? code;
}

// ─── 메시지 ID 단축 ──────────────────────────────────────────────────────────
function shortenId(id: string | null) {
  if (!id) return '-';
  if (id.length <= 16) return id;
  return `${id.slice(0, 8)}...${id.slice(-8)}`;
}

// ─── 요약 통계 카드 ──────────────────────────────────────────────────────────
function SummaryCard({
  label,
  count,
  color,
  icon,
}: {
  label: string;
  count: number;
  color: string;
  icon: React.ReactNode;
}) {
  return (
    <Card
      style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
      styles={{ body: { padding: '20px 24px' } }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div
          style={{
            width: 40,
            height: 40,
            borderRadius: 10,
            background: `${color}14`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <span style={{ color, fontSize: 20 }}>{icon}</span>
        </div>
        <div>
          <Text style={{ fontSize: 13, color: '#6b7280', display: 'block' }}>{label}</Text>
          <Text style={{ fontSize: 20, fontWeight: 600, color: '#111827' }}>
            {count.toLocaleString()}
          </Text>
          <Text style={{ fontSize: 11, color: '#9ca3af', display: 'block' }}>현재 페이지 기준</Text>
        </div>
      </div>
    </Card>
  );
}

// ─── 상세 Drawer ─────────────────────────────────────────────────────────────
function DetailDrawer({
  correlationId,
  open,
  onClose,
}: {
  correlationId: string | null;
  open: boolean;
  onClose: () => void;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: master, isLoading } = useEmailResultDetail(correlationId);

  // 상세 데이터가 변경되면 목록도 갱신
  const prevMasterRef = useRef<typeof master>(undefined);
  useEffect(() => {
    if (master && master !== prevMasterRef.current) {
      prevMasterRef.current = master;
      void queryClient.invalidateQueries({ queryKey: ['emailResults'] });
    }
  }, [master, queryClient]);

  // correlationId에 해당하는 detail을 찾아 표시
  const detail = master?.details?.find((d) => d.correlationId === correlationId)
    ?? master?.details?.[0];

  const fields = detail
    ? [
        { label: '추적 ID', value: detail.correlationId ?? '-', mono: true },
        { label: 'SES 메시지 ID', value: detail.sesMessageId ?? '-', mono: true },
        { label: '테넌트 ID', value: master?.tenantId ?? '-' },
        { label: '발신자', value: detail.sendEmailAddr },
        { label: '수신자', value: detail.rcvEmailAddr ?? '-' },
        { label: '제목', value: detail.emailTitle ?? '-' },
        {
          label: '상태',
          value: <StatusTag type="emailResult" status={detail.sendStsCd} />,
        },
        {
          label: '결과 유형',
          value: resultTypeLabel(detail.sendRsltTypCd),
        },
        {
          label: '발송시각',
          value: detail.sesRealSendDt
            ? dayjs.utc(detail.sesRealSendDt).local().format('YYYY-MM-DD HH:mm:ss')
            : detail.stmFirRegDt
              ? dayjs.utc(detail.stmFirRegDt).local().format('YYYY-MM-DD HH:mm:ss')
              : '-',
        },
      ]
    : [];

  const showSuppressionLink = detail?.sendStsCd === 'Bounced' || detail?.sendStsCd === 'Complained';

  return (
    <Drawer
      title="발송 결과 상세"
      placement="right"
      width={560}
      open={open}
      onClose={onClose}
      loading={isLoading}
    >
      {detail && (
        <div>
          {fields.map((field) => (
            <div
              key={field.label}
              style={{
                display: 'flex',
                gap: 12,
                padding: '12px 0',
                borderBottom: '1px solid #f0f2f5',
              }}
            >
              <Text
                style={{
                  width: 100,
                  color: '#6b7280',
                  fontSize: 14,
                  flexShrink: 0,
                }}
              >
                {field.label}
              </Text>
              <div style={{ flex: 1 }}>
                {typeof field.value === 'string' ? (
                  <Text
                    style={{
                      fontSize: 14,
                      color: '#111827',
                      fontWeight: 500,
                      fontFamily: field.mono ? 'monospace' : undefined,
                      wordBreak: 'break-all',
                    }}
                  >
                    {field.value}
                  </Text>
                ) : (
                  field.value
                )}
              </div>
            </div>
          ))}

          {showSuppressionLink && (
            <div style={{ marginTop: 24 }}>
              <Button
                type="default"
                icon={<StopOutlined />}
                onClick={() => navigate('/suppression')}
                style={{ borderRadius: 8 }}
              >
                수신 거부 목록에서 확인
              </Button>
            </div>
          )}
        </div>
      )}
    </Drawer>
  );
}

// ─── 이메일 발송 결과 페이지 ──────────────────────────────────────────────────
export default function EmailResults() {
  const [searchParams] = useSearchParams();
  const initialMessageId = searchParams.get('messageId');

  const [tenantId, setTenantId] = useState<string | undefined>(undefined);
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(7, 'day'),
    dayjs(),
  ]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [drawerOpen, setDrawerOpen] = useState(!!initialMessageId);
  const [selectedSesMsgId, setSelectedSesMsgId] = useState<string | null>(initialMessageId);

  const { data: tenantsData } = useTenants({ size: 100 });

  const queryParams = useMemo(
    () => ({
      tenantId,
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      page,
      size: pageSize,
    }),
    [tenantId, dateRange, statusFilter, page, pageSize],
  );

  const { data, isLoading } = useEmailResults(queryParams);

  // 요약 통계 계산 (현재 페이지 기준)
  const summary = useMemo(() => {
    const results = data?.results ?? [];
    return {
      delivered: results.filter((r) => r.sendStsCd === 'Delivered').length,
      bounced: results.filter((r) => r.sendStsCd === 'Bounced').length,
      complained: results.filter((r) => r.sendStsCd === 'Complained').length,
    };
  }, [data]);

  const handleRowClick = (record: EmailResult) => {
    if (record.correlationId) {
      setSelectedSesMsgId(record.correlationId);
      setDrawerOpen(true);
    }
  };

  const columns: ProColumns<EmailResult>[] = [
    {
      title: '발송시각',
      dataIndex: 'stmFirRegDt',
      width: 160,
      render: (_, record) => (
        <Text style={{ fontSize: 13, fontFamily: 'monospace', color: '#374151' }}>
          {dayjs.utc(record.stmFirRegDt).local().format('YYYY-MM-DD HH:mm:ss')}
        </Text>
      ),
    },
    {
      title: '발신자',
      dataIndex: 'sendEmailAddr',
      ellipsis: true,
      width: 200,
      render: (val) => (
        <Text style={{ fontSize: 13, color: '#374151' }}>{val as string}</Text>
      ),
    },
    {
      title: '수신자',
      dataIndex: 'rcvEmailAddr',
      ellipsis: true,
      width: 200,
      render: (val) => (
        <Text style={{ fontSize: 13, color: '#374151' }}>{val as string}</Text>
      ),
    },
    {
      title: '제목',
      dataIndex: 'emailTitle',
      ellipsis: true,
      render: (val) => (
        <Text style={{ fontSize: 13, color: '#374151' }}>{(val as string) ?? '-'}</Text>
      ),
    },
    {
      title: '유형',
      dataIndex: 'emailTmpletId',
      width: 90,
      render: (_, record) => (
        record.emailTmpletId ? (
          <Tooltip title={`템플릿: ${record.emailTmpletId}`}>
            <Tag color="purple" style={{ margin: 0, borderRadius: 4, fontSize: 12 }}>템플릿</Tag>
          </Tooltip>
        ) : (
          <Tag color="blue" style={{ margin: 0, borderRadius: 4, fontSize: 12 }}>텍스트</Tag>
        )
      ),
    },
    {
      title: '추적 ID',
      dataIndex: 'correlationId',
      width: 180,
      render: (_, record) => (
        record.correlationId ? (
          <Space size={4}>
            <Tooltip title={record.correlationId}>
              <Text
                style={{
                  fontSize: 13,
                  fontFamily: 'monospace',
                  color: '#374151',
                }}
              >
                {shortenId(record.correlationId)}
              </Text>
            </Tooltip>
            <Tooltip title="복사">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined style={{ fontSize: 12 }} />}
                onClick={(e) => {
                  e.stopPropagation();
                  void navigator.clipboard.writeText(record.correlationId!);
                  void message.success('복사되었습니다.');
                }}
                style={{ color: '#9ca3af' }}
              />
            </Tooltip>
          </Space>
        ) : (
          <Text style={{ fontSize: 12, color: '#9ca3af' }}>-</Text>
        )
      ),
    },
    {
      title: '상태',
      dataIndex: 'sendStsCd',
      width: 110,
      render: (_, record) => (
        <StatusTag type="emailResult" status={record.sendStsCd} />
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="발송 결과 조회"
        subtitle="이메일 발송 상태를 확인합니다."
        breadcrumbs={[
          { title: '홈', href: '/' },
          { title: '서비스 관리' },
          { title: '발송 결과 조회' },
        ]}
      />

      {/* 요약 통계 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <SummaryCard
            label="전달완료"
            count={summary.delivered}
            color="#12b76a"
            icon={<CheckCircleFilled />}
          />
        </Col>
        <Col xs={24} sm={8}>
          <SummaryCard
            label="반송"
            count={summary.bounced}
            color="#f79009"
            icon={<ExclamationCircleFilled />}
          />
        </Col>
        <Col xs={24} sm={8}>
          <SummaryCard
            label="수신거부"
            count={summary.complained}
            color="#f04438"
            icon={<StopOutlined />}
          />
        </Col>
      </Row>

      {/* 필터 영역 */}
      <Card
        style={{ borderRadius: 12, border: '1px solid #e5e8ed', marginBottom: 16 }}
        styles={{ body: { padding: '16px 24px' } }}
      >
        <Row gutter={16} align="middle">
          <Col xs={24} sm={6}>
            <Text style={{ fontSize: 13, color: '#6b7280', display: 'block', marginBottom: 4 }}>
              테넌트
            </Text>
            <Select
              placeholder="전체 테넌트"
              allowClear
              showSearch
              optionFilterProp="label"
              style={{ width: '100%' }}
              value={tenantId}
              onChange={(v) => {
                setTenantId(v);
                setPage(0);
              }}
              options={tenantsData?.tenants?.map((t) => ({
                label: `${t.tenantName} (${t.domain})`,
                value: t.tenantId,
              }))}
            />
          </Col>
          <Col xs={24} sm={8}>
            <Text style={{ fontSize: 13, color: '#6b7280', display: 'block', marginBottom: 4 }}>
              기간
            </Text>
            <RangePicker
              value={dateRange}
              onChange={(dates) => {
                if (dates && dates[0] && dates[1]) {
                  setDateRange([dates[0], dates[1]]);
                  setPage(0);
                }
              }}
              style={{ width: '100%' }}
            />
          </Col>
          <Col xs={24} sm={10}>
            <Text style={{ fontSize: 13, color: '#6b7280', display: 'block', marginBottom: 4 }}>
              상태
            </Text>
            <Segmented
              options={STATUS_SEGMENTS.map((s) => ({ label: s.label, value: s.value }))}
              value={statusFilter}
              onChange={(v) => {
                setStatusFilter(v as string);
                setPage(0);
              }}
            />
          </Col>
        </Row>
      </Card>

      {/* 결과 테이블 */}
      <ProTable<EmailResult>
        rowKey="emailSendDtlSeq"
        columns={columns}
        dataSource={data?.results ?? []}
        loading={isLoading}
        search={false}
        toolBarRender={false}
        locale={{
          emptyText: (
            <div style={{ padding: '48px 0', color: '#9ca3af' }}>
              <div style={{ fontSize: 14, marginBottom: 8 }}>조건에 해당하는 발송 결과가 없습니다.</div>
              {statusFilter !== 'ALL' && (
                <Button
                  type="link"
                  size="small"
                  onClick={() => { setStatusFilter('ALL'); setPage(0); }}
                >
                  필터 초기화
                </Button>
              )}
            </div>
          ),
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record),
          style: { cursor: record.correlationId ? 'pointer' : 'default' },
          title: record.correlationId ? '클릭하여 상세 보기' : '상세 정보가 아직 생성되지 않았습니다',
        })}
        pagination={{
          current: page + 1,
          pageSize,
          total: data?.totalCount ?? 0,
          showSizeChanger: true,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p, size) => {
            setPage(p - 1);
            setPageSize(size);
          },
          style: { padding: '12px 16px' },
        }}
        scroll={{ x: 1100 }}
        cardBordered
        style={{ borderRadius: 12 }}
        tableStyle={{ borderRadius: 12 }}
      />

      {/* 상세 Drawer */}
      <DetailDrawer
        correlationId={selectedSesMsgId}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedSesMsgId(null);
        }}
      />
    </div>
  );
}
