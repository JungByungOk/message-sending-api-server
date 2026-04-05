import { Button, Card, Col, Row, Skeleton, Space, Steps, Table, Tag, Typography, Alert } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeftOutlined,
  CheckCircleFilled,
  CopyOutlined,
  GlobalOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { useOnboardingStatus, useDkimRecords, useActivateTenant } from '@/hooks/useOnboarding';
import { message } from 'antd';
import PageHeader from '@/components/PageHeader';
import StatusTag from '@/components/StatusTag';

const { Title, Text } = Typography;

export default function OnboardingStatus() {
  const { tenantId = '' } = useParams<{ tenantId: string }>();
  const navigate = useNavigate();

  const { data: status, isLoading, isError } = useOnboardingStatus(tenantId);
  const { data: dkimRecords } = useDkimRecords(tenantId);
  const activateTenant = useActivateTenant();

  const stepStatusMap = (s: string) => {
    if (s === 'COMPLETED') return 'finish';
    if (s === 'WAITING') return 'process';
    return 'wait';
  };

  const isReady =
    status?.verificationStatus === 'VERIFIED' ||
    status?.steps?.every((step) => step.status === 'COMPLETED');

  const handleActivate = () => {
    activateTenant.mutate(tenantId, {
      onSuccess: () => {
        void message.success('테넌트가 활성화되었습니다.');
        navigate(`/tenant/${tenantId}`);
      },
      onError: () => {
        void message.error('활성화에 실패했습니다.');
      },
    });
  };

  const handleCopy = (text: string) => {
    void navigator.clipboard.writeText(text).then(() => {
      void message.success('복사되었습니다.');
    });
  };

  const dkimColumns = [
    {
      title: '이름',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
      render: (val: string) => (
        <Text
          style={{
            fontFamily: 'monospace',
            fontSize: 11,
            background: '#f8fafc',
            padding: '2px 6px',
            borderRadius: 4,
            border: '1px solid #e5e8ed',
          }}
        >
          {val}
        </Text>
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
      title: '값',
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

  if (!tenantId) {
    return (
      <div style={{ padding: 24 }}>
        <Alert type="error" message="테넌트 ID가 필요합니다." showIcon style={{ borderRadius: 8 }} />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div style={{ padding: 24 }}>
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    );
  }

  if (isError || !status) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="온보딩 상태를 불러올 수 없습니다."
          showIcon
          style={{ borderRadius: 8 }}
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="온보딩 상태"
        subtitle={`테넌트 ${status.tenantId} 의 DNS 인증 진행 상태를 확인합니다.`}
        breadcrumbs={[
          { title: '홈', href: '/' },
          { title: '온보딩', href: '/onboarding' },
          { title: '상태 확인' },
        ]}
        actions={
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/onboarding')}
            style={{ borderRadius: 8 }}
          >
            온보딩으로
          </Button>
        }
      />

      {/* ─── 테넌트 정보 카드 ─── */}
      <Card
        style={{ marginBottom: 16, borderRadius: 12, border: '1px solid #e5e8ed' }}
        bodyStyle={{ padding: 24 }}
      >
        <Row gutter={[24, 16]}>
          <Col xs={24} sm={12}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
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
                <GlobalOutlined style={{ color: '#1677ff', fontSize: 18 }} />
              </div>
              <div>
                <Text style={{ fontSize: 12, color: '#9ca3af', display: 'block' }}>도메인</Text>
                <Text
                  style={{
                    fontFamily: 'monospace',
                    fontWeight: 600,
                    color: '#111827',
                    fontSize: 14,
                  }}
                >
                  {status.domain}
                </Text>
              </div>
            </div>
          </Col>
          <Col xs={24} sm={12}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 10,
                  background: 'rgba(18,183,106,0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <SafetyCertificateOutlined style={{ color: '#12b76a', fontSize: 18 }} />
              </div>
              <div>
                <Text style={{ fontSize: 12, color: '#9ca3af', display: 'block' }}>인증 상태</Text>
                <StatusTag type="verification" status={status.verificationStatus} />
              </div>
            </div>
          </Col>
          <Col xs={24} sm={12}>
            <Text style={{ fontSize: 12, color: '#9ca3af', display: 'block', marginBottom: 4 }}>
              테넌트 ID
            </Text>
            <Text copyable style={{ fontFamily: 'monospace', fontSize: 12, color: '#374151' }}>
              {status.tenantId}
            </Text>
          </Col>
          <Col xs={24} sm={12}>
            <Text style={{ fontSize: 12, color: '#9ca3af', display: 'block', marginBottom: 4 }}>
              테넌트 상태
            </Text>
            <StatusTag type="tenant" status={status.tenantStatus} />
          </Col>
        </Row>
      </Card>

      <Row gutter={16}>
        {/* ─── 진행 단계 ─── */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space size={8}>
                <CheckCircleFilled style={{ color: '#1677ff' }} />
                <span style={{ fontWeight: 600 }}>진행 단계</span>
              </Space>
            }
            style={{ marginBottom: 16, borderRadius: 12, border: '1px solid #e5e8ed', height: '100%' }}
            bodyStyle={{ padding: 24 }}
          >
            <Steps
              direction="vertical"
              size="small"
              items={status.steps.map((s) => ({
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
          </Card>
        </Col>

        {/* ─── DKIM 레코드 ─── */}
        {dkimRecords?.dkimRecords && dkimRecords.dkimRecords.length > 0 && (
          <Col xs={24} lg={12}>
            <Card
              title={
                <Space size={8}>
                  <SafetyCertificateOutlined style={{ color: '#7c3aed' }} />
                  <span style={{ fontWeight: 600 }}>DKIM 레코드</span>
                </Space>
              }
              style={{ marginBottom: 16, borderRadius: 12, border: '1px solid #e5e8ed' }}
              bodyStyle={{ padding: 0 }}
            >
              <Table
                dataSource={dkimRecords.dkimRecords}
                columns={dkimColumns}
                rowKey="name"
                pagination={false}
                size="small"
              />
            </Card>
          </Col>
        )}
      </Row>

      {/* ─── 활성화 버튼 ─── */}
      {isReady && status.tenantStatus !== 'ACTIVE' && (
        <Card
          style={{
            borderRadius: 12,
            border: '1px solid rgba(18,183,106,0.3)',
            background: 'rgba(18,183,106,0.04)',
          }}
          bodyStyle={{ padding: 20 }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <Title level={5} style={{ margin: 0, color: '#065f46' }}>
                인증이 완료되었습니다.
              </Title>
              <Text style={{ fontSize: 14, color: '#047857' }}>
                테넌트를 활성화하면 이메일 발송을 시작할 수 있습니다.
              </Text>
            </div>
            <Button
              type="primary"
              icon={<RocketOutlined />}
              size="large"
              onClick={handleActivate}
              loading={activateTenant.isPending}
              style={{
                borderRadius: 8,
                background: '#12b76a',
                borderColor: '#12b76a',
                flexShrink: 0,
              }}
            >
              테넌트 활성화
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}
