import type { AppTheme } from '../types';
import { sharedComponents, sharedFontFamily } from '../shared';

const rioDeJaneiro: AppTheme = {
  key: 'rio-de-janeiro',
  label: 'Rio de Janeiro',
  description:
    '리우의 푸른 언덕과 부드러운 아침 햇살, 바다 공기에서 영감을 받은 테마입니다. 산뜻한 민트 배경 위에 열대우림 그린과 해안 블루가 조화를 이루며, 장시간 작업에도 밝고 차분한 화면을 유지합니다. 선명한 그린 액센트가 가독성을 높여줍니다.',
  palette: ['#1056C0', '#0A80B3', '#247AAC', '#94B98B', '#DAE0A6', '#7A6D14', '#93B920', '#E6C41A'],
  antd: {
    token: {
      colorPrimary: '#247AAC',
      colorBgLayout: '#F7FAF6',
      borderRadius: 6,
      fontFamily: sharedFontFamily,
      fontSize: 15,
      colorBgContainer: '#ffffff',
      boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
      boxShadowSecondary: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
    },
    components: {
      ...sharedComponents,
      Table: {
        headerBg: '#F7FAF6',
        headerColor: '#3A5A3E',
        headerSplitColor: 'transparent',
        rowHoverBg: '#EEF4EC',
      },
      Layout: { headerBg: '#ffffff', siderBg: '#ffffff' },
    },
  },
  proLayout: {
    header: {
      colorBgHeader: '#ffffff',
      colorHeaderTitle: '#1E3A22',
      colorTextMenu: '#3A5A3E',
      colorTextMenuSelected: '#247AAC',
      colorBgMenuItemSelected: '#E0EEF6',
      colorTextMenuActive: '#247AAC',
    },
    sider: {
      colorBgMenuItemSelected: '#E0EEF6',
      colorTextMenuSelected: '#247AAC',
      colorTextMenuActive: '#247AAC',
      colorTextMenu: '#3A5A3E',
      colorMenuBackground: '#ffffff',
      colorBgMenuItemHover: '#EEF4EC',
    },
  },
  authCss: {
    logoBg: '#247AAC',
    layoutBg: '#F7FAF6',
  },
};

export default rioDeJaneiro;
