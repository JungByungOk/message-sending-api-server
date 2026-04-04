import type { AppTheme } from '../types';
import { sharedComponents, sharedFontFamily } from '../shared';

const sydneyCoastal: AppTheme = {
  key: 'sydney-coastal',
  label: 'Sydney Coastal',
  description:
    '시드니의 화창한 항구 아침에서 영감을 받은 테마입니다. 포슬린 화이트와 안개 낀 블루가 깨끗하고 경쾌한 화면을 만들어주며, 깊은 틸 액센트와 따뜻한 샌드스톤 하이라이트가 탭, 테두리, 검색 결과에 적절한 대비를 제공합니다. 차분하면서도 햇살 가득한 느낌을 유지합니다.',
  palette: ['#169DC5', '#CAE4EC', '#F6F9FA', '#73B9D0', '#2EB9A6', '#D17F51', '#BA7347'],
  antd: {
    token: {
      colorPrimary: '#169DC5',
      colorBgLayout: '#F6F9FA',
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
        headerBg: '#F6F9FA',
        headerColor: '#2A5A6E',
        headerSplitColor: 'transparent',
        rowHoverBg: '#EAF4F8',
      },
      Layout: { headerBg: '#ffffff', siderBg: '#ffffff' },
    },
  },
  proLayout: {
    header: {
      colorBgHeader: '#ffffff',
      colorHeaderTitle: '#1B4D5E',
      colorTextMenu: '#2A5A6E',
      colorTextMenuSelected: '#169DC5',
      colorBgMenuItemSelected: '#D9EEF5',
      colorTextMenuActive: '#169DC5',
    },
    sider: {
      colorBgMenuItemSelected: '#D9EEF5',
      colorTextMenuSelected: '#169DC5',
      colorTextMenuActive: '#169DC5',
      colorTextMenu: '#2A5A6E',
      colorMenuBackground: '#ffffff',
      colorBgMenuItemHover: '#EAF4F8',
    },
  },
  authCss: {
    logoBg: '#169DC5',
    layoutBg: '#F6F9FA',
  },
};

export default sydneyCoastal;
