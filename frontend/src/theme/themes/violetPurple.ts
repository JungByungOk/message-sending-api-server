import type { AppTheme } from '../types';
import { sharedComponents, sharedFontFamily } from '../shared';

const violetPurple: AppTheme = {
  key: 'violet-purple',
  label: 'Violet Purple',
  description:
    '우아한 바이올렛 퍼플 테마입니다. 보라색 계열의 부드러운 색감이 창의적이면서도 세련된 분위기를 연출합니다. 일상적인 업무 화면에 개성과 감각을 더하고 싶을 때 추천하는 테마입니다.',
  palette: ['#6554C0', '#EDE7F6', '#2C2447', '#4A4565', '#FAFAFE', '#F3F0FF', '#ffffff'],
  antd: {
    token: {
      colorPrimary: '#6554C0',
      colorBgLayout: '#FAFAFE',
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
        headerBg: '#FAFAFE',
        headerColor: '#4A4565',
        headerSplitColor: 'transparent',
        rowHoverBg: '#F3F0FF',
      },
      Layout: { headerBg: '#ffffff', siderBg: '#ffffff' },
    },
  },
  proLayout: {
    header: {
      colorBgHeader: '#ffffff',
      colorHeaderTitle: '#2C2447',
      colorTextMenu: '#4A4565',
      colorTextMenuSelected: '#6554C0',
      colorBgMenuItemSelected: '#EDE7F6',
      colorTextMenuActive: '#6554C0',
    },
    sider: {
      colorBgMenuItemSelected: '#EDE7F6',
      colorTextMenuSelected: '#6554C0',
      colorTextMenuActive: '#6554C0',
      colorTextMenu: '#4A4565',
      colorMenuBackground: '#ffffff',
      colorBgMenuItemHover: '#F5F3FF',
    },
  },
  authCss: {
    logoBg: '#6554C0',
    layoutBg: '#FAFAFE',
  },
};

export default violetPurple;
