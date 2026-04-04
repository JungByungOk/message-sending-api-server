import { useEffect, useState } from 'react';
import {
  Badge,
  Button,
  Card,
  Form,
  Input,
  Radio,
  Select,
  Space,
  Spin,
  Typography,
  message,
} from 'antd';
import {
  ApiOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
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

function TestResultCard({ result }: { result: AwsTestResult }) {
  return (
    <Card
      size="small"
      style={{
        marginTop: 16,
        backgroundColor: result.connected ? '#f6ffed' : '#fff2f0',
        borderColor: result.connected ? '#b7eb8f' : '#ffccc7',
      }}
    >
      <Space>
        {result.connected ? (
          <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 18 }} />
        ) : (
          <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 18 }} />
        )}
        <div>
          <Text strong>{result.connected ? '연결 성공' : '연결 실패'}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 13 }}>{result.message}</Text>
        </div>
      </Space>
    </Card>
  );
}

export default function AwsSettingsPage() {
  const [form] = Form.useForm<AwsSettings>();
  const authType = Form.useWatch('authType', form);
  const { data: settings, isLoading } = useAwsSettings();
  const saveMutation = useSaveAwsSettings();
  const testMutation = useTestAwsConnection();
  const [testResult, setTestResult] = useState<AwsTestResult | null>(null);

  useEffect(() => {
    if (settings) {
      form.setFieldsValue({
        endpoint: settings.endpoint || '',
        region: settings.region || 'ap-northeast-2',
        authType: (settings.authType as 'API_KEY' | 'IAM') || 'API_KEY',
        apiKey: '',
        accessKey: settings.accessKey || '',
        secretKey: '',
      });
    }
  }, [settings, form]);

  const getFormValues = (): AwsSettings => {
    const values = form.getFieldsValue();
    return {
      ...values,
      apiKey: values.apiKey || '',
      secretKey: values.secretKey || '',
    };
  };

  const handleSave = async () => {
    try {
      await form.validateFields();
      const values = getFormValues();
      await saveMutation.mutateAsync(values);
      void message.success('API Gateway 설정이 저장되었습니다.');
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
      if (result.connected) {
        void message.success('API Gateway 연결 성공');
      } else {
        void message.warning(result.message);
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
          <ApiOutlined style={{ marginRight: 8 }} />
          API Gateway 연결 설정
        </Title>
        <Badge
          status={settings?.configured ? 'success' : 'default'}
          text={settings?.configured ? '설정됨' : '미설정'}
        />
      </div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        이메일 발송 요청이 전달되는 AWS API Gateway 연결 정보를 관리합니다.
      </Text>

      <Form form={form} layout="vertical" requiredMark="optional" initialValues={{ authType: 'API_KEY' }}>
        {/* Endpoint */}
        <Card title="Gateway Endpoint" size="small">
          <Form.Item
            label="Endpoint URL"
            name="endpoint"
            rules={[{ required: true, message: 'Endpoint URL을 입력하세요' }]}
          >
            <Input placeholder="https://xxxxxxxxxx.execute-api.ap-northeast-2.amazonaws.com/prod" />
          </Form.Item>
          <Form.Item
            label="Region"
            name="region"
            rules={[{ required: true, message: '리전을 선택하세요' }]}
          >
            <Select options={AWS_REGIONS} placeholder="리전 선택" />
          </Form.Item>
        </Card>

        {/* Authentication */}
        <Card title="인증 설정" size="small" style={{ marginTop: 16 }}>
          <Form.Item label="인증 방식" name="authType">
            <Radio.Group>
              <Radio.Button value="API_KEY">API Key</Radio.Button>
              <Radio.Button value="IAM">IAM Signature V4</Radio.Button>
            </Radio.Group>
          </Form.Item>

          {authType === 'API_KEY' && (
            <Form.Item
              label="API Key"
              name="apiKey"
              help={settings?.apiKeyMasked ? `현재: ${settings.apiKeyMasked} (변경 시에만 입력)` : undefined}
            >
              <Input.Password placeholder="비워두면 기존 값 유지" />
            </Form.Item>
          )}

          {authType === 'IAM' && (
            <>
              <Form.Item
                label="Access Key"
                name="accessKey"
                rules={[{ required: true, message: 'Access Key를 입력하세요' }]}
              >
                <Input placeholder="AKIA..." />
              </Form.Item>
              <Form.Item
                label="Secret Key"
                name="secretKey"
                help={settings?.secretKeyMasked ? `현재: ${settings.secretKeyMasked} (변경 시에만 입력)` : undefined}
              >
                <Input.Password placeholder="비워두면 기존 값 유지" />
              </Form.Item>
            </>
          )}
        </Card>

        {/* 테스트 결과 */}
        {testResult && <TestResultCard result={testResult} />}

        {/* 버튼 */}
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
