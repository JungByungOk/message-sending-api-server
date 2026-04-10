import { useState } from 'react';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';
import type { LoginRequest } from '@/types/auth';

const { Title, Text } = Typography;

export default function LoginPage() {
  const navigate = useNavigate();
  const { setTokens, setUser } = useAuthStore();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const { data } = await authApi.login(values);
      setTokens(data.accessToken, data.refreshToken);
      setUser(data.user);
      void message.success(`${data.user.displayName}님, 환영합니다.`);
      navigate('/', { replace: true });
    } catch (error: any) {
      const msg = error.response?.data?.message ?? '로그인에 실패했습니다.';
      void message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card
        style={{
          width: 400,
          borderRadius: 12,
          boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: 16,
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 16,
            }}
          >
            <MailOutlined style={{ color: '#fff', fontSize: 28 }} />
          </div>
          <Title level={3} style={{ margin: 0 }}>Joins EMS</Title>
          <Text type="secondary">이메일 관리 시스템에 로그인하세요</Text>
        </div>

        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
          layout="vertical"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '사용자 이름을 입력하세요' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="사용자 이름" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '비밀번호를 입력하세요' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="비밀번호" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading} block>
              로그인
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
