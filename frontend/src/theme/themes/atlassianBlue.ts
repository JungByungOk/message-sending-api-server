import type { AppTheme } from '../types';
import { sharedComponents, sharedFontFamily } from '../shared';

const atlassianBlue: AppTheme = {
  key: 'atlassian-blue',
  label: 'Atlassian Blue',
  description:
    'Atlassian의 디자인 시스템에서 영감을 받은 테마입니다. 밝은 화이트 배경 위에 선명한 블루 액센트로 집중력을 높이고, 부드러운 그레이 톤으로 시각적 피로를 줄여줍니다. 업무 생산성 도구에 최적화된 깔끔한 느낌을 제공합니다.',
  palette: ['#0065FF', '#E9F2FF', '#172B4D', '#42526E', '#FAFBFC', '#F4F5F7', '#ffffff'],
  antd: {
    token: {
      colorPrimary: '#0065FF',
      colorBgLayout: '#FAFBFC',
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
        headerBg: '#FAFBFC',
        headerColor: '#42526E',
        headerSplitColor: 'transparent',
        rowHoverBg: '#F4F5F7',
      },
      Layout: { headerBg: '#ffffff', siderBg: '#ffffff' },
    },
  },
  proLayout: {
    header: {
      colorBgHeader: '#ffffff',
      colorHeaderTitle: '#172B4D',
      colorTextMenu: '#42526E',
      colorTextMenuSelected: '#0065FF',
      colorBgMenuItemSelected: '#E9F2FF',
      colorTextMenuActive: '#0065FF',
    },
    sider: {
      colorBgMenuItemSelected: '#E9F2FF',
      colorTextMenuSelected: '#0065FF',
      colorTextMenuActive: '#0065FF',
      colorTextMenu: '#42526E',
      colorMenuBackground: '#ffffff',
      colorBgMenuItemHover: '#F4F5F7',
    },
  },
  authCss: {
    logoBg: '#0065FF',
    layoutBg: '#FAFBFC',
  },
};

export default atlassianBlue;
