import type { ThemeConfig } from 'antd';

export interface ProLayoutToken {
  header: Record<string, string>;
  sider: Record<string, string>;
}

export interface AppTheme {
  key: string;
  label: string;
  description: string;
  palette: string[];
  antd: ThemeConfig;
  proLayout: ProLayoutToken;
  authCss: {
    logoBg: string;
    layoutBg: string;
  };
}
