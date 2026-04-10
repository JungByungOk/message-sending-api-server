import { useEffect, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Select,
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
  SendOutlined,
  SyncOutlined,
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

const POLLING_OPTIONS = [
  { value: '60000', label: '1분' },
  { value: '300000', label: '5분' },
  { value: '600000', label: '10분' },
];

function TestResultCard({ result }: { result: AwsTestResult }) {
  return (
    <Alert
      type={result.connected ? 'success' : 'error'}
      showIcon
      icon={result.connected ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
      message={result.connected ? '연결 성공' : '연결 실패'}
      description={result.message}
      style={{ marginTop: 16 }}
    />
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
        gatewayEndpoint: settings.gatewayEndpoint || '',
        gatewayRegion: settings.gatewayRegion || 'ap-northeast-2',
        gatewayApiKey: '',
        gatewaySendPath: settings.gatewaySendPath || '/send-email',
        gatewayResultsPath: settings.gatewayResultsPath || '/results',
        gatewayConfigPath: settings.gatewayConfigPath || '/config',
        gatewayTenantSetupPath: settings.gatewayTenantSetupPath || '/tenant-setup',
        pollingInterval: settings.pollingInterval || '300000',
      });
    }
  }, [settings, form]);

  const handleSave = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();
      await saveMutation.mutateAsync(values);
      void message.success('설정이 저장되었습니다.');
      setTestResult(null);
    } catch {
      void message.error('설정 저장에 실패했습니다.');
    }
  };

  const handleTest = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();
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
          AWS 연동 설정
        </Title>
        <Badge
          status={settings?.gatewayConfigured ? 'success' : 'default'}
          text={settings?.gatewayConfigured ? '설정됨' : '미설정'}
        />
      </div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        API Gateway 연결 및 발송 결과 수신 방식을 설정합니다.
      </Text>

      <Form form={form} layout="vertical" requiredMark="optional">
        {/* === API Gateway === */}
        <div>
            <Card
              title={<><SendOutlined style={{ marginRight: 8 }} />API Gateway</>}
              size="small"
              extra={
                <Badge
                  status={settings?.gatewayConfigured ? 'success' : 'error'}
                  text={settings?.gatewayConfigured ? '연결됨' : '미연결'}
                />
              }
            >
              <Form.Item
                label="Endpoint URL"
                name="gatewayEndpoint"
                rules={[{ required: true, message: 'Endpoint를 입력하세요' }]}
              >
                <Input placeholder="https://xxxxxxxxxx.execute-api.ap-northeast-2.amazonaws.com/prod" />
              </Form.Item>

              <Row gutter={12}>
                <Col span={6}>
                  <Form.Item label="발송" name="gatewaySendPath">
                    <Input placeholder="/send-email" />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item label="조회" name="gatewayResultsPath">
                    <Input placeholder="/results" />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item label="설정" name="gatewayConfigPath">
                    <Input placeholder="/config" />
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item label="온보딩" name="gatewayTenantSetupPath">
                    <Input placeholder="/tenant-setup" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item label="Region" name="gatewayRegion">
                <Select options={AWS_REGIONS} />
              </Form.Item>

              <Form.Item
                label="API Key"
                name="gatewayApiKey"
                help={settings?.gatewayApiKeyMasked ? `현재: ${settings.gatewayApiKeyMasked}` : undefined}
              >
                <Input.Password placeholder="비워두면 기존 값 유지" />
              </Form.Item>
            </Card>
        </div>

          {/* === Delivery === */}
        <div style={{ marginTop: 16 }}>
            <Card
              title={<><SyncOutlined style={{ marginRight: 8 }} />발송 결과 수신</>}
              size="small"
            >
              <Form.Item label="보정 폴링 주기" name="pollingInterval">
                <Select options={POLLING_OPTIONS} />
              </Form.Item>
            </Card>
        </div>

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
