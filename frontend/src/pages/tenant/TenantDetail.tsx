import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Progress,
  Row,
  Skeleton,
  Space,
  Tag,
  Tabs,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import {
  ApiOutlined,
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  GlobalOutlined,
  KeyOutlined,
  MailOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyOutlined,
  SendOutlined,
  SettingOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useTenant, useUpdateTenant, useRegenerateApiKey, useSenders, useAddSender, useRemoveSender, usePauseTenant, useResumeTenant } from '@/hooks/useTenants';
import { useDkimRecords, useEmailVerificationStatus, useResendVerification, useVerifyEmail, useActivateTenant } from '@/hooks/useTenantSetup';
import type { UpdateTenantRequest } from '@/types/tenant';
import StatusTag from '@/components/StatusTag';
import PageHeader from '@/components/PageHeader';

const { Title, Text } = Typography;

// 발신자 인증 상태 Badge 컴포넌트
function SenderVerificationBadge({
  tenantId,
  email,
  onResend,
}: {
  tenantId: string;
  email: string;
  onResend: (email: string) => void;
}) {
  const { data: status, isLoading, isError, isTimedOut, resetTimeout } = useEmailVerificationStatus(tenantId, email);

  if (isLoading) {
    return <Badge status="processing" text={<Text style={{ fontSize: 12, color: '#9ca3af' }}>확인 중...</Text>} />;
  }

  if (isError) {
    return (
      <Space size={4}>
        <Tag
          icon={<CloseCircleOutlined />}
          color="error"
          style={{ borderRadius: 6, fontSize: 12 }}
        >
          조회 실패
        </Tag>
        <Tooltip title="인증 이메일 재발송">
          <Button
            type="text"
            size="small"
            icon={<SendOutlined />}
            onClick={() => { resetTimeout(); onResend(email); }}
            style={{ color: '#f79009', padding: '0 4px' }}
          />
        </Tooltip>
      </Space>
    );
  }

  if (!status) return null;

  if (status.verificationStatus === 'SUCCESS') {
    return (
      <Tag
        icon={<CheckCircleOutlined />}
        color="success"
        style={{ borderRadius: 6, fontSize: 12 }}
      >
        인증됨
      </Tag>
    );
  }

  if (status.verificationStatus === 'FAILED') {
    return (
      <Space size={4}>
        <Tag
          icon={<CloseCircleOutlined />}
          color="error"
          style={{ borderRadius: 6, fontSize: 12 }}
        >
          인증 실패
        </Tag>
        <Tooltip title="인증 이메일 재발송">
          <Button
            type="text"
            size="small"
            icon={<SendOutlined />}
            onClick={() => { resetTimeout(); onResend(email); }}
            style={{ color: '#f79009', padding: '0 4px' }}
          />
        </Tooltip>
      </Space>
    );
  }

  // PENDING (타임아웃 포함)
  return (
    <Space size={4}>
      <Tag
        icon={<ClockCircleOutlined />}
        color={isTimedOut ? 'error' : 'warning'}
        style={{ borderRadius: 6, fontSize: 12 }}
      >
        {isTimedOut ? '인증 시간 초과' : '인증 대기중'}
      </Tag>
      <Tooltip title="인증 이메일 재발송">
        <Button
          type="text"
          size="small"
          icon={<SendOutlined />}
          onClick={() => { resetTimeout(); onResend(email); }}
          style={{ color: '#f79009', padding: '0 4px' }}
        />
      </Tooltip>
    </Space>
  );
}

export default function TenantDetail() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: tenant, isLoading } = useTenant(id);
  const { mutate: updateTenant, isPending: isUpdating } = useUpdateTenant();
  const { mutate: regenerate, isPending: isRegenerating } = useRegenerateApiKey();

  const [editModalOpen, setEditModalOpen] = useState(false);
  const [senderModalOpen, setSenderModalOpen] = useState(false);
  const [apiKeyVisible, setApiKeyVisible] = useState(false);
  const [form] = Form.useForm<UpdateTenantRequest>();
  const [senderForm] = Form.useForm();
  const { data: senders, isLoading: isSendersLoading } = useSenders(id);
  const { mutate: addSender, isPending: isAddingSender } = useAddSender();
  const { mutate: removeSender } = useRemoveSender();
  const { mutate: verifyEmail, isPending: isVerifyingEmail } = useVerifyEmail();
  const { mutate: resendVerification } = useResendVerification();
  const { data: dkimRecords } = useDkimRecords(id);
  const { mutate: activateTenant, isPending: isActivating } = useActivateTenant();
  const queryClient = useQueryClient();
  const pauseMutation = usePauseTenant();
  const resumeMutation = useResumeTenant();
  const pauseLoading = pauseMutation.isPending;
  const resumeLoading = resumeMutation.isPending;

  const handlePause = () => {
    if (!id) return;
    pauseMutation.mutate(id, {
      onSuccess: () => {
        void message.success('테넌트 발송이 일시정지되었습니다.');
        void queryClient.invalidateQueries({ queryKey: ['tenant', id] });
      },
      onError: () => void message.error('일시정지 실패'),
    });
  };

  const handleResume = () => {
    if (!id) return;
    resumeMutation.mutate(id, {
      onSuccess: () => {
        void message.success('테넌트 발송이 재개되었습니다.');
        void queryClient.invalidateQueries({ queryKey: ['tenant', id] });
      },
      onError: () => void message.error('발송 재개 실패'),
    });
  };

  const handleEditOpen = () => {
    if (!tenant) return;
    form.setFieldsValue({
      tenantName: tenant.tenantName,
      quotaDaily: tenant.quotaDaily,
      quotaMonthly: tenant.quotaMonthly,
    });
    setEditModalOpen(true);
  };

  const handleEditSubmit = (values: UpdateTenantRequest) => {
    updateTenant(
      { id, payload: values },
      {
        onSuccess: () => {
          void message.success('테넌트 정보가 수정되었습니다.');
          setEditModalOpen(false);
        },
      },
    );
  };

  const handleRegenerateApiKey = () => {
    regenerate(id, {
      onSuccess: () => {
        void message.success('API 키가 재생성되었습니다.');
        setApiKeyVisible(false);
      },
    });
  };

  if (isLoading) {
    return (
      <div style={{ padding: 24 }}>
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    );
  }

  if (!tenant) {
    return (
      <div style={{ padding: 24 }}>
        <Text type="secondary">테넌트를 찾을 수 없습니다.</Text>
      </div>
    );
  }

  const maskedApiKey = tenant.apiKey
    ? `${tenant.apiKey.substring(0, 8)}${'•'.repeat(24)}${tenant.apiKey.slice(-4)}`
    : '-';

  const tabItems = [
    {
      key: 'info',
      label: (
        <Space size={6}>
          <TeamOutlined />
          기본 정보
        </Space>
      ),
      children: (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr',
            gap: '16px 24px',
            padding: '12px 0',
          }}
        >
          {[
            {
              label: '테넌트 ID',
              value: (
                <Text copyable style={{ fontFamily: 'monospace', fontSize: 12 }}>
                  {tenant.tenantId}
                </Text>
              ),
            },
            {
              label: '테넌트명',
              value: <Text strong>{tenant.tenantName}</Text>,
            },
            {
              label: '도메인',
              value: tenant.domain,
            },
            {
              label: 'Config Set',
              value: tenant.configSetName ?? <Text type="secondary">-</Text>,
            },
            {
              label: '상태',
              value: <StatusTag type="tenant" status={tenant.status} />,
            },
            {
              label: '인증 상태',
              value: <StatusTag type="verification" status={tenant.verificationStatus} />,
            },
            {
              label: '생성일',
              value: (
                <Text style={{ color: '#6b7280', fontSize: 13 }}>
                  {dayjs.utc(tenant.createdAt).local().format('YYYY-MM-DD HH:mm:ss')}
                </Text>
              ),
            },
            {
              label: '수정일',
              value: (
                <Text style={{ color: '#6b7280', fontSize: 13 }}>
                  {dayjs.utc(tenant.updatedAt).local().format('YYYY-MM-DD HH:mm:ss')}
                </Text>
              ),
            },
          ].map((item) => (
            <div
              key={item.label}
              style={{
                display: 'flex',
                alignItems: 'baseline',
                gap: 16,
              }}
            >
              <div
                style={{
                  color: '#6b7280',
                  fontWeight: 500,
                  fontSize: 13,
                  minWidth: 100,
                  flexShrink: 0,
                }}
              >
                {item.label}
              </div>
              <div style={{ color: '#111827', fontSize: 14 }}>{item.value}</div>
            </div>
          ))}

          {/* SES 전송 상태 */}
          <Card title="SES 전송 상태" style={{ marginTop: 24 }}>
            <Descriptions column={2}>
              <Descriptions.Item label="SES 테넌트명">
                {tenant.sesTenantName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="전송 상태">
                <StatusTag type="tenant" status={tenant.status} />
              </Descriptions.Item>
            </Descriptions>
            <Space style={{ marginTop: 16 }}>
              {tenant.status === 'ACTIVE' && (
                <Button danger onClick={() => handlePause()} loading={pauseLoading}>
                  발송 일시정지
                </Button>
              )}
              {tenant.status === 'PAUSED' && (
                <Button type="primary" onClick={() => handleResume()} loading={resumeLoading}>
                  발송 재개
                </Button>
              )}
            </Space>
          </Card>
        </div>
      ),
    },
    {
      key: 'quota',
      label: (
        <Space size={6}>
          <SettingOutlined />
          쿼터
        </Space>
      ),
      children: (
        <div style={{ padding: '8px 0' }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12}>
              <Card
                style={{
                  borderRadius: 10,
                  border: '1px solid #e5e8ed',
                  background: '#fafbfc',
                }}
                bodyStyle={{ padding: 20 }}
              >
                <Text style={{ fontSize: 13, color: '#6b7280', fontWeight: 500 }}>
                  일일 쿼터
                </Text>
                <div
                  style={{
                    fontSize: 24,
                    fontWeight: 700,
                    color: '#111827',
                    margin: '8px 0 4px',
                  }}
                >
                  {tenant.quotaDaily.toLocaleString()}
                  <Text style={{ fontSize: 14, color: '#9ca3af', fontWeight: 400, marginLeft: 4 }}>
                    건 / 일
                  </Text>
                </div>
                <Progress
                  percent={Math.min(100, Math.round((0 / tenant.quotaDaily) * 100))}
                  strokeColor="#1677ff"
                  trailColor="#e5e8ed"
                  showInfo={false}
                  style={{ margin: 0 }}
                />
                <Text style={{ fontSize: 12, color: '#9ca3af', marginTop: 4, display: 'block' }}>
                  오늘 0건 발송됨
                </Text>
              </Card>
            </Col>
            <Col xs={24} sm={12}>
              <Card
                style={{
                  borderRadius: 10,
                  border: '1px solid #e5e8ed',
                  background: '#fafbfc',
                }}
                bodyStyle={{ padding: 20 }}
              >
                <Text style={{ fontSize: 13, color: '#6b7280', fontWeight: 500 }}>
                  월간 쿼터
                </Text>
                <div
                  style={{
                    fontSize: 24,
                    fontWeight: 700,
                    color: '#111827',
                    margin: '8px 0 4px',
                  }}
                >
                  {tenant.quotaMonthly.toLocaleString()}
                  <Text style={{ fontSize: 14, color: '#9ca3af', fontWeight: 400, marginLeft: 4 }}>
                    건 / 월
                  </Text>
                </div>
                <Progress
                  percent={Math.min(100, Math.round((0 / tenant.quotaMonthly) * 100))}
                  strokeColor="#f79009"
                  trailColor="#e5e8ed"
                  showInfo={false}
                  style={{ margin: 0 }}
                />
                <Text style={{ fontSize: 12, color: '#9ca3af', marginTop: 4, display: 'block' }}>
                  이번 달 0건 발송됨
                </Text>
              </Card>
            </Col>
          </Row>
        </div>
      ),
    },
    {
      key: 'apikey',
      label: (
        <Space size={6}>
          <KeyOutlined />
          API Key
        </Space>
      ),
      children: (
        <div style={{ padding: '8px 0' }}>
          <Alert
            type="info"
            showIcon
            message="API Key 사용 안내"
            description={
              <div style={{ fontSize: 13 }}>
                ESM API 호출 시 Authorization 헤더에 사용되는 인증 키입니다.<br />
                테넌트별로 고유하며, 이 키로 발신 테넌트를 식별합니다.<br />
                외부에 노출되지 않도록 주의하세요.
              </div>
            }
            style={{ marginBottom: 16, borderRadius: 8 }}
          />

          <div style={{ marginBottom: 16 }}>
            <Title level={5} style={{ margin: 0 }}>서비스 API Key</Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              이 키는 서버 간 통신에만 사용하세요.
            </Text>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: '#f1f5f9',
              border: '1px solid #e2e8f0',
              borderRadius: 8,
              padding: '10px 16px',
              marginBottom: 16,
            }}
          >
            <ApiOutlined style={{ color: '#94a3b8' }} />
            <Text
              style={{
                flex: 1,
                fontFamily: 'monospace',
                fontSize: 13,
                color: '#374151',
                letterSpacing: '0.5px',
              }}
            >
              {apiKeyVisible ? tenant.apiKey : maskedApiKey}
            </Text>
            <Button
              type="text"
              size="small"
              icon={apiKeyVisible ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              onClick={() => setApiKeyVisible(!apiKeyVisible)}
              style={{ color: '#94a3b8' }}
            />
            <Typography.Text
              copyable={{ text: tenant.apiKey }}
              style={{ color: '#94a3b8' }}
            />
          </div>

          <Popconfirm
            title="API 키 재생성"
            description="API 키를 재생성하면 기존 키는 더 이상 사용할 수 없습니다. 계속하시겠습니까?"
            onConfirm={handleRegenerateApiKey}
            okText="재생성"
            cancelText="취소"
            okButtonProps={{ danger: true }}
          >
            <Button
              danger
              icon={<ReloadOutlined />}
              loading={isRegenerating}
              style={{ borderRadius: 8 }}
            >
              API Key 재생성
            </Button>
          </Popconfirm>
        </div>
      ),
    },
    {
      key: 'senders',
      label: (
        <Space size={6}>
          <MailOutlined />
          발신자 관리
        </Space>
      ),
      children: (
        <div style={{ padding: '8px 0' }}>
          {/* 도메인 미인증 시 안내 배너 */}
          {tenant.verificationStatus !== 'VERIFIED' && (
            <Alert
              type="warning"
              showIcon
              message="도메인이 인증되지 않았습니다"
              description="발신자를 추가하면 이메일 개별 인증이 필요합니다. 도메인 인증을 완료하면 발신자를 바로 등록할 수 있습니다."
              style={{ marginBottom: 16, borderRadius: 8 }}
            />
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div>
              <Title level={5} style={{ margin: 0 }}>등록된 발신자</Title>
              <Text type="secondary" style={{ fontSize: 13 }}>
                @{tenant.domain} 도메인의 이메일만 등록 가능합니다.
              </Text>
            </div>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                senderForm.resetFields();
                setSenderModalOpen(true);
              }}
              style={{ borderRadius: 8 }}
            >
              발신자 추가
            </Button>
          </div>

          {isSendersLoading ? (
            <Skeleton active />
          ) : !senders || senders.length === 0 ? (
            <Card style={{ borderRadius: 10, border: '1px solid #e5e8ed', background: '#fafbfc', textAlign: 'center', padding: 32 }}>
              <MailOutlined style={{ fontSize: 32, color: '#d1d5db', marginBottom: 8 }} />
              <div>
                <Text type="secondary">등록된 발신자가 없습니다.</Text>
              </div>
              <div style={{ marginTop: 4 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>발신자를 추가하면 이메일 발송 시 선택할 수 있습니다.</Text>
              </div>
            </Card>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {senders.map((sender) => (
                <Card
                  key={sender.id}
                  size="small"
                  style={{ borderRadius: 8, border: '1px solid #e5e8ed' }}
                  bodyStyle={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                    <div style={{
                      width: 36, height: 36, borderRadius: 8,
                      background: 'rgba(22,119,255,0.08)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      flexShrink: 0,
                    }}>
                      <MailOutlined style={{ color: '#1677ff' }} />
                    </div>
                    <div>
                      <Text strong style={{ fontSize: 14 }}>{sender.email}</Text>
                      {sender.displayName && (
                        <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                          {sender.displayName}
                        </Text>
                      )}
                    </div>
                    {sender.isDefault && (
                      <span style={{
                        fontSize: 11, fontWeight: 600, color: '#1677ff',
                        background: 'rgba(22,119,255,0.08)', padding: '2px 8px',
                        borderRadius: 4, border: '1px solid rgba(22,119,255,0.2)',
                      }}>
                        기본
                      </span>
                    )}
                    {/* 도메인 미인증 시 이메일 개별 인증 상태 표시 */}
                    {tenant.verificationStatus !== 'VERIFIED' && (
                      <SenderVerificationBadge
                        tenantId={id}
                        email={sender.email}
                        onResend={(email) => {
                          resendVerification(
                            { tenantId: id, email },
                            { onSuccess: () => void message.success('인증 이메일을 재발송했습니다.') },
                          );
                        }}
                      />
                    )}
                    {/* 도메인 인증 완료 시 인증됨 Badge */}
                    {tenant.verificationStatus === 'VERIFIED' && (
                      <Tag
                        icon={<CheckCircleOutlined />}
                        color="success"
                        style={{ borderRadius: 6, fontSize: 12 }}
                      >
                        도메인 인증됨
                      </Tag>
                    )}
                  </div>
                  <Popconfirm
                    title="발신자 삭제"
                    description={`${sender.email}을 삭제하시겠습니까?`}
                    onConfirm={() => {
                      removeSender({ tenantId: id, email: sender.email }, {
                        onSuccess: () => void message.success('발신자가 삭제되었습니다.'),
                      });
                    }}
                    okText="삭제"
                    cancelText="취소"
                    okButtonProps={{ danger: true }}
                  >
                    <Button type="text" danger icon={<DeleteOutlined />} size="small" />
                  </Popconfirm>
                </Card>
              ))}
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'dns',
      label: (
        <Space size={6}>
          <GlobalOutlined />
          DNS 인증
          {tenant.verificationStatus === 'VERIFIED' && (
            <Tag color="success" style={{ borderRadius: 6, fontSize: 11, marginLeft: 4 }}>완료</Tag>
          )}
        </Space>
      ),
      children: (
        <div style={{ padding: '8px 0' }}>
          {/* 인증 상태 배너 */}
          {tenant.verificationStatus === 'VERIFIED' ? (
            <Alert
              type="success"
              showIcon
              icon={<CheckCircleOutlined />}
              message="도메인 인증이 완료되었습니다"
              description={`${tenant.domain} 도메인의 DKIM 인증이 확인되었습니다. 모든 발신자가 자동으로 인증됩니다.`}
              style={{ marginBottom: 16, borderRadius: 8 }}
            />
          ) : (
            <Alert
              type="info"
              showIcon
              message="DNS 레코드를 추가하여 도메인을 인증하세요"
              description="DKIM 레코드를 DNS에 추가하면 도메인의 모든 발신자가 자동 인증됩니다. DNS 전파에 최대 72시간이 소요될 수 있습니다."
              style={{ marginBottom: 16, borderRadius: 8 }}
            />
          )}

          {/* 이메일 인증 3요소 안내 */}
          <Card
            style={{ marginBottom: 16, borderRadius: 10, border: '1px solid #e5e8ed', background: '#fafbfc' }}
            styles={{ body: { padding: '16px 20px' } }}
          >
            <Text strong style={{ fontSize: 14, display: 'block', marginBottom: 12 }}>이메일 인증 3요소</Text>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                {
                  label: 'SPF (Sender Policy Framework)',
                  desc: '발신 서버의 정당성을 검증합니다.',
                  tag: '자동 처리',
                  tagColor: 'success',
                  detail: 'AWS SES를 통해 발송하면 SPF는 자동으로 처리됩니다. 별도 설정이 필요 없습니다.',
                },
                {
                  label: 'DKIM (DomainKeys Identified Mail)',
                  desc: '이메일에 디지털 서명을 추가하여 위변조를 방지합니다.',
                  tag: 'DNS 설정 필요',
                  tagColor: 'warning',
                  detail: '아래 DKIM 레코드(CNAME)를 DNS에 추가해야 합니다.',
                },
                {
                  label: 'DMARC (Domain-based Message Authentication)',
                  desc: 'SPF/DKIM 인증 실패 시 수신 서버의 처리 방식을 지정합니다.',
                  tag: 'DNS 설정 권장',
                  tagColor: 'warning',
                  detail: 'TXT 레코드를 DNS에 추가하여 DMARC 정책을 설정합니다.',
                },
              ].map((item) => (
                <div
                  key={item.label}
                  style={{
                    display: 'flex', alignItems: 'flex-start', gap: 12,
                    padding: '10px 12px', borderRadius: 8, background: '#fff', border: '1px solid #e5e8ed',
                  }}
                >
                  <Tag color={item.tagColor} style={{ borderRadius: 6, fontSize: 11, flexShrink: 0, marginTop: 2, width: 90, textAlign: 'center' }}>
                    {item.tag}
                  </Tag>
                  <div>
                    <Text strong style={{ fontSize: 13 }}>{item.label}</Text>
                    <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>{item.desc}</div>
                    <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 2 }}>{item.detail}</div>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* DKIM 레코드 */}
          <Card
            title={
              <Space>
                <SafetyOutlined style={{ color: '#1677ff' }} />
                <Text style={{ fontWeight: 600 }}>DKIM 레코드</Text>
              </Space>
            }
            style={{ marginBottom: 16, borderRadius: 10, border: '1px solid #e5e8ed' }}
            styles={{ body: { padding: '0 16px' } }}
          >
            {dkimRecords?.dkimRecords && dkimRecords.dkimRecords.length > 0 ? (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #e5e8ed' }}>
                    <th style={{ padding: '10px 8px', textAlign: 'left', color: '#6b7280', fontWeight: 500 }}>타입</th>
                    <th style={{ padding: '10px 8px', textAlign: 'left', color: '#6b7280', fontWeight: 500 }}>호스트</th>
                    <th style={{ padding: '10px 8px', textAlign: 'left', color: '#6b7280', fontWeight: 500 }}>값</th>
                    <th style={{ padding: '10px 8px', width: 40 }}></th>
                  </tr>
                </thead>
                <tbody>
                  {dkimRecords.dkimRecords.map((rec: { name: string; type: string; value: string }) => (
                    <tr key={rec.name} style={{ borderBottom: '1px solid #f5f5f5' }}>
                      <td style={{ padding: '10px 8px' }}>
                        <Tag style={{ borderRadius: 4 }}>{rec.type}</Tag>
                      </td>
                      <td style={{ padding: '10px 8px', fontFamily: 'monospace', fontSize: 12, wordBreak: 'break-all' }}>
                        {rec.name}
                      </td>
                      <td style={{ padding: '10px 8px', fontFamily: 'monospace', fontSize: 12, wordBreak: 'break-all' }}>
                        {rec.value}
                      </td>
                      <td style={{ padding: '10px 8px' }}>
                        <Button
                          type="text"
                          size="small"
                          icon={<CopyOutlined />}
                          onClick={() => {
                            void navigator.clipboard.writeText(rec.value);
                            void message.success('복사되었습니다.');
                          }}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#9ca3af', fontSize: 13 }}>
                DKIM 레코드가 없습니다.<br />
                테넌트 생성 시 SES Identity가 등록되지 않았을 수 있습니다.<br />
                테넌트를 삭제 후 재생성하거나, AWS 콘솔에서 직접 SES Identity를 등록하세요.
              </div>
            )}
          </Card>

          {/* DMARC 안내 */}
          <Alert
            type="warning"
            showIcon
            message="DMARC 레코드 추가 (권장)"
            description={
              <div style={{ fontSize: 13 }}>
                <p style={{ margin: '4px 0 8px' }}>
                  DMARC는 SPF/DKIM 인증 실패 시 수신 서버의 처리 방식을 지정합니다. 아래 TXT 레코드를 DNS에 추가하세요.
                </p>
                <div style={{
                  background: '#f6f8fa', borderRadius: 6, padding: '10px 14px',
                  fontFamily: 'monospace', fontSize: 12, marginBottom: 8,
                  border: '1px solid #e5e8ed', wordBreak: 'break-all',
                }}>
                  <div><strong>호스트:</strong> _dmarc.{tenant.domain}</div>
                  <div><strong>타입:</strong> TXT</div>
                  <div><strong>값:</strong> v=DMARC1; p=none; rua=mailto:dmarc-reports@{tenant.domain}</div>
                </div>
                <ul style={{ margin: 0, paddingLeft: 18, color: '#6b7280', fontSize: 12 }}>
                  <li><strong>p=none</strong>: 모니터링만 (처음 시작 시 권장)</li>
                  <li><strong>p=quarantine</strong>: 인증 실패 시 스팸함으로 이동</li>
                  <li><strong>p=reject</strong>: 인증 실패 시 수신 거부</li>
                </ul>
              </div>
            }
            style={{ marginBottom: 16, borderRadius: 8 }}
          />

          {/* DNS 설정 가이드 */}
          <Card
            title={
              <Space>
                <GlobalOutlined style={{ color: '#7c3aed' }} />
                <Text style={{ fontWeight: 600 }}>DNS 레코드 추가 방법</Text>
              </Space>
            }
            style={{ marginBottom: 16, borderRadius: 10, border: '1px solid #e5e8ed' }}
            styles={{ body: { padding: '16px 20px' } }}
          >
            <div style={{ fontSize: 13, color: '#374151', lineHeight: 2 }}>
              <Text strong style={{ fontSize: 14, display: 'block', marginBottom: 8 }}>1. DNS 관리 페이지 접속</Text>
              <div style={{ paddingLeft: 16, marginBottom: 12 }}>
                도메인을 구매한 업체(가비아, 카페24, AWS Route 53, Cloudflare 등)의 관리 페이지에 로그인하세요.
              </div>

              <Text strong style={{ fontSize: 14, display: 'block', marginBottom: 8 }}>2. DKIM 레코드 추가 (CNAME)</Text>
              <div style={{ paddingLeft: 16, marginBottom: 12 }}>
                위 DKIM 레코드 테이블의 각 행에 대해:<br />
                - <strong>레코드 타입</strong>: CNAME 선택<br />
                - <strong>호스트/이름</strong>: 테이블의 "호스트" 값 입력<br />
                - <strong>값/대상</strong>: 테이블의 "값" 입력<br />
                - <strong>TTL</strong>: 기본값 또는 3600 (1시간)
              </div>

              <Text strong style={{ fontSize: 14, display: 'block', marginBottom: 8 }}>3. DMARC 레코드 추가 (TXT)</Text>
              <div style={{ paddingLeft: 16, marginBottom: 12 }}>
                - <strong>레코드 타입</strong>: TXT 선택<br />
                - <strong>호스트/이름</strong>: <code style={{ background: '#f1f5f9', padding: '1px 6px', borderRadius: 4 }}>_dmarc</code><br />
                - <strong>값</strong>: 위 DMARC 안내의 값 입력<br />
                - <strong>TTL</strong>: 기본값 또는 3600
              </div>

              <Text strong style={{ fontSize: 14, display: 'block', marginBottom: 8 }}>4. 전파 대기</Text>
              <div style={{ paddingLeft: 16 }}>
                DNS 레코드 추가 후 전파까지 최대 <strong>72시간</strong>이 소요될 수 있습니다.<br />
                전파가 완료되면 위 인증 상태가 자동으로 업데이트됩니다.
              </div>
            </div>
          </Card>

          {/* 활성화 버튼 */}
          {tenant.status !== 'ACTIVE' && (
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                loading={isActivating}
                onClick={() => {
                  activateTenant(id, {
                    onSuccess: () => void message.success('테넌트가 활성화되었습니다.'),
                    onError: () => void message.error('활성화에 실패했습니다. DNS 인증 상태를 확인하세요.'),
                  });
                }}
                style={{ borderRadius: 8 }}
              >
                테넌트 활성화
              </Button>
            </div>
          )}
        </div>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title={tenant.tenantName}
        subtitle={tenant.domain}
        breadcrumbs={[
          { title: '홈', href: '/' },
          { title: '테넌트 관리', href: '/tenant' },
          { title: tenant.tenantName },
        ]}
        actions={
          <>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/tenant')}
              style={{ borderRadius: 8 }}
            >
              목록으로
            </Button>
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={handleEditOpen}
              style={{ borderRadius: 8 }}
            >
              수정
            </Button>
          </>
        }
      />

      <Card
        style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
        bodyStyle={{ padding: '0 24px 24px' }}
      >
        <Tabs items={tabItems} size="large" />
      </Card>

      {/* 수정 모달 */}
      <Modal
        centered
        title={
          <Space>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: 'rgba(22,119,255,0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <EditOutlined style={{ color: '#1677ff' }} />
            </div>
            <div>
              <div style={{ fontWeight: 600, fontSize: 15 }}>테넌트 수정</div>
              <div style={{ fontSize: 13, color: '#9ca3af', fontWeight: 400 }}>
                {tenant.tenantName}
              </div>
            </div>
          </Space>
        }
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={() => form.submit()}
        okText="저장"
        cancelText="취소"
        confirmLoading={isUpdating}
        width={480}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleEditSubmit}
          style={{ marginTop: 16 }}
          requiredMark={false}
        >
          <Form.Item
            name="tenantName"
            label={<Text style={{ fontWeight: 500 }}>테넌트명</Text>}
            rules={[{ required: true, message: '테넌트명을 입력해 주세요.' }]}
          >
            <Input size="large" />
          </Form.Item>
          <Form.Item
            name="quotaDaily"
            label={<Text style={{ fontWeight: 500 }}>일일 쿼터 (건)</Text>}
          >
            <InputNumber<number>
              min={0}
              size="large"
              style={{ width: '100%' }}
              formatter={(val) => `${val ?? 0}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(val) => Number((val ?? '').replace(/,/g, '')) as number}
              addonAfter="건"
            />
          </Form.Item>
          <Form.Item
            name="quotaMonthly"
            label={<Text style={{ fontWeight: 500 }}>월간 쿼터 (건)</Text>}
          >
            <InputNumber<number>
              min={0}
              size="large"
              style={{ width: '100%' }}
              formatter={(val) => `${val ?? 0}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(val) => Number((val ?? '').replace(/,/g, '')) as number}
              addonAfter="건"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 발신자 추가 모달 */}
      <Modal
        centered
        title={
          <Space>
            <div style={{
              width: 32, height: 32, borderRadius: 8,
              background: 'rgba(22,119,255,0.1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <MailOutlined style={{ color: '#1677ff' }} />
            </div>
            <div>
              <div style={{ fontWeight: 600, fontSize: 15 }}>발신자 추가</div>
              <div style={{ fontSize: 13, color: '#9ca3af', fontWeight: 400 }}>
                @{tenant.domain} 도메인만 허용됩니다.
              </div>
            </div>
          </Space>
        }
        footer={
          tenant.verificationStatus !== 'VERIFIED' ? (
            <div>
              <Alert
                type="info"
                showIcon
                icon={<MailOutlined />}
                message="이메일 인증이 필요합니다"
                description="도메인이 인증되지 않아 발신자 등록 후 인증 이메일이 자동 발송됩니다. 메일함을 확인하고 인증 링크를 클릭하세요."
                style={{ marginBottom: 12, borderRadius: 8, textAlign: 'left' }}
              />
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                <Button onClick={() => setSenderModalOpen(false)}>취소</Button>
                <Button
                  type="primary"
                  loading={isAddingSender || isVerifyingEmail}
                  onClick={() => senderForm.submit()}
                >
                  등록 및 인증 이메일 발송
                </Button>
              </div>
            </div>
          ) : undefined
        }
        open={senderModalOpen}
        onCancel={() => setSenderModalOpen(false)}
        onOk={tenant.verificationStatus === 'VERIFIED' ? () => senderForm.submit() : undefined}
        okText="등록"
        cancelText="취소"
        confirmLoading={isAddingSender || isVerifyingEmail}
        width={480}
      >
        <Form
          form={senderForm}
          layout="vertical"
          onFinish={(values: { email: string; displayName?: string; isDefault?: boolean }) => {
            addSender(
              { tenantId: id, sender: values },
              {
                onSuccess: () => {
                  // 도메인 미인증 시 이메일 인증 요청 자동 발송
                  if (tenant.verificationStatus !== 'VERIFIED') {
                    verifyEmail(
                      { tenantId: id, email: values.email },
                      {
                        onSuccess: () => {
                          void message.success('발신자가 등록되었습니다. 인증 이메일을 확인하세요.');
                        },
                        onError: () => {
                          void message.warning('발신자가 등록되었으나 인증 이메일 발송에 실패했습니다.');
                        },
                      },
                    );
                  } else {
                    void message.success('발신자가 등록되었습니다.');
                  }
                  setSenderModalOpen(false);
                  senderForm.resetFields();
                },
              },
            );
          }}
          style={{ marginTop: 16 }}
          requiredMark={false}
        >
          <Form.Item
            name="email"
            label={<Text style={{ fontWeight: 500 }}>발신자 이메일</Text>}
            rules={[
              { required: true, message: '이메일을 입력하세요.' },
              { type: 'email', message: '올바른 이메일 형식이 아닙니다.' },
              {
                validator: (_, value) => {
                  if (value && !value.endsWith(`@${tenant.domain}`)) {
                    return Promise.reject(`@${tenant.domain} 도메인만 허용됩니다.`);
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Input
              placeholder={`예: no-reply@${tenant.domain}`}
              prefix={<MailOutlined style={{ color: '#9ca3af' }} />}
              size="large"
            />
          </Form.Item>
          <Form.Item
            name="displayName"
            label={<Text style={{ fontWeight: 500 }}>표시 이름 (선택)</Text>}
          >
            <Input placeholder="예: MyCompany 고객센터" size="large" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
