import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Result,
  Row,
  Space,
  Spin,
  Steps,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleFilled,
  CopyOutlined,
  GlobalOutlined,
  MailOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  ThunderboltFilled,
} from '@ant-design/icons';
import { ProDescriptions } from '@ant-design/pro-components';
import { useNavigate } from 'react-router-dom';
import {
  useActivateTenant,
  useDkimRecords,
  useOnboardingStatus,
  useStartOnboarding,
} from '@/hooks/useOnboarding';
import type { OnboardingResult } from '@/types/onboarding';
import type { OnboardingStartRequest } from '@/types/onboarding';
import PageHeader from '@/components/PageHeader';

const { Title, Text } = Typography;

export default function OnboardingWizard() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(0);
  const [tenantId, setTenantId] = useState('');
  const [onboardingResult, setOnboardingResult] = useState<OnboardingResult | null>(null);
  const [activated, setActivated] = useState(false);

  const [form] = Form.useForm<OnboardingStartRequest>();

  const startOnboarding = useStartOnboarding();
  const { data: dkimRecords } = useDkimRecords(tenantId);
  const { data: onboardingStatus } = useOnboardingStatus(tenantId);
  const activateTenant = useActivateTenant();

  const handleStartSubmit = async () => {
    const values = await form.validateFields();
    startOnboarding.mutate(values, {
      onSuccess: (result) => {
        setOnboardingResult(result);
        setTenantId(result.tenantId);
        setCurrentStep(1);
      },
      onError: () => {
        void message.error('온보딩 시작에 실패했습니다.');
      },
    });
  };

  const handleCopy = (text: string) => {
    void navigator.clipboard.writeText(text).then(() => {
      void message.success('클립보드에 복사되었습니다.');
    });
  };

  const isVerified =
    onboardingStatus?.verificationStatus === 'VERIFIED' ||
    onboardingStatus?.steps?.every((s) => s.status === 'COMPLETED');

  const handleActivate = () => {
    activateTenant.mutate(tenantId, {
      onSuccess: () => {
        setActivated(true);
      },
      onError: () => {
        void message.error('활성화에 실패했습니다.');
      },
    });
  };

  const stepStatusMap = (status: string) => {
    if (status === 'COMPLETED') return 'finish';
    if (status === 'WAITING') return 'process';
    return 'wait';
  };

  const dkimColumns = [
    {
      title: '이름 (Name)',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
      render: (val: string) => (
        <Text style={{ fontFamily: 'monospace', fontSize: 11 }}>{val}</Text>
      ),
    },
    {
      title: '유형',
      dataIndex: 'type',
      key: 'type',
      width: 70,
      render: (val: string) => (
        <Tag style={{ borderRadius: 6, fontFamily: 'monospace', fontSize: 11 }}>{val}</Tag>
      ),
    },
    {
      title: '값 (Value)',
      dataIndex: 'value',
      key: 'value',
      ellipsis: true,
      render: (val: string) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Text
            style={{
              fontFamily: 'monospace',
              fontSize: 11,
              color: '#374151',
              flex: 1,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {val}
          </Text>
          <Button
            type="text"
            size="small"
            icon={<CopyOutlined />}
            onClick={() => handleCopy(val)}
            style={{ color: '#9ca3af', flexShrink: 0 }}
          />
        </div>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="온보딩 마법사"
        subtitle="새 테넌트의 도메인을 등록하고 이메일 발송 환경을 설정합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '온보딩' }]}
      />

      <Card
        style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
        bodyStyle={{ padding: '24px 24px 32px' }}
      >
        {/* ─── Steps 헤더 ─── */}
        <Steps
          current={currentStep}
          style={{ marginBottom: 32 }}
          items={[
            { title: '도메인 등록', icon: <GlobalOutlined /> },
            { title: 'DNS 설정', icon: <SafetyCertificateOutlined /> },
            { title: '인증 확인', icon: <CheckCircleFilled /> },
            { title: '활성화', icon: <RocketOutlined /> },
          ]}
        />

        {/* ─── Step 1: 도메인 등록 ─── */}
        {currentStep === 0 && (
          <Form
            form={form}
            layout="vertical"
            requiredMark={false}
          >
            <Card
              title={
                <Space>
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: 8,
                      background: 'rgba(22,119,255,0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <TeamOutlined style={{ color: '#1677ff', fontSize: 13 }} />
                  </div>
                  <Title level={5} style={{ margin: 0 }}>테넌트 정보</Title>
                </Space>
              }
              style={{ marginBottom: 24, borderRadius: 12, border: '1px solid #e5e8ed' }}
              bodyStyle={{ padding: '20px 24px' }}
            >
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="tenantName"
                    label={<Text style={{ fontWeight: 500 }}>테넌트 이름</Text>}
                    rules={[{ required: true, message: '테넌트 이름을 입력하세요.' }]}
                  >
                    <Input
                      size="large"
                      placeholder="예: My Company"
                      prefix={<TeamOutlined style={{ color: '#9ca3af' }} />}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="domain"
                    label={<Text style={{ fontWeight: 500 }}>도메인</Text>}
                    rules={[{ required: true, message: '도메인을 입력하세요.' }]}
                    extra={
                      <Text style={{ fontSize: 12, color: '#9ca3af' }}>
                        이메일 발송에 사용할 도메인입니다. (예: example.com)
                      </Text>
                    }
                  >
                    <Input
                      size="large"
                      placeholder="example.com"
                      prefix={<GlobalOutlined style={{ color: '#9ca3af' }} />}
                    />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="contactEmail"
                    label={<Text style={{ fontWeight: 500 }}>담당자 이메일 (선택)</Text>}
                    style={{ marginBottom: 0 }}
                  >
                    <Input
                      size="large"
                      placeholder="admin@example.com"
                      prefix={<MailOutlined style={{ color: '#9ca3af' }} />}
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                type="primary"
                size="large"
                onClick={() => void handleStartSubmit()}
                loading={startOnboarding.isPending}
                icon={<RocketOutlined />}
                style={{ borderRadius: 8, minWidth: 120 }}
              >
                다음 단계
              </Button>
            </div>
          </Form>
        )}

        {/* ─── Step 2: DNS 설정 ─── */}
        {currentStep === 1 && (
          <div>
            <Alert
              type="info"
              showIcon
              message="DNS 레코드 추가 안내"
              description="아래 DKIM 레코드를 DNS 공급업체에 추가하세요. DNS 전파에 최대 72시간이 소요될 수 있습니다."
              style={{ marginBottom: 20, borderRadius: 8 }}
            />

            <Card
              size="small"
              style={{ marginBottom: 24, borderRadius: 8, border: '1px solid #e5e8ed' }}
              bodyStyle={{ padding: 0 }}
            >
              <Table
                dataSource={dkimRecords?.dkimRecords ?? onboardingResult?.dkimRecords?.dkimRecords ?? []}
                columns={dkimColumns}
                rowKey="name"
                pagination={false}
                size="small"
                locale={{ emptyText: 'DKIM 레코드를 불러오는 중...' }}
              />
            </Card>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
              <Button onClick={() => setCurrentStep(0)} style={{ borderRadius: 8 }}>
                이전
              </Button>
              <Button
                type="primary"
                onClick={() => setCurrentStep(2)}
                style={{ borderRadius: 8 }}
              >
                DNS 추가 완료, 다음으로
              </Button>
            </div>
          </div>
        )}

        {/* ─── Step 3: 인증 확인 ─── */}
        {currentStep === 2 && (
          <div>
            {onboardingStatus ? (
              <>
                <Steps
                  direction="vertical"
                  size="small"
                  style={{ marginBottom: 24, maxWidth: 400 }}
                  items={onboardingStatus.steps.map((s) => ({
                    title: s.name,
                    status: stepStatusMap(s.status),
                    description:
                      s.status === 'COMPLETED' ? (
                        <Text style={{ fontSize: 12, color: '#12b76a' }}>완료됨</Text>
                      ) : s.status === 'WAITING' ? (
                        <Text style={{ fontSize: 12, color: '#f79009' }}>확인 중...</Text>
                      ) : (
                        <Text style={{ fontSize: 12, color: '#9ca3af' }}>대기 중</Text>
                      ),
                  }))}
                />

                {isVerified ? (
                  <Result
                    icon={<CheckCircleFilled style={{ color: '#12b76a', fontSize: 48 }} />}
                    title="인증 완료"
                    subTitle="모든 DNS 레코드가 성공적으로 확인되었습니다."
                    style={{ padding: '24px 0' }}
                    extra={[
                      <Button
                        type="primary"
                        key="next"
                        icon={<RocketOutlined />}
                        size="large"
                        onClick={() => setCurrentStep(3)}
                        style={{ borderRadius: 8 }}
                      >
                        활성화 단계로 이동
                      </Button>,
                    ]}
                  />
                ) : (
                  <Alert
                    type="warning"
                    showIcon
                    icon={<Spin size="small" />}
                    message="인증 상태 확인 중..."
                    description="10초마다 자동으로 상태를 갱신합니다."
                    style={{ borderRadius: 8 }}
                  />
                )}
              </>
            ) : (
              <div style={{ textAlign: 'center', padding: 40 }}>
                <Spin size="large" />
                <div style={{ marginTop: 12 }}>
                  <Text type="secondary">상태 로딩 중...</Text>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ─── Step 4: 활성화 ─── */}
        {currentStep === 3 && (
          <div>
            {activated ? (
              <Result
                icon={<ThunderboltFilled style={{ color: '#12b76a', fontSize: 48 }} />}
                title="테넌트 활성화 완료!"
                subTitle="이메일 발송 서비스가 활성화되었습니다."
                style={{ padding: '24px 0' }}
                extra={[
                  <Button
                    type="primary"
                    key="detail"
                    size="large"
                    onClick={() => navigate(`/tenant/${tenantId}`)}
                    style={{ borderRadius: 8 }}
                  >
                    테넌트 상세 보기
                  </Button>,
                  <Button
                    key="home"
                    size="large"
                    onClick={() => navigate('/')}
                    style={{ borderRadius: 8 }}
                  >
                    대시보드로 이동
                  </Button>,
                ]}
              />
            ) : (
              <div>
                <Card
                  style={{
                    marginBottom: 24,
                    borderRadius: 10,
                    border: '1px solid #e5e8ed',
                    background: '#fafbfc',
                  }}
                  bodyStyle={{ padding: 20 }}
                >
                  <ProDescriptions
                    column={1}
                    title={<Text style={{ fontWeight: 600 }}>테넌트 정보 요약</Text>}
                    dataSource={onboardingResult?.tenant ?? undefined}
                    columns={[
                      {
                        title: '테넌트 ID',
                        dataIndex: 'tenantId',
                        render: (val) => (
                          <Text copyable style={{ fontFamily: 'monospace', fontSize: 12 }}>
                            {val as string}
                          </Text>
                        ),
                      },
                      { title: '도메인', dataIndex: 'domain' },
                      {
                        title: '인증 상태',
                        dataIndex: 'verificationStatus',
                        render: (val) => (
                          <Text style={{ color: '#12b76a', fontWeight: 500 }}>
                            {val as string}
                          </Text>
                        ),
                      },
                    ]}
                  />
                </Card>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  <Button
                    onClick={() => setCurrentStep(2)}
                    style={{ borderRadius: 8 }}
                  >
                    이전
                  </Button>
                  <Button
                    type="primary"
                    icon={<RocketOutlined />}
                    size="large"
                    onClick={handleActivate}
                    loading={activateTenant.isPending}
                    style={{ borderRadius: 8 }}
                  >
                    테넌트 활성화
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}
