import { useState, useEffect } from 'react';
import { Button, Card, Divider, Form, Input, InputNumber, Space, Switch, Typography, message } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { authApi } from '@/api/auth';
import apiClient from '@/api/client';
import AwsSettingsPage from './AwsSettings';
import ThemeSettingsPage from './ThemeSettings';

const { Title, Text } = Typography;

function PasswordChangeSection() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { currentPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      void message.error('새 비밀번호가 일치하지 않습니다.');
      return;
    }
    setLoading(true);
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      form.resetFields();
      void message.success('비밀번호가 변경되었습니다.');
    } catch (error: any) {
      const msg = error.response?.data?.message ?? '비밀번호 변경에 실패했습니다.';
      void message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={4} style={{ margin: 0, marginBottom: 4 }}>비밀번호 변경</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        계정 비밀번호를 변경합니다.
      </Text>
      <Card>
        <Form form={form} layout="vertical" onFinish={onFinish} style={{ maxWidth: 400 }}>
          <Form.Item
            name="currentPassword"
            label="현재 비밀번호"
            rules={[{ required: true, message: '현재 비밀번호를 입력하세요' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="현재 비밀번호" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="새 비밀번호"
            rules={[
              { required: true, message: '새 비밀번호를 입력하세요' },
              { min: 8, message: '비밀번호는 8자 이상이어야 합니다' },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="새 비밀번호" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="새 비밀번호 확인"
            rules={[{ required: true, message: '새 비밀번호를 다시 입력하세요' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="새 비밀번호 확인" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading}>
              비밀번호 변경
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}

function VdmSettings() {
  const [enabled, setEnabled] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    apiClient.get('/settings/vdm').then(res => setEnabled(res.data.enabled)).catch(() => {});
  }, []);

  const handleToggle = async (checked: boolean) => {
    setLoading(true);
    try {
      await apiClient.put('/settings/vdm', { enabled: checked });
      setEnabled(checked);
      void message.success(`VDM ${checked ? '활성화' : '비활성화'} 완료`);
    } catch {
      void message.error('VDM 설정 변경 실패');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={4} style={{ margin: 0, marginBottom: 4 }}>Virtual Deliverability Manager (VDM)</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        ISP별 전달률 인사이트, 자동 전달 최적화 기능을 활성화합니다. ($0.07/1,000건 추가 과금)
      </Text>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <Text strong>VDM 활성화</Text>
            <br />
            <Text type="secondary" style={{ fontSize: 13 }}>
              활성화 시 이메일 발송 건수에 따라 추가 비용이 발생합니다.
            </Text>
          </div>
          <Switch checked={enabled} onChange={handleToggle} loading={loading} />
        </div>
      </Card>
    </div>
  );
}

function PollingIntervalSettings() {
  const [interval, setInterval] = useState(2);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    apiClient.get('/settings/polling-interval').then(res => {
      setInterval(Number(res.data.intervalMinutes));
    }).catch(() => {});
  }, []);

  const handleSave = async () => {
    if (interval < 1 || interval > 10) {
      void message.warning('폴링 주기는 1~10분 사이여야 합니다.');
      return;
    }
    setLoading(true);
    try {
      await apiClient.put('/settings/polling-interval', { intervalMinutes: interval });
      void message.success('폴링 주기가 변경되었습니다.');
    } catch {
      void message.error('폴링 주기 변경 실패');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={4} style={{ margin: 0, marginBottom: 4 }}>발송 결과 폴링 주기</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        DynamoDB에서 발송 결과를 조회하는 주기를 설정합니다.
      </Text>
      <Card>
        <Space>
          <InputNumber
            min={1}
            max={10}
            value={interval}
            onChange={(v) => v !== null && setInterval(v)}
            addonAfter="분"
            style={{ width: 120 }}
          />
          <Button type="primary" onClick={handleSave} loading={loading}>저장</Button>
        </Space>
        <br />
        <Text type="secondary" style={{ fontSize: 13, marginTop: 8, display: 'block' }}>
          1분 미만은 DynamoDB 비용 과다, 10분 초과는 결과 반영 지연 우려
        </Text>
      </Card>
    </div>
  );
}

export default function SettingsPage() {
  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 4 }}>설정</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        서비스 환경을 설정합니다.
      </Text>

      <PasswordChangeSection />

      <Divider />

      <VdmSettings />

      <Divider />

      <PollingIntervalSettings />

      <Divider />

      <AwsSettingsPage />

      <Divider />

      <ThemeSettingsPage />
    </div>
  );
}
