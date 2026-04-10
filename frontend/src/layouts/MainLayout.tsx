import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ProLayout } from '@ant-design/pro-components';
import { Avatar, Typography } from 'antd';
import {
  BarChartOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DollarOutlined,
  FileTextOutlined,
  FundOutlined,
  MailOutlined,
  SettingOutlined,
  StopOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';

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
        name: '테스트 이메일 발송',
        icon: <MailOutlined />,
      },
      {
        path: '/email/results',
        name: '발송 결과 조회',
        icon: <BarChartOutlined />,
      },
      {
        path: '/template',
        name: '템플릿 관리',
        icon: <FileTextOutlined />,
      },
      {
        path: '/scheduler',
        name: '스케줄러 등록 현황',
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
        path: '/suppression',
        name: '수신 거부',
        icon: <StopOutlined />,
      },
    ],
  },
  {
    path: '/monitoring',
    name: '모니터링',
    icon: <FundOutlined />,
  },
  {
    path: '/cost',
    name: 'AWS 비용',
    icon: <DollarOutlined />,
  },
  {
    path: '/settings',
    name: '설정',
    icon: <SettingOutlined />,
  },
];


export default function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
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
