import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
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
  Tabs,
  Typography,
  message,
} from 'antd';
import {
  ApiOutlined,
  ArrowLeftOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  KeyOutlined,
  ReloadOutlined,
  SafetyOutlined,
  SettingOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useTenant, useUpdateTenant, useRegenerateApiKey } from '@/hooks/useTenants';
import type { UpdateTenantRequest } from '@/types/tenant';
import StatusTag from '@/components/StatusTag';
import PageHeader from '@/components/PageHeader';

const { Title, Text } = Typography;

export default function TenantDetail() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: tenant, isLoading } = useTenant(id);
  const { mutate: updateTenant, isPending: isUpdating } = useUpdateTenant();
  const { mutate: regenerate, isPending: isRegenerating } = useRegenerateApiKey();

  const [editModalOpen, setEditModalOpen] = useState(false);
  const [apiKeyVisible, setApiKeyVisible] = useState(false);
  const [form] = Form.useForm<UpdateTenantRequest>();

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
    </div>
  );
}
