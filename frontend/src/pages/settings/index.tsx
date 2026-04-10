import { useState } from 'react';
import { Button, Card, Divider, Form, Input, Typography, message } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { authApi } from '@/api/auth';
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

export default function SettingsPage() {
  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 4 }}>설정</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        서비스 환경을 설정합니다.
      </Text>

      <PasswordChangeSection />

      <Divider />

      <AwsSettingsPage />

      <Divider />

      <ThemeSettingsPage />
    </div>
  );
}
