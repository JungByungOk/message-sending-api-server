import { useEffect, useState } from 'react';
import {
  Badge,
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloudOutlined,
  ExperimentOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { useAwsSettings, useSaveAwsSettings, useTestAwsConnection } from '@/hooks/useSettings';
import type { AwsSettings, AwsTestResult } from '@/types/settings';

const { Title, Text } = Typography;

const AWS_REGIONS = [
  { value: 'ap-northeast-2', label: 'Asia Pacific (Seoul)' },
  { value: 'ap-northeast-1', label: 'Asia Pacific (Tokyo)' },
  { value: 'ap-southeast-1', label: 'Asia Pacific (Singapore)' },
  { value: 'us-east-1', label: 'US East (N. Virginia)' },
  { value: 'us-west-2', label: 'US West (Oregon)' },
  { value: 'eu-west-1', label: 'Europe (Ireland)' },
  { value: 'eu-central-1', label: 'Europe (Frankfurt)' },
];

function ConnectionBadge({ connected, label }: { connected: boolean; label: string }) {
  return (
    <Badge
      status={connected ? 'success' : 'error'}
      text={
        <Space size={4}>
          {connected ? (
            <CheckCircleOutlined style={{ color: '#52c41a' }} />
          ) : (
            <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
          )}
          <Text style={{ fontSize: 13 }}>{label}</Text>
        </Space>
      }
    />
  );
}

function TestResultCard({ result }: { result: AwsTestResult }) {
  return (
    <Card size="small" style={{ marginTop: 16, backgroundColor: '#fafafa' }}>
      <Title level={5} style={{ marginTop: 0, marginBottom: 12 }}>연결 테스트 결과</Title>
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text strong>AWS SES</Text>
          <ConnectionBadge connected={result.sesConnected} label={result.sesMessage} />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text strong>AWS DynamoDB</Text>
          <ConnectionBadge connected={result.dynamoConnected} label={result.dynamoMessage} />
        </div>
      </Space>
    </Card>
  );
}

export default function AwsSettingsPage() {
  const [form] = Form.useForm<AwsSettings>();
  const { data: settings, isLoading } = useAwsSettings();
  const saveMutation = useSaveAwsSettings();
  const testMutation = useTestAwsConnection();
  const [testResult, setTestResult] = useState<AwsTestResult | null>(null);

  useEffect(() => {
    if (settings) {
      form.setFieldsValue({
        sesRegion: settings.sesRegion || 'ap-northeast-2',
        sesAccessKey: settings.sesAccessKey || '',
        sesSecretKey: '',
        dynamoRegion: settings.dynamoRegion || 'ap-northeast-2',
        dynamoAccessKey: settings.dynamoAccessKey || '',
        dynamoSecretKey: '',
        endpoint: settings.endpoint || '',
      });
    }
  }, [settings, form]);

  const getFormValues = (): AwsSettings => {
    const values = form.getFieldsValue();
    // Secret Key가 비어있으면 기존 값 유지 (마스킹된 값 대신)
    return {
      ...values,
      sesSecretKey: values.sesSecretKey || '',
      dynamoSecretKey: values.dynamoSecretKey || '',
    };
  };

  const handleSave = async () => {
    try {
      await form.validateFields();
      const values = getFormValues();
      await saveMutation.mutateAsync(values);
      void message.success('AWS 설정이 저장되었습니다.');
      setTestResult(null);
    } catch {
      void message.error('설정 저장에 실패했습니다.');
    }
  };

  const handleTest = async () => {
    try {
      await form.validateFields();
      const values = getFormValues();
      const result = await testMutation.mutateAsync(values);
      setTestResult(result);
      if (result.sesConnected && result.dynamoConnected) {
        void message.success('모든 AWS 서비스 연결 성공');
      } else {
        void message.warning('일부 서비스 연결에 실패했습니다.');
      }
    } catch {
      void message.error('연결 테스트에 실패했습니다.');
    }
  };

  if (isLoading) {
    return <Spin tip="설정 로드 중..." />;
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
        <Title level={4} style={{ margin: 0 }}>
          <CloudOutlined style={{ marginRight: 8 }} />
          AWS 연결 설정
        </Title>
        <Space>
          {settings && (
            <Badge
              status={settings.sesConfigured ? 'success' : 'default'}
              text={settings.sesConfigured ? '설정됨' : '미설정'}
            />
          )}
          {settings?.source === 'database' && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              (DB 설정 사용 중)
            </Text>
          )}
        </Space>
      </div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        AWS SES 이메일 발송 및 DynamoDB 이벤트 추적에 사용되는 연결 정보를 관리합니다.
      </Text>

      <Form form={form} layout="vertical" requiredMark="optional">
        <Row gutter={24}>
          {/* SES 설정 */}
          <Col xs={24} lg={12}>
            <Card
              title="AWS SES 설정"
              size="small"
              extra={
                settings && (
                  <Badge
                    status={settings.sesConfigured ? 'success' : 'error'}
                    text={settings.sesConfigured ? '연결됨' : '미연결'}
                  />
                )
              }
            >
              <Form.Item label="Region" name="sesRegion" rules={[{ required: true, message: '리전을 선택하세요' }]}>
                <Select options={AWS_REGIONS} placeholder="리전 선택" />
              </Form.Item>
              <Form.Item label="Access Key" name="sesAccessKey" rules={[{ required: true, message: 'Access Key를 입력하세요' }]}>
                <Input placeholder="AKIA..." />
              </Form.Item>
              <Form.Item
                label="Secret Key"
                name="sesSecretKey"
                help={settings?.sesSecretKeyMasked ? `현재: ${settings.sesSecretKeyMasked} (변경 시에만 입력)` : undefined}
              >
                <Input.Password placeholder="비워두면 기존 값 유지" />
              </Form.Item>
            </Card>
          </Col>

          {/* DynamoDB 설정 */}
          <Col xs={24} lg={12}>
            <Card
              title="AWS DynamoDB 설정"
              size="small"
              extra={
                settings && (
                  <Badge
                    status={settings.dynamoConfigured ? 'success' : 'error'}
                    text={settings.dynamoConfigured ? '연결됨' : '미연결'}
                  />
                )
              }
            >
              <Form.Item label="Region" name="dynamoRegion" rules={[{ required: true, message: '리전을 선택하세요' }]}>
                <Select options={AWS_REGIONS} placeholder="리전 선택" />
              </Form.Item>
              <Form.Item label="Access Key" name="dynamoAccessKey" rules={[{ required: true, message: 'Access Key를 입력하세요' }]}>
                <Input placeholder="AKIA..." />
              </Form.Item>
              <Form.Item
                label="Secret Key"
                name="dynamoSecretKey"
                help={settings?.dynamoSecretKeyMasked ? `현재: ${settings.dynamoSecretKeyMasked} (변경 시에만 입력)` : undefined}
              >
                <Input.Password placeholder="비워두면 기존 값 유지" />
              </Form.Item>
            </Card>
          </Col>
        </Row>

        {/* Endpoint Override */}
        <Card title="Endpoint Override" size="small" style={{ marginTop: 16 }}>
          <Form.Item
            label="Endpoint URL"
            name="endpoint"
            help="LocalStack 등 커스텀 AWS 엔드포인트를 사용하는 경우 입력합니다. 비워두면 AWS 기본 엔드포인트를 사용합니다."
          >
            <Input placeholder="http://localhost:4566" />
          </Form.Item>
        </Card>

        {/* 테스트 결과 */}
        {testResult && <TestResultCard result={testResult} />}

        {/* 버튼 영역 */}
        <div style={{ marginTop: 24, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button
            icon={<ExperimentOutlined />}
            onClick={handleTest}
            loading={testMutation.isPending}
          >
            연결 테스트
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saveMutation.isPending}
          >
            설정 저장
          </Button>
        </div>
      </Form>

      {settings?.updatedAt && (
        <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 12, textAlign: 'right' }}>
          마지막 수정: {new Date(settings.updatedAt).toLocaleString('ko-KR')}
        </Text>
      )}
    </div>
  );
}
