import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  Empty,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Typography,
  message,
} from 'antd';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import {
  DeleteOutlined,
  ExclamationCircleFilled,
  MailOutlined,
  SearchOutlined,
  StopOutlined,
  WarningFilled,
} from '@ant-design/icons';
import { useTenants } from '@/hooks/useTenants';
import { useSuppressions, useRemoveSuppression } from '@/hooks/useSuppression';
import type { SuppressionEntry } from '@/types/suppression';
import PageHeader from '@/components/PageHeader';
import StatusTag from '@/components/StatusTag';

const { Text } = Typography;

export default function SuppressionList() {
  const [selectedTenantId, setSelectedTenantId] = useState<string>('');
  const [page, setPage] = useState(0);

  const { data: tenantsData } = useTenants();
  const { data: suppressionData, isLoading } = useSuppressions(selectedTenantId, {
    page,
    size: 20,
  });
  const removeSuppression = useRemoveSuppression();

  const handleDelete = (email: string) => {
    if (!selectedTenantId) return;
    removeSuppression.mutate(
      { tenantId: selectedTenantId, email },
      {
        onSuccess: () => {
          void message.success('수신 거부가 삭제되었습니다.');
        },
        onError: () => {
          void message.error('삭제에 실패했습니다.');
        },
      },
    );
  };

  const totalCount = suppressionData?.totalCount ?? 0;
  const bounceCount =
    suppressionData?.suppressions.filter((s) => s.reason === 'BOUNCE').length ?? 0;
  const complaintCount =
    suppressionData?.suppressions.filter((s) => s.reason === 'COMPLAINT').length ?? 0;

  const columns: ProColumns<SuppressionEntry>[] = [
    {
      title: '이메일',
      dataIndex: 'email',
      key: 'email',
      ellipsis: true,
      render: (val) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MailOutlined style={{ color: '#9ca3af', fontSize: 13 }} />
          <Text
            copyable={{ text: val as string }}
            style={{ fontFamily: 'monospace', fontSize: 12, color: '#374151' }}
          >
            {val as string}
          </Text>
        </div>
      ),
    },
    {
      title: '사유',
      dataIndex: 'reason',
      key: 'reason',
      width: 130,
      render: (_, record) => <StatusTag type="suppression" status={record.reason} />,
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      valueType: 'dateTime',
      render: (_, record) => (
        <Text style={{ fontSize: 13, color: '#6b7280' }}>
          {new Date(record.createdAt).toLocaleString('ko-KR')}
        </Text>
      ),
    },
    {
      title: '작업',
      key: 'action',
      width: 80,
      render: (_, record) => (
        <Popconfirm
          title="수신 거부 삭제"
          description="수신 거부 목록에서 삭제하시겠습니까?"
          okText="삭제"
          cancelText="취소"
          okButtonProps={{ danger: true }}
          onConfirm={() => handleDelete(record.email)}
        >
          <Button
            type="text"
            size="small"
            icon={<DeleteOutlined />}
            danger
            style={{ borderRadius: 6 }}
          />
        </Popconfirm>
      ),
    },
  ];

  const tenantOptions =
    tenantsData?.tenants.map((t) => ({
      label: (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Text style={{ fontWeight: 500 }}>{t.tenantName}</Text>
          <Text style={{ fontSize: 11, color: '#9ca3af', fontFamily: 'monospace' }}>
            {t.domain}
          </Text>
        </div>
      ),
      value: t.tenantId,
    })) ?? [];

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="수신 거부 목록"
        subtitle="반송 또는 스팸 신고로 발송이 차단된 이메일 주소를 관리합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '수신 거부' }]}
      />

      {/* ─── 테넌트 선택 ─── */}
      <Card
        style={{ marginBottom: 16, borderRadius: 12, border: '1px solid #e5e8ed' }}
        bodyStyle={{ padding: 20 }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Text style={{ fontWeight: 500, flexShrink: 0, color: '#374151' }}>테넌트 선택</Text>
          <Select
            style={{ flex: 1, maxWidth: 400 }}
            placeholder={
              <Space size={6}>
                <SearchOutlined />
                테넌트를 검색하거나 선택하세요
              </Space>
            }
            options={tenantOptions}
            value={selectedTenantId || undefined}
            onChange={(val) => {
              setSelectedTenantId(val);
              setPage(0);
            }}
            allowClear
            showSearch
            filterOption={(input, option) =>
              (option?.value as string ?? '').toLowerCase().includes(input.toLowerCase())
            }
            size="large"
          />
        </div>
      </Card>

      {/* ─── 통계 바 (테넌트 선택 시) ─── */}
      {selectedTenantId && (
        <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
          {[
            {
              title: '전체 차단',
              value: totalCount,
              icon: <StopOutlined />,
              color: '#374151',
              bg: '#f8fafc',
            },
            {
              title: '반송 (Bounce)',
              value: bounceCount,
              icon: <ExclamationCircleFilled />,
              color: '#f04438',
              bg: 'rgba(240,68,56,0.08)',
            },
            {
              title: '스팸 신고',
              value: complaintCount,
              icon: <WarningFilled />,
              color: '#f79009',
              bg: 'rgba(247,144,9,0.08)',
            },
          ].map((item) => (
            <Col xs={24} sm={8} key={item.title}>
              <Card
                style={{ borderRadius: 10, border: '1px solid #e5e8ed' }}
                bodyStyle={{ padding: '16px 20px' }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div>
                    <Text
                      style={{ fontSize: 12, color: '#6b7280', fontWeight: 500, display: 'block', marginBottom: 4 }}
                    >
                      {item.title}
                    </Text>
                    <Statistic
                      value={item.value}
                      valueStyle={{ fontSize: 22, fontWeight: 700, color: '#111827' }}
                    />
                  </div>
                  <div
                    style={{
                      width: 36,
                      height: 36,
                      borderRadius: 8,
                      background: item.bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 16,
                      color: item.color,
                    }}
                  >
                    {item.icon}
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* ─── 목록 또는 빈 상태 ─── */}
      {!selectedTenantId ? (
        <Card
          style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
          bodyStyle={{ padding: 60 }}
        >
          <Empty
            image={
              <StopOutlined style={{ fontSize: 48, color: '#d1d5db' }} />
            }
            imageStyle={{ height: 'auto' }}
            description={
              <Space direction="vertical" size={4}>
                <Text style={{ color: '#6b7280', fontWeight: 500 }}>
                  테넌트를 선택하세요
                </Text>
                <Text type="secondary" style={{ fontSize: 13 }}>
                  위에서 테넌트를 선택하면 수신 거부 목록이 표시됩니다.
                </Text>
              </Space>
            }
          />
        </Card>
      ) : (
        <Card
          bodyStyle={{ padding: 0 }}
          style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
        >
          <ProTable<SuppressionEntry>
            rowKey="id"
            columns={columns}
            dataSource={suppressionData?.suppressions ?? []}
            loading={isLoading}
            search={false}
            toolBarRender={false}
            pagination={{
              total: suppressionData?.totalCount ?? 0,
              pageSize: 20,
              current: page + 1,
              onChange: (p) => setPage(p - 1),
              showTotal: (total) => `총 ${total}건`,
            }}
            locale={{
              emptyText: (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={
                    <Text type="secondary" style={{ fontSize: 14 }}>
                      수신 거부 목록이 없습니다.
                    </Text>
                  }
                />
              ),
            }}
          />
        </Card>
      )}
    </div>
  );
}
