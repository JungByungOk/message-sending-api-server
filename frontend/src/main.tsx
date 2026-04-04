import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ConfigProvider, App as AntApp } from 'antd';
import koKR from 'antd/locale/ko_KR';
import 'dayjs/locale/ko';
import App from './App';
import './index.css';
import { useThemeStore } from './stores/theme';

function Root() {
  const theme = useThemeStore((s) => s.current);

  // CSS custom properties for non-AntD styles
  if (typeof document !== 'undefined') {
    const root = document.documentElement;
    root.style.setProperty('--ems-logo-bg', theme.authCss.logoBg);
    root.style.setProperty('--ems-layout-bg', theme.authCss.layoutBg);
  }

  return (
    <ConfigProvider theme={theme.antd} locale={koKR}>
      <AntApp>
        <App />
      </AntApp>
    </ConfigProvider>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
);
