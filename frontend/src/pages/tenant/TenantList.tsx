import { useState } from 'react';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import {
  Button,
  Dropdown,
  Empty,
  Input,
  Modal,
  Space,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  DeleteOutlined,
  EllipsisOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  PlusOutlined,
  StopOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { useActivateTenant, useDeactivateTenant, useDeleteTenant, useTenants } from '@/hooks/useTenants';
import type { Tenant } from '@/types/tenant';
import StatusTag from '@/components/StatusTag';
import PageHeader from '@/components/PageHeader';

const { Text } = Typography;

export default function TenantList() {
  const navigate = useNavigate();
  const { data, isLoading } = useTenants();
  const { mutate: activate, isPending: isActivating } = useActivateTenant();
  const { mutate: deactivate, isPending: isDeactivating } = useDeactivateTenant();
  const { mutate: deleteTenant, isPending: isDeleting } = useDeleteTenant();

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Tenant | null>(null);
  const [domainInput, setDomainInput] = useState('');

  const handleDeactivate = (tenantId: string) => {
    deactivate(tenantId, {
      onSuccess: () => {
        void message.success('테넌트가 비활성화되었습니다.');
      },
    });
  };

  const handleDeleteOpen = (tenant: Tenant) => {
    setDeleteTarget(tenant);
    setDomainInput('');
    setDeleteModalOpen(true);
  };

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;
    deleteTenant(deleteTarget.tenantId, {
      onSuccess: () => {
        void message.success('테넌트가 영구 삭제되었습니다.');
        setDeleteModalOpen(false);
        setDeleteTarget(null);
      },
    });
  };

  const columns: ProColumns<Tenant>[] = [
    {
      title: '테넌트명',
      dataIndex: 'tenantName',
      ellipsis: true,
      render: (_, record) => (
        <div>
          <Text
            style={{ fontWeight: 600, color: '#111827', cursor: 'pointer', display: 'block' }}
            onClick={() => navigate(`/tenant/${record.tenantId}`)}
          >
            {record.tenantName}
          </Text>
          <Text style={{ fontSize: 12, color: '#9ca3af' }}>{record.tenantId}</Text>
        </div>
      ),
    },
    {
      title: '도메인',
      dataIndex: 'domain',
      ellipsis: true,
      render: (val) => (
        <Text style={{ color: '#374151' }}>
          {val as string}
        </Text>
      ),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (_, record) => <StatusTag type="tenant" status={record.status} />,
    },
    {
      title: '인증',
      dataIndex: 'verificationStatus',
      width: 120,
      render: (_, record) => (
        <StatusTag type="verification" status={record.verificationStatus} />
      ),
    },
    {
      title: '일일 쿼터',
      dataIndex: 'quotaDaily',
      align: 'right',
      width: 110,
      render: (val) => (
        <Text style={{ fontSize: 14, fontWeight: 500, color: '#374151' }}>
          {(val as number).toLocaleString()}
        </Text>
      ),
    },
    {
      title: '월간 쿼터',
      dataIndex: 'quotaMonthly',
      align: 'right',
      width: 110,
      render: (val) => (
        <Text style={{ fontSize: 14, fontWeight: 500, color: '#374151' }}>
          {(val as number).toLocaleString()}
        </Text>
      ),
    },
    {
      title: '생성일',
      dataIndex: 'createdAt',
      width: 110,
      render: (val) => (
        <Text style={{ fontSize: 13, color: '#6b7280' }}>
          {dayjs(val as string).format('YYYY-MM-DD')}
        </Text>
      ),
    },
    {
      title: '',
      width: 60,
      fixed: 'right',
      render: (_, record) => (
        <Dropdown
          menu={{
            items: [
              {
                key: 'view',
                icon: <EyeOutlined />,
                label: '상세 보기',
                onClick: () => navigate(`/tenant/${record.tenantId}`),
              },
              {
                type: 'divider',
              },
              {
                key: 'activate',
                icon: <CheckCircleOutlined />,
                label: '활성화',
                disabled: record.status !== 'INACTIVE' || isActivating,
                onClick: () => {
                  activate(record.tenantId, {
                    onSuccess: () => {
                      void message.success('테넌트가 활성화되었습니다.');
                    },
                  });
                },
              },
              {
                key: 'deactivate',
                icon: <StopOutlined />,
                label: '비활성화',
                danger: true,
                disabled: isDeactivating || record.status === 'INACTIVE',
                onClick: () => {
                  Modal.confirm({
                    title: '테넌트 비활성화',
                    content: '정말 비활성화하시겠습니까?',
                    okText: '비활성화',
                    cancelText: '취소',
                    okButtonProps: { danger: true },
                    onOk: () => handleDeactivate(record.tenantId),
                  });
                },
              },
              {
                key: 'delete',
                icon: <DeleteOutlined />,
                label: '영구 삭제',
                danger: true,
                disabled: record.status !== 'INACTIVE' || isDeleting,
                onClick: () => handleDeleteOpen(record),
              },
            ],
          }}
          trigger={['click']}
        >
          <Button
            type="text"
            icon={<EllipsisOutlined />}
            size="small"
            style={{ color: '#6b7280', borderRadius: 6 }}
          />
        </Dropdown>
      ),
    },
  ];

  const emptyTenants = !isLoading && (data?.tenants.length ?? 0) === 0;

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="테넌트 목록"
        subtitle="서비스를 이용 중인 테넌트를 관리합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '테넌트 관리' }]}
        actions={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/tenant/create')}
            style={{ borderRadius: 8 }}
          >
            테넌트 생성
          </Button>
        }
      />

      <ProTable<Tenant>
        rowKey="tenantId"
        columns={columns}
        dataSource={data?.tenants ?? []}
        loading={isLoading}
        search={false}
        toolBarRender={false}
        pagination={{
          total: data?.totalCount ?? 0,
          showSizeChanger: true,
          showTotal: (total) => `총 ${total}건`,
          style: { padding: '12px 16px' },
        }}
        locale={{
          emptyText: emptyTenants ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Space direction="vertical" size={4}>
                  <Text style={{ color: '#6b7280' }}>등록된 테넌트가 없습니다.</Text>
                  <Button
                    type="primary"
                    size="small"
                    icon={<TeamOutlined />}
                    onClick={() => navigate('/tenant/create')}
                  >
                    첫 테넌트 생성
                  </Button>
                </Space>
              }
            />
          ) : (
            '데이터 없음'
          ),
        }}
        cardBordered
        style={{ borderRadius: 12 }}
        tableStyle={{ borderRadius: 12 }}
      />

      {/* 영구 삭제 확인 모달 */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: '#f04438', fontSize: 20 }} />
            <span>테넌트 영구 삭제</span>
          </Space>
        }
        open={deleteModalOpen}
        onCancel={() => setDeleteModalOpen(false)}
        onOk={handleDeleteConfirm}
        okText="영구 삭제"
        cancelText="취소"
        okButtonProps={{
          danger: true,
          disabled: domainInput !== deleteTarget?.domain,
          loading: isDeleting,
        }}
        width={480}
      >
        <div style={{ marginTop: 16 }}>
          <Text>
            이 작업은 <Text strong>되돌릴 수 없습니다.</Text> 테넌트와 관련된 모든 데이터가 영구적으로 삭제됩니다.
          </Text>
          <div style={{ marginTop: 16 }}>
            <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
              확인을 위해 도메인 <Text strong code>{deleteTarget?.domain}</Text>을 입력해 주세요.
            </Text>
            <Input
              placeholder={deleteTarget?.domain}
              value={domainInput}
              onChange={(e) => setDomainInput(e.target.value)}
              status={domainInput && domainInput !== deleteTarget?.domain ? 'error' : undefined}
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}
