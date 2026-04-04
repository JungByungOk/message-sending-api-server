import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ProLayout } from '@ant-design/pro-components';
import { Avatar, Badge, Button, Tooltip, Typography } from 'antd';
import {
  ClockCircleOutlined,
  DashboardOutlined,
  FileTextOutlined,
  KeyOutlined,
  MailOutlined,
  SettingOutlined,
  StopOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/stores/auth';
import { useThemeStore } from '@/stores/theme';

const { Text } = Typography;

// ─── 메뉴 구성 (그룹별) ────────────────────────────────────────────────────────
const menuItems = [
  {
    path: '/',
    name: '대시보드',
    icon: <DashboardOutlined />,
  },
  {
    name: '서비스 관리',
    icon: <MailOutlined />,
    children: [
      {
        path: '/email/send',
        name: '이메일 발송',
        icon: <MailOutlined />,
      },
      {
        path: '/template',
        name: '템플릿 관리',
        icon: <FileTextOutlined />,
      },
      {
        path: '/scheduler',
        name: '스케줄러',
        icon: <ClockCircleOutlined />,
      },
    ],
  },
  {
    name: '테넌트 관리',
    icon: <TeamOutlined />,
    children: [
      {
        path: '/tenant',
        name: '테넌트 목록',
        icon: <TeamOutlined />,
      },
      {
        path: '/onboarding',
        name: '온보딩',
        icon: <UserAddOutlined />,
      },
      {
        path: '/suppression',
        name: '수신 거부',
        icon: <StopOutlined />,
      },
    ],
  },
  {
    path: '/settings',
    name: '설정',
    icon: <SettingOutlined />,
  },
];

// ─── API Key 상태 표시기 ────────────────────────────────────────────────────────
function ApiKeyIndicator({ hasKey, onClick }: { hasKey: boolean; onClick: () => void }) {
  return (
    <Tooltip title={hasKey ? 'API Key 설정됨' : 'API Key 미설정 — 설정 페이지에서 입력'}>
      <Button
        type="text"
        size="small"
        onClick={onClick}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          height: 32,
          padding: '0 10px',
          borderRadius: 8,
          color: hasKey ? '#12b76a' : '#f04438',
          background: hasKey ? 'rgba(18,183,106,0.08)' : 'rgba(240,68,56,0.08)',
          border: `1px solid ${hasKey ? 'rgba(18,183,106,0.2)' : 'rgba(240,68,56,0.2)'}`,
          fontWeight: 500,
          fontSize: 13,
        }}
      >
        <Badge
          status={hasKey ? 'success' : 'error'}
          style={{ marginRight: 0 }}
        />
        <KeyOutlined />
        <span>API Key</span>
      </Button>
    </Tooltip>
  );
}

export default function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { apiKey } = useAuthStore();
  const { proLayout, antd: antdTheme } = useThemeStore((s) => s.current);
  const primaryColor = (antdTheme.token as Record<string, unknown>)?.colorPrimary as string ?? '#0065FF';

  const [collapsed, setCollapsed] = useState(false);

  return (
    <>
      <ProLayout
        title="Joins EMS"
        logo={
          <div
            style={{
              width: 28,
              height: 28,
              borderRadius: 8,
              background: primaryColor,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <MailOutlined style={{ color: '#fff', fontSize: 14 }} />
          </div>
        }
        layout="mix"
        fixSiderbar
        siderWidth={280}
        collapsed={collapsed}
        onCollapse={setCollapsed}
        location={{ pathname: location.pathname }}
        menuItemRender={(item, dom) => (
          <span
            onClick={() => item.path && navigate(item.path)}
            style={{ cursor: 'pointer', display: 'block', width: '100%' }}
          >
            {dom}
          </span>
        )}
        menuDataRender={() => menuItems}
        token={{
          header: proLayout.header,
          sider: proLayout.sider,
        }}
        actionsRender={() => [
          <ApiKeyIndicator key="api-key" hasKey={!!apiKey} onClick={() => navigate('/settings')} />,
          <Avatar
            key="user"
            size={32}
            icon={<UserOutlined />}
            style={{
              cursor: 'pointer',
              background: primaryColor,
              flexShrink: 0,
            }}
            onClick={() => navigate('/settings')}
          />,
        ]}
        footerRender={() => (
          <div
            style={{
              textAlign: 'center',
              padding: '12px 0',
              borderTop: '1px solid rgba(0, 0, 0, 0.06)',
            }}
          >
            <Text style={{ fontSize: 12, color: 'rgba(0,0,0,0.35)' }}>
              © {new Date().getFullYear()} Joins EMS. All rights reserved.
            </Text>
          </div>
        )}
        style={{ minHeight: '100vh' }}
      >
        <Outlet />
      </ProLayout>
    </>
  );
}
