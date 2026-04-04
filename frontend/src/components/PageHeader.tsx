import { Breadcrumb, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Title, Text } = Typography;

interface BreadcrumbItem {
  title: string;
  href?: string;
}

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumbs?: BreadcrumbItem[];
  actions?: ReactNode;
  style?: React.CSSProperties;
}

export default function PageHeader({
  title,
  subtitle,
  breadcrumbs,
  actions,
  style,
}: PageHeaderProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        marginBottom: 24,
        ...style,
      }}
    >
      <div>
        {breadcrumbs && breadcrumbs.length > 0 && (
          <Breadcrumb
            style={{ marginBottom: 8 }}
            items={breadcrumbs.map((b) => ({
              title: b.href ? <a href={b.href}>{b.title}</a> : b.title,
            }))}
          />
        )}
        <Title
          level={3}
          style={{
            margin: 0,
            fontWeight: 700,
            letterSpacing: '-0.3px',
            color: '#111827',
          }}
        >
          {title}
        </Title>
        {subtitle && (
          <Text type="secondary" style={{ marginTop: 4, display: 'block', fontSize: 14 }}>
            {subtitle}
          </Text>
        )}
      </div>
      {actions && (
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0, marginTop: 4 }}>
          {actions}
        </div>
      )}
    </div>
  );
}
