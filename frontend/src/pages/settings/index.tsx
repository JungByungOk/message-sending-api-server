import { useState } from 'react';
import { Badge, Button, Card, Divider, Input, Space, Typography, message } from 'antd';
import {
  ApiOutlined,
  DeleteOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/stores/auth';
import AwsSettingsPage from './AwsSettings';
import ThemeSettingsPage from './ThemeSettings';

const { Title, Text } = Typography;

function ApiKeySettings() {
  const { apiKey, setApiKey, clearApiKey } = useAuthStore();
  const [inputKey, setInputKey] = useState(apiKey);
  const [editing, setEditing] = useState(false);

  const handleSave = () => {
    const trimmed = inputKey.trim();
    if (!trimmed) {
      void message.warning('API Key를 입력해 주세요.');
      return;
    }
    setApiKey(trimmed);
    setEditing(false);
    void message.success('API Key가 저장되었습니다.');
  };

  const handleClear = () => {
    clearApiKey();
    setInputKey('');
    setEditing(false);
    void message.info('API Key가 초기화되었습니다.');
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
        <Title level={4} style={{ margin: 0 }}>API Key 설정</Title>
        <Badge status={apiKey ? 'success' : 'error'} text={apiKey ? '설정됨' : '미설정'} />
      </div>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        백엔드 서버 인증에 사용되는 키를 관리합니다.
      </Text>

      <Card>
        <Space.Compact style={{ width: '100%' }}>
          <Input.Password
            placeholder="API Key를 입력하세요"
            value={editing ? inputKey : (apiKey ?? '')}
            readOnly={!editing && !!apiKey}
            onChange={(e) => setInputKey(e.target.value)}
            onPressEnter={editing ? handleSave : undefined}
            prefix={<ApiOutlined style={{ color: '#9ca3af' }} />}
          />
          {editing ? (
            <>
              <Button type="primary" icon={<SaveOutlined />} onClick={handleSave}>저장</Button>
              <Button onClick={() => setEditing(false)}>취소</Button>
            </>
          ) : (
            <>
              <Button onClick={() => { setInputKey(apiKey ?? ''); setEditing(true); }}>변경</Button>
              <Button danger icon={<DeleteOutlined />} onClick={handleClear}>초기화</Button>
            </>
          )}
        </Space.Compact>
        <Text type="secondary" style={{ fontSize: 13, marginTop: 8, display: 'block' }}>
          API Key는 브라우저 로컬 스토리지에 저장되며, 모든 API 요청의 Authorization 헤더에 포함됩니다.
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

      <ApiKeySettings />

      <Divider />

      <AwsSettingsPage />

      <Divider />

      <ThemeSettingsPage />
    </div>
  );
}
