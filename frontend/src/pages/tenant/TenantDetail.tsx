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
import {
  ApiOutlined,
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
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
import { useTenant, useUpdateTenant, useRegenerateApiKey, useSenders, useAddSender, useRemoveSender } from '@/hooks/useTenants';
import { useEmailVerificationStatus, useResendVerification, useVerifyEmail } from '@/hooks/useOnboarding';
import type { UpdateTenantRequest } from '@/types/tenant';
import type { EmailVerificationStatus } from '@/types/onboarding';
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
  const { data: status, isLoading } = useEmailVerificationStatus(tenantId, email);

  if (isLoading) {
    return <Badge status="processing" text={<Text style={{ fontSize: 12, color: '#9ca3af' }}>확인 중...</Text>} />;
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
            onClick={() => onResend(email)}
            style={{ color: '#f79009', padding: '0 4px' }}
          />
        </Tooltip>
      </Space>
    );
  }

  // PENDING
  return (
    <Space size={4}>
      <Tag
        icon={<ClockCircleOutlined />}
        color="warning"
        style={{ borderRadius: 6, fontSize: 12 }}
      >
        인증 대기중
      </Tag>
      <Tooltip title="인증 이메일 재발송">
        <Button
          type="text"
          size="small"
          icon={<SendOutlined />}
          onClick={() => onResend(email)}
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

  // 이메일 인증 상태 조회를 위한 선택된 발신자 이메일 추적
  const [verifyingEmail, setVerifyingEmail] = useState<string | null>(null);

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
                  {dayjs(tenant.createdAt).format('YYYY-MM-DD HH:mm:ss')}
                </Text>
              ),
            },
            {
              label: '수정일',
              value: (
                <Text style={{ color: '#6b7280', fontSize: 13 }}>
                  {dayjs(tenant.updatedAt).format('YYYY-MM-DD HH:mm:ss')}
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
          <Card
            style={{
              borderRadius: 10,
              border: '1px solid #e5e8ed',
              background: '#fafbfc',
            }}
            bodyStyle={{ padding: 24 }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 10,
                  background: 'rgba(22,119,255,0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <SafetyOutlined style={{ color: '#1677ff', fontSize: 18 }} />
              </div>
              <div>
                <Title level={5} style={{ margin: 0 }}>서비스 API Key</Title>
                <Text style={{ fontSize: 13, color: '#9ca3af' }}>
                  이 키는 외부에 노출되지 않도록 주의하세요.
                </Text>
              </div>
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
          </Card>
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
