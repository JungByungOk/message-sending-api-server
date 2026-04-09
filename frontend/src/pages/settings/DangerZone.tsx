import { useState } from 'react';
import { Button, Card, Input, Modal, Space, Typography, message, Alert } from 'antd';
import { DeleteOutlined, ExclamationCircleFilled, WarningOutlined } from '@ant-design/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi } from '@/api/settings';
import type { ResetResult } from '@/types/settings';

const { Text, Title } = Typography;

function ResetConfirmModal({
  open,
  title,
  description,
  loading,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  description: React.ReactNode;
  loading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const [inputValue, setInputValue] = useState('');

  const handleCancel = () => {
    setInputValue('');
    onCancel();
  };

  const handleConfirm = () => {
    onConfirm();
    setInputValue('');
  };

  return (
    <Modal
      centered
      open={open}
      title={
        <Space>
          <ExclamationCircleFilled style={{ color: '#f04438', fontSize: 20 }} />
          <span>{title}</span>
        </Space>
      }
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel} disabled={loading}>
          취소
        </Button>,
        <Button
          key="confirm"
          danger
          type="primary"
          loading={loading}
          disabled={inputValue !== 'RESET'}
          onClick={handleConfirm}
        >
          초기화 실행
        </Button>,
      ]}
      width={480}
    >
      <div style={{ marginTop: 16 }}>
        <Alert
          type="error"
          showIcon
          icon={<WarningOutlined />}
          message="이 작업은 되돌릴 수 없습니다"
          description="초기화된 데이터는 복구할 수 없습니다. 신중하게 진행하세요."
          style={{ marginBottom: 16 }}
        />
        <div style={{ marginBottom: 16 }}>{description}</div>
        <div>
          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            계속하려면 아래에 <Text code>RESET</Text>을 입력하세요:
          </Text>
          <Input
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="RESET"
            status={inputValue && inputValue !== 'RESET' ? 'error' : undefined}
          />
        </div>
      </div>
    </Modal>
  );
}

export default function DangerZone() {
  const [emailResetOpen, setEmailResetOpen] = useState(false);
  const [fullResetOpen, setFullResetOpen] = useState(false);
  const queryClient = useQueryClient();

  const showResult = (result: ResetResult) => {
    if (result.success) {
      const summary = Object.entries(result.deleted)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
      void message.success(`초기화 완료 (${summary || '삭제 대상 없음'})`);
    }
    if (result.warnings?.length > 0) {
      result.warnings.forEach((w) => void message.warning(w));
    }
  };

  const emailResetMutation = useMutation({
    mutationFn: () => settingsApi.resetEmailResults({ confirm: 'RESET' }),
    onSuccess: (result) => {
      setEmailResetOpen(false);
      showResult(result);
      void queryClient.invalidateQueries({ queryKey: ['emailResults'] });
    },
    onError: () => {
      void message.error('발송 결과 초기화에 실패했습니다.');
    },
  });

  const fullResetMutation = useMutation({
    mutationFn: () => settingsApi.resetAll({ confirm: 'RESET' }),
    onSuccess: (result) => {
      setFullResetOpen(false);
      showResult(result);
      void queryClient.invalidateQueries();
    },
    onError: () => {
      void message.error('전체 초기화에 실패했습니다.');
    },
  });

  return (
    <div>
      <Title level={4} style={{ margin: 0, marginBottom: 4, color: '#f04438' }}>
        <WarningOutlined style={{ marginRight: 8 }} />
        Danger Zone
      </Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
        데이터를 초기화합니다. 시스템 설정과 수신거부 목록은 유지됩니다.
      </Text>

      <Card
        style={{
          borderColor: '#fca5a5',
          borderRadius: 10,
          background: '#fef2f2',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div>
            <Text strong style={{ fontSize: 15 }}>발송 결과 초기화</Text>
            <br />
            <Text type="secondary" style={{ fontSize: 13 }}>
              이메일 발송 기록, 이벤트 이력, 첨부파일 정보를 삭제합니다.
            </Text>
          </div>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={() => setEmailResetOpen(true)}
          >
            발송 결과 초기화
          </Button>
        </div>

        <div style={{ borderTop: '1px solid #fca5a5', paddingTop: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Text strong style={{ fontSize: 15, color: '#dc2626' }}>전체 초기화</Text>
            <br />
            <Text type="secondary" style={{ fontSize: 13 }}>
              테넌트, 발송 기록, 예약 작업, AWS 리소스를 모두 삭제합니다.
            </Text>
          </div>
          <Button
            danger
            type="primary"
            icon={<DeleteOutlined />}
            onClick={() => setFullResetOpen(true)}
          >
            전체 초기화
          </Button>
        </div>
      </Card>

      <ResetConfirmModal
        open={emailResetOpen}
        title="발송 결과 초기화"
        description={
          <Text>
            다음 데이터가 <Text strong>영구 삭제</Text>됩니다:
            <ul style={{ marginTop: 8 }}>
              <li>이메일 발송 마스터/상세 기록</li>
              <li>이메일 이벤트 이력 (Delivery, Bounce, Open 등)</li>
              <li>예약 발송 배치 데이터</li>
              <li>첨부파일 정보</li>
              <li>AWS DynamoDB 이벤트/중복방지 데이터</li>
            </ul>
          </Text>
        }
        loading={emailResetMutation.isPending}
        onConfirm={() => emailResetMutation.mutate()}
        onCancel={() => setEmailResetOpen(false)}
      />

      <ResetConfirmModal
        open={fullResetOpen}
        title="전체 초기화"
        description={
          <Text>
            다음 데이터가 <Text strong>영구 삭제</Text>됩니다:
            <ul style={{ marginTop: 8 }}>
              <li>모든 테넌트 및 발신자 정보</li>
              <li>이메일 발송 기록 및 이벤트 이력</li>
              <li>Quartz 예약 작업</li>
              <li>AWS 리소스 (ConfigSet, SES Identity, DynamoDB 설정)</li>
            </ul>
            <Text type="secondary">시스템 설정, 수신거부/블랙리스트는 유지됩니다.</Text>
          </Text>
        }
        loading={fullResetMutation.isPending}
        onConfirm={() => fullResetMutation.mutate()}
        onCancel={() => setFullResetOpen(false)}
      />
    </div>
  );
}
