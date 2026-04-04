import { Button, Card, Col, Form, Input, InputNumber, Row, Slider, Space, Typography, message } from 'antd';
import {
  ArrowLeftOutlined,
  CheckOutlined,
  GlobalOutlined,
  InfoCircleOutlined,
  MailOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useCreateTenant } from '@/hooks/useTenants';
import type { CreateTenantRequest } from '@/types/tenant';
import PageHeader from '@/components/PageHeader';

const { Title, Text } = Typography;

export default function TenantCreate() {
  const navigate = useNavigate();
  const [form] = Form.useForm<CreateTenantRequest>();
  const { mutate: create, isPending } = useCreateTenant();

  const handleSubmit = (values: CreateTenantRequest) => {
    create(values, {
      onSuccess: () => {
        void message.success('테넌트가 생성되었습니다.');
        navigate('/tenant');
      },
      onError: (error) => {
        const msg: string = (error as { response?: { data?: { message?: string } } })
          ?.response?.data?.message ?? '';

        if (msg.startsWith('DUPLICATE_TENANT_NAME:')) {
          form.setFields([{
            name: 'tenantName',
            errors: [`이미 등록된 테넌트명입니다: ${msg.split(':')[1]}`],
          }]);
        } else if (msg.startsWith('DUPLICATE_DOMAIN:')) {
          form.setFields([{
            name: 'domain',
            errors: [`이미 등록된 도메인입니다: ${msg.split(':')[1]}`],
          }]);
        }
      },
    });
  };

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="테넌트 생성"
        subtitle="새 테넌트를 등록하고 이메일 서비스를 구성합니다."
        breadcrumbs={[
          { title: '홈', href: '/' },
          { title: '테넌트 관리', href: '/tenant' },
          { title: '신규 생성' },
        ]}
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{ quotaDaily: 10000, quotaMonthly: 300000 }}
        requiredMark={false}
      >
        {/* ─── 기본 정보 ─── */}
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
              <Title level={5} style={{ margin: 0 }}>기본 정보</Title>
            </Space>
          }
          style={{ marginBottom: 16, borderRadius: 12, border: '1px solid #e5e8ed' }}
          bodyStyle={{ padding: '20px 24px' }}
        >
          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="tenantName"
                label={<Text style={{ fontWeight: 500 }}>테넌트명</Text>}
                rules={[
                  { required: true, message: '테넌트명을 입력해 주세요.' },
                  { min: 2, message: '최소 2자 이상 입력해 주세요.' },
                ]}
              >
                <Input
                  placeholder="예: My Company"
                  prefix={<TeamOutlined style={{ color: '#9ca3af' }} />}
                  size="large"
                />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="domain"
                label={<Text style={{ fontWeight: 500 }}>도메인</Text>}
                rules={[
                  { required: true, message: '도메인을 입력해 주세요.' },
                  {
                    pattern: /^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?\.[a-zA-Z]{2,}$/,
                    message: '올바른 도메인 형식이 아닙니다. (예: example.com)',
                  },
                ]}
                extra={
                  <Text style={{ fontSize: 12, color: '#9ca3af' }}>
                    <InfoCircleOutlined style={{ marginRight: 4 }} />
                    이메일 발송에 사용할 도메인 (예: example.com)
                  </Text>
                }
              >
                <Input
                  placeholder="example.com"
                  prefix={<GlobalOutlined style={{ color: '#9ca3af' }} />}
                  size="large"
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* ─── 쿼터 설정 ─── */}
        <Card
          title={
            <Space>
              <div
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 8,
                  background: 'rgba(247,144,9,0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <MailOutlined style={{ color: '#f79009', fontSize: 13 }} />
              </div>
              <Title level={5} style={{ margin: 0 }}>발송 쿼터</Title>
            </Space>
          }
          style={{ marginBottom: 24, borderRadius: 12, border: '1px solid #e5e8ed' }}
          bodyStyle={{ padding: '20px 24px' }}
        >
          <Row gutter={32}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="quotaDaily"
                label={<Text style={{ fontWeight: 500 }}>일일 쿼터 (건)</Text>}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <Form.Item name="quotaDaily" noStyle>
                    <InputNumber<number>
                      min={0}
                      max={1000000}
                      size="large"
                      style={{ width: '100%' }}
                      formatter={(val) =>
                        `${val ?? 0}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                      }
                      parser={(val) =>
                        Number((val ?? '').replace(/,/g, '')) as number
                      }
                      addonAfter="건"
                    />
                  </Form.Item>
                  <Form.Item name="quotaDaily" noStyle>
                    <Slider
                      min={0}
                      max={100000}
                      step={1000}
                      tooltip={{ formatter: (v) => `${(v ?? 0).toLocaleString()}건` }}
                    />
                  </Form.Item>
                </div>
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                name="quotaMonthly"
                label={<Text style={{ fontWeight: 500 }}>월간 쿼터 (건)</Text>}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <Form.Item name="quotaMonthly" noStyle>
                    <InputNumber<number>
                      min={0}
                      max={10000000}
                      size="large"
                      style={{ width: '100%' }}
                      formatter={(val) =>
                        `${val ?? 0}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                      }
                      parser={(val) =>
                        Number((val ?? '').replace(/,/g, '')) as number
                      }
                      addonAfter="건"
                    />
                  </Form.Item>
                  <Form.Item name="quotaMonthly" noStyle>
                    <Slider
                      min={0}
                      max={3000000}
                      step={10000}
                      tooltip={{ formatter: (v) => `${(v ?? 0).toLocaleString()}건` }}
                    />
                  </Form.Item>
                </div>
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* ─── 액션 버튼 ─── */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/tenant')}
            size="large"
            style={{ borderRadius: 8 }}
          >
            취소
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            icon={<CheckOutlined />}
            loading={isPending}
            size="large"
            style={{ borderRadius: 8, minWidth: 100 }}
          >
            테넌트 생성
          </Button>
        </div>
      </Form>
    </div>
  );
}
