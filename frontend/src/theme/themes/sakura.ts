import type { AppTheme } from '../types';
import { sharedComponents, sharedFontFamily } from '../shared';

const sakura: AppTheme = {
  key: 'sakura',
  label: 'Sakura',
  description:
    '벚꽃의 매혹적인 아름다움에서 영감을 받은 테마입니다. 부드러운 봄의 차분한 감성을 담아, 섬세한 핑크가 배경을 감싸고 차분한 그린과 블루가 고요한 정원과 맑은 하늘을 표현합니다. 눈의 피로를 줄이며 집중력을 높여주는 편안한 색감입니다.',
  palette: ['#FE0CFC', '#F80BE6', '#FF09DC', '#CB8891', '#9B556C', '#DBEACB', '#61BC71', '#69A2BD', '#607FA9', '#68978B'],
  antd: {
    token: {
      colorPrimary: '#9B556C',
      colorBgLayout: '#FDF6F8',
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
        headerBg: '#FDF6F8',
        headerColor: '#6B3A4A',
        headerSplitColor: 'transparent',
        rowHoverBg: '#F9EDF0',
      },
      Layout: { headerBg: '#ffffff', siderBg: '#ffffff' },
    },
  },
  proLayout: {
    header: {
      colorBgHeader: '#ffffff',
      colorHeaderTitle: '#4A2333',
      colorTextMenu: '#6B3A4A',
      colorTextMenuSelected: '#9B556C',
      colorBgMenuItemSelected: '#F5E2E8',
      colorTextMenuActive: '#9B556C',
    },
    sider: {
      colorBgMenuItemSelected: '#F5E2E8',
      colorTextMenuSelected: '#9B556C',
      colorTextMenuActive: '#9B556C',
      colorTextMenu: '#6B3A4A',
      colorMenuBackground: '#ffffff',
      colorBgMenuItemHover: '#F9EDF0',
    },
  },
  authCss: {
    logoBg: '#9B556C',
    layoutBg: '#FDF6F8',
  },
};

export default sakura;
