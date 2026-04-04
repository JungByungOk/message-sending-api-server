import type { AppTheme } from '../types';
import { sharedComponents, sharedFontFamily } from '../shared';

const tealGreen: AppTheme = {
  key: 'teal-green',
  label: 'Teal Green',
  description:
    '자연에서 영감을 받은 차분한 틸 그린 테마입니다. 청록색 액센트와 부드러운 민트 배경이 편안한 작업 환경을 만들어줍니다. 장시간 화면을 보는 업무에서도 눈의 피로를 줄이고, 안정감 있는 인터페이스를 경험할 수 있습니다.',
  palette: ['#00B8A9', '#E0F7F5', '#1A3C38', '#3D5A56', '#F6FFFE', '#E6FAF8', '#ffffff'],
  antd: {
    token: {
      colorPrimary: '#00B8A9',
      colorBgLayout: '#F6FFFE',
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
        headerBg: '#F6FFFE',
        headerColor: '#3D5A56',
        headerSplitColor: 'transparent',
        rowHoverBg: '#E6FAF8',
      },
      Layout: { headerBg: '#ffffff', siderBg: '#ffffff' },
    },
  },
  proLayout: {
    header: {
      colorBgHeader: '#ffffff',
      colorHeaderTitle: '#1A3C38',
      colorTextMenu: '#3D5A56',
      colorTextMenuSelected: '#00B8A9',
      colorBgMenuItemSelected: '#E0F7F5',
      colorTextMenuActive: '#00B8A9',
    },
    sider: {
      colorBgMenuItemSelected: '#E0F7F5',
      colorTextMenuSelected: '#00B8A9',
      colorTextMenuActive: '#00B8A9',
      colorTextMenu: '#3D5A56',
      colorMenuBackground: '#ffffff',
      colorBgMenuItemHover: '#F0FAF9',
    },
  },
  authCss: {
    logoBg: '#00B8A9',
    layoutBg: '#F6FFFE',
  },
};

export default tealGreen;
