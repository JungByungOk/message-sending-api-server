import { CheckCircleFilled } from '@ant-design/icons';
import { Card, Col, Row, Tooltip, Typography } from 'antd';
import { themes } from '@/theme/themeRegistry';
import type { AppTheme } from '@/theme/types';
import { useThemeStore } from '@/stores/theme';
import atlassianBlueIcon from '@/assets/themes/atlassian-blue.png';
import tealGreenIcon from '@/assets/themes/teal-green.jpg';
import violetPurpleIcon from '@/assets/themes/violet-purple.jpg';
import sydneyCoastalIcon from '@/assets/themes/sydney-coastal.png';
import sydneyCoastalBgImage from '@/assets/themes/sydney-coastal-bg.jpg';
import rioDeJaneiroIcon from '@/assets/themes/rio-de-janeiro.jpg';
import rioDeJaneiroBgImage from '@/assets/themes/rio-de-janeiro-bg.jpg';
import sakuraIcon from '@/assets/themes/sakura.png';
import sakuraBgImage from '@/assets/themes/sakura.jpg';

const THEME_BG_IMAGES: Record<string, string> = {
  'violet-purple': violetPurpleIcon,
  'sydney-coastal': sydneyCoastalBgImage,
  'rio-de-janeiro': rioDeJaneiroBgImage,
  sakura: sakuraBgImage,
};

const THEME_ICONS: Record<string, React.ReactNode> = {
  'atlassian-blue': <img src={atlassianBlueIcon} alt="Atlassian Blue" width={64} height={64} style={{ borderRadius: '50%' }} />,
  'teal-green': <img src={tealGreenIcon} alt="Teal Green" width={64} height={64} style={{ borderRadius: '50%', objectFit: 'cover' }} />,
  'violet-purple': <img src={violetPurpleIcon} alt="Violet Purple" width={64} height={64} style={{ borderRadius: '50%', objectFit: 'cover' }} />,
  'sydney-coastal': <img src={sydneyCoastalIcon} alt="Sydney Coastal" width={64} height={64} style={{ borderRadius: '50%' }} />,
  'rio-de-janeiro': <img src={rioDeJaneiroIcon} alt="Rio de Janeiro" width={64} height={64} style={{ borderRadius: '50%', objectFit: 'cover' }} />,
  sakura: <img src={sakuraIcon} alt="Sakura" width={64} height={64} style={{ borderRadius: '50%', objectFit: 'cover' }} />,
};

function ThemeCard({
  theme,
  selected,
  onSelect,
}: {
  theme: AppTheme;
  selected: boolean;
  onSelect: () => void;
}) {
  const primary = (theme.antd.token as Record<string, unknown>)?.colorPrimary as string;
  const bgLayout = (theme.antd.token as Record<string, unknown>)?.colorBgLayout as string;
  const siderSelected = theme.proLayout.sider.colorBgMenuItemSelected;

  return (
    <Card
      hoverable
      onClick={onSelect}
      style={{
        borderColor: selected ? primary : undefined,
        borderWidth: selected ? 2 : 1,
        cursor: 'pointer',
        position: 'relative',
        overflow: 'hidden',
      }}
      styles={{ body: { padding: 0 } }}
    >
      {/* Mini preview */}
      <div style={{ display: 'flex', height: 120 }}>
        {/* Sider preview */}
        <div
          style={{
            width: 48,
            backgroundColor: '#ffffff',
            borderRight: '1px solid #f0f0f0',
            padding: '8px 6px',
            display: 'flex',
            flexDirection: 'column',
            gap: 4,
          }}
        >
          <div style={{ height: 6, borderRadius: 3, backgroundColor: siderSelected }} />
          <div style={{ height: 6, borderRadius: 3, backgroundColor: '#f0f0f0' }} />
          <div style={{ height: 6, borderRadius: 3, backgroundColor: '#f0f0f0' }} />
        </div>
        {/* Content preview */}
        <div
          style={{
            flex: 1,
            backgroundColor: bgLayout,
            padding: 10,
            display: 'flex',
            flexDirection: 'column',
            gap: 6,
          }}
        >
          <div style={{ height: 8, width: '60%', borderRadius: 4, backgroundColor: primary }} />
          <div style={{ display: 'flex', gap: 6, flex: 1 }}>
            <div style={{ flex: 1, borderRadius: 4, backgroundColor: '#ffffff', border: '1px solid #f0f0f0' }} />
            <div style={{ flex: 1, borderRadius: 4, backgroundColor: '#ffffff', border: '1px solid #f0f0f0' }} />
          </div>
        </div>
      </div>

      {/* Label */}
      <div
        style={{
          padding: '10px 14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: '1px solid #f0f0f0',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 16, height: 16, borderRadius: '50%', backgroundColor: primary }} />
          <Typography.Text strong={selected}>{theme.label}</Typography.Text>
        </div>
        {selected && <CheckCircleFilled style={{ color: primary, fontSize: 18 }} />}
      </div>
    </Card>
  );
}

function ColorRow({ label, color }: { label: string; color: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <div
        style={{
          width: 20,
          height: 20,
          borderRadius: 4,
          backgroundColor: color,
          border: color === '#ffffff' ? '1px solid #f0f0f0' : undefined,
          flexShrink: 0,
        }}
      />
      <Typography.Text style={{ fontSize: 13, flex: 1 }}>{label}</Typography.Text>
      <Typography.Text code style={{ fontSize: 11 }}>{color}</Typography.Text>
    </div>
  );
}

function ThemeDetailPanel({ theme }: { theme: AppTheme }) {
  const primary = (theme.antd.token as Record<string, unknown>)?.colorPrimary as string;
  const bgLayout = (theme.antd.token as Record<string, unknown>)?.colorBgLayout as string;
  const siderSelected = theme.proLayout.sider.colorBgMenuItemSelected;
  const headerTitle = theme.proLayout.header.colorHeaderTitle;
  const textMenu = theme.proLayout.sider.colorTextMenu;

  return (
    <Card
      style={{
        borderTop: `3px solid ${primary}`,
        position: 'sticky',
        top: 16,
        overflow: 'hidden',
      }}
      styles={{ body: { padding: 0 } }}
    >
      {/* Banner */}
      <div style={{ height: 140, position: 'relative', overflow: 'hidden' }}>
        {THEME_BG_IMAGES[theme.key] ? (
          <img
            src={THEME_BG_IMAGES[theme.key]}
            alt=""
            style={{
              position: 'absolute',
              inset: -20,
              width: 'calc(100% + 40px)',
              height: 'calc(100% + 40px)',
              objectFit: 'cover',
              filter: 'blur(3px)',
            }}
          />
        ) : (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              background: `
                radial-gradient(circle at 20% 30%, ${primary}50 0%, transparent 50%),
                radial-gradient(circle at 80% 60%, ${siderSelected} 0%, transparent 40%),
                radial-gradient(circle at 50% 80%, ${primary}30 0%, transparent 50%),
                linear-gradient(135deg, ${primary}18, ${bgLayout})
              `,
              filter: 'blur(20px)',
              transform: 'scale(1.2)',
            }}
          />
        )}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            backgroundColor: THEME_BG_IMAGES[theme.key] ? 'rgba(0,0,0,0.1)' : 'rgba(255,255,255,0.15)',
          }}
        />
      </div>

      {/* Icon */}
      <div style={{ position: 'relative', display: 'flex', justifyContent: 'center', marginTop: -32, zIndex: 1 }}>
        {THEME_ICONS[theme.key] ? (
          <div
            style={{
              borderRadius: '50%',
              boxShadow: `0 4px 12px rgba(0,0,0,0.15), 0 8px 24px ${primary}40`,
              lineHeight: 0,
            }}
          >
            {THEME_ICONS[theme.key]}
          </div>
        ) : (
          <div
            style={{
              width: 72,
              height: 72,
              borderRadius: '50%',
              backgroundColor: primary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: `0 4px 12px rgba(0,0,0,0.15), 0 8px 24px ${primary}40`,
              border: '3px solid rgba(255,255,255,0.4)',
            }}
          >
            <CheckCircleFilled style={{ color: '#ffffff', fontSize: 28 }} />
          </div>
        )}
      </div>

      {/* Theme name */}
      <div style={{ textAlign: 'center', padding: '8px 20px 12px' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{theme.label}</Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 13 }}>현재 적용 중</Typography.Text>
      </div>

      <div style={{ height: 1, backgroundColor: '#f0f0f0', margin: '0 20px' }} />

      {/* Colors */}
      <div style={{ padding: '16px 20px' }}>
        <Typography.Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 10 }}>
          색상 구성
        </Typography.Text>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <ColorRow label="Primary" color={primary} />
          <ColorRow label="Selected BG" color={siderSelected} />
          <ColorRow label="Layout BG" color={bgLayout} />
          <ColorRow label="Title" color={headerTitle} />
          <ColorRow label="Text" color={textMenu} />
        </div>
      </div>

      <div style={{ height: 1, backgroundColor: '#f0f0f0', margin: '0 20px' }} />

      {/* Description */}
      <div style={{ padding: '16px 20px' }}>
        <Typography.Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>
          설명
        </Typography.Text>
        <Typography.Paragraph
          style={{ margin: 0, fontSize: 14, lineHeight: 1.7, color: 'rgba(0,0,0,0.65)' }}
        >
          {theme.description}
        </Typography.Paragraph>
      </div>

      <div style={{ height: 1, backgroundColor: '#f0f0f0', margin: '0 20px' }} />

      {/* Palette */}
      <div style={{ padding: '16px 20px 20px' }}>
        <Typography.Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 10 }}>
          Color Palette
        </Typography.Text>
        <div style={{ display: 'flex', gap: 0, borderRadius: 8, overflow: 'hidden' }}>
          {theme.palette.map((color) => (
            <Tooltip key={color} title={color}>
              <div
                style={{
                  flex: 1,
                  height: 36,
                  backgroundColor: color,
                  cursor: 'pointer',
                  border: color === '#ffffff' ? '1px solid #f0f0f0' : undefined,
                  transition: 'transform 0.2s',
                }}
              />
            </Tooltip>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 0, marginTop: 4 }}>
          {theme.palette.map((color) => (
            <Typography.Text
              key={color}
              type="secondary"
              style={{ flex: 1, fontSize: 9, textAlign: 'center' }}
            >
              {color}
            </Typography.Text>
          ))}
        </div>
      </div>
    </Card>
  );
}

export default function ThemeSettingsPage() {
  const themeKey = useThemeStore((s) => s.themeKey);
  const setTheme = useThemeStore((s) => s.setTheme);
  const currentTheme = useThemeStore((s) => s.current);

  return (
    <div style={{ maxWidth: 960 }}>
      <Typography.Title level={4} style={{ marginBottom: 4 }}>
        테마 설정
      </Typography.Title>
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        원하는 테마를 선택하면 즉시 적용됩니다.
      </Typography.Text>
      <Row gutter={24}>
        <Col xs={24} lg={14}>
          <Row gutter={[16, 16]}>
            {themes.map((t) => (
              <Col key={t.key} xs={24} sm={12}>
                <ThemeCard
                  theme={t}
                  selected={themeKey === t.key}
                  onSelect={() => setTheme(t.key)}
                />
              </Col>
            ))}
          </Row>
        </Col>
        <Col xs={24} lg={10}>
          <ThemeDetailPanel theme={currentTheme} />
        </Col>
      </Row>
    </div>
  );
}
