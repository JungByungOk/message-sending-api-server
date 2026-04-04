import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Result,
  Row,
  Select,
  Space,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleFilled,
  CloseOutlined,
  CodeOutlined,
  CopyOutlined,
  MailOutlined,
  MinusCircleOutlined,
  PlusOutlined,
  SendOutlined,
  TagOutlined,
} from '@ant-design/icons';
import { useSendEmail, useSendTemplatedEmail, useTemplates } from '@/hooks/useEmail';
import { useTenants, useSenders } from '@/hooks/useTenants';
import type { SendEmailRequest, SendTemplatedEmailRequest } from '@/types/email';
import PageHeader from '@/components/PageHeader';

const { Text } = Typography;
const { TextArea } = Input;

// ─── 이메일 태그 입력 컴포넌트 ───────────────────────────────────────────────
function EmailTagInput({
  value,
  onChange,
  placeholder,
}: {
  value?: string;
  onChange?: (v: string) => void;
  placeholder?: string;
}) {
  const [inputValue, setInputValue] = useState('');
  const emails = value ? value.split(',').map((s) => s.trim()).filter(Boolean) : [];

  const handleAdd = () => {
    const trimmed = inputValue.trim();
    if (!trimmed) return;
    const next = [...emails, trimmed].join(', ');
    onChange?.(next);
    setInputValue('');
  };

  const handleRemove = (email: string) => {
    const next = emails.filter((e) => e !== email).join(', ');
    onChange?.(next);
  };

  return (
    <div
      style={{
        border: '1px solid #e5e8ed',
        borderRadius: 8,
        padding: '6px 10px',
        minHeight: 40,
        background: '#fff',
        display: 'flex',
        flexWrap: 'wrap',
        gap: 4,
        alignItems: 'center',
        cursor: 'text',
      }}
      onClick={() => document.getElementById('email-tag-input')?.focus()}
    >
      {emails.map((email) => (
        <Tag
          key={email}
          closable
          onClose={() => handleRemove(email)}
          closeIcon={<CloseOutlined style={{ fontSize: 10 }} />}
          style={{
            borderRadius: 6,
            margin: 0,
            fontSize: 13,
            background: 'rgba(22,119,255,0.08)',
            border: '1px solid rgba(22,119,255,0.2)',
            color: '#1677ff',
          }}
        >
          {email}
        </Tag>
      ))}
      <input
        id="email-tag-input"
        value={inputValue}
        onChange={(e) => setInputValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            handleAdd();
          }
          if (e.key === 'Backspace' && !inputValue && emails.length > 0) {
            handleRemove(emails[emails.length - 1]);
          }
        }}
        onBlur={handleAdd}
        placeholder={emails.length === 0 ? placeholder : ''}
        style={{
          border: 'none',
          outline: 'none',
          fontSize: 14,
          flex: 1,
          minWidth: 180,
          background: 'transparent',
          color: '#374151',
        }}
      />
    </div>
  );
}

// ─── 발송 확인 모달 ───────────────────────────────────────────────────────────
function ConfirmSendModal({
  open,
  from,
  to,
  subject,
  onConfirm,
  onCancel,
  loading,
}: {
  open: boolean;
  from: string;
  to: string;
  subject: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading: boolean;
}) {
  return (
    <Modal
      title={
        <Space>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'rgba(22,119,255,0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <SendOutlined style={{ color: '#1677ff' }} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 15 }}>이메일 발송 확인</div>
            <div style={{ fontSize: 13, color: '#9ca3af', fontWeight: 400 }}>
              아래 내용으로 이메일을 발송합니다.
            </div>
          </div>
        </Space>
      }
      open={open}
      onOk={onConfirm}
      onCancel={onCancel}
      okText="발송"
      cancelText="취소"
      confirmLoading={loading}
      okButtonProps={{ icon: <SendOutlined />, type: 'primary' }}
      width={440}
    >
      <div style={{ marginTop: 16 }}>
        {[
          { label: '발신자', value: from },
          { label: '수신자', value: to },
          { label: '제목', value: subject },
        ].map((item) => (
          <div
            key={item.label}
            style={{
              display: 'flex',
              gap: 12,
              padding: '10px 0',
              borderBottom: '1px solid #f0f2f5',
            }}
          >
            <Text style={{ width: 60, color: '#6b7280', fontSize: 14, flexShrink: 0 }}>
              {item.label}
            </Text>
            <Text style={{ fontSize: 14, color: '#111827', fontWeight: 500 }}>
              {item.value || '-'}
            </Text>
          </div>
        ))}
      </div>
    </Modal>
  );
}

// ─── 성공 결과 모달 ────────────────────────────────────────────────────────────
function SuccessModal({
  open,
  messageId,
  onClose,
}: {
  open: boolean;
  messageId: string;
  onClose: () => void;
}) {
  return (
    <Modal open={open} onOk={onClose} onCancel={onClose} footer={null} width={440}>
      <Result
        icon={<CheckCircleFilled style={{ color: '#12b76a', fontSize: 48 }} />}
        title="이메일 발송 완료"
        subTitle="이메일이 성공적으로 발송되었습니다."
        extra={[
          <div
            key="msg-id"
            style={{
              background: '#f8fafc',
              border: '1px solid #e5e8ed',
              borderRadius: 8,
              padding: '10px 16px',
              marginBottom: 16,
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <Text style={{ fontSize: 13, color: '#6b7280', flexShrink: 0 }}>메시지 ID</Text>
            <Text
              style={{
                fontFamily: 'monospace',
                fontSize: 11,
                color: '#374151',
                flex: 1,
                wordBreak: 'break-all',
              }}
            >
              {messageId}
            </Text>
            <Tooltip title="복사">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                onClick={() => {
                  void navigator.clipboard.writeText(messageId);
                  void message.success('복사되었습니다.');
                }}
                style={{ color: '#9ca3af', flexShrink: 0 }}
              />
            </Tooltip>
          </div>,
          <Button key="close" type="primary" block onClick={onClose} style={{ borderRadius: 8 }}>
            확인
          </Button>,
        ]}
      />
    </Modal>
  );
}

// ─── 태그 목록 편집기 ─────────────────────────────────────────────────────────
function TagListEditor({ name, addLabel }: { name: string; addLabel: string }) {
  return (
    <Form.List name={name}>
      {(fields, { add, remove }) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {fields.map(({ key, name: fieldName, ...restField }) => (
            <div key={key} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <Form.Item
                {...restField}
                name={[fieldName, name === 'tags' ? 'name' : 'key']}
                style={{ marginBottom: 0, flex: 1 }}
              >
                <Input
                  placeholder={name === 'tags' ? '태그 이름' : '키'}
                  prefix={<TagOutlined style={{ color: '#9ca3af' }} />}
                />
              </Form.Item>
              <Form.Item
                {...restField}
                name={[fieldName, 'value']}
                style={{ marginBottom: 0, flex: 1 }}
              >
                <Input placeholder="값" />
              </Form.Item>
              <Button
                type="text"
                icon={<MinusCircleOutlined />}
                onClick={() => remove(fieldName)}
                style={{ color: '#f04438', flexShrink: 0 }}
              />
            </div>
          ))}
          <Button
            type="dashed"
            onClick={() => add()}
            icon={<PlusOutlined />}
            style={{ borderRadius: 8, color: '#6b7280', borderColor: '#d1d5db' }}
          >
            {addLabel}
          </Button>
        </div>
      )}
    </Form.List>
  );
}

// ─── 텍스트 이메일 폼 ─────────────────────────────────────────────────────────
function TextEmailForm() {
  const [form] = Form.useForm();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);
  const [messageId, setMessageId] = useState('');
  const [selectedTenantId, setSelectedTenantId] = useState('');
  const [pendingValues, setPendingValues] = useState<{
    from: string;
    to: string;
    subject: string;
    body: string;
    tags?: Array<{ name: string; value: string }>;
  } | null>(null);
  const { mutate: send, isPending } = useSendEmail();
  const { data: tenantsData } = useTenants({ size: 100 });
  const { data: senders } = useSenders(selectedTenantId);
  const [htmlPreview, setHtmlPreview] = useState(false);
  const bodyValue = Form.useWatch('body', form) as string | undefined;

  const handleFinish = (values: {
    from: string;
    to: string;
    subject: string;
    body: string;
    tags?: Array<{ name: string; value: string }>;
  }) => {
    setPendingValues(values);
    setConfirmOpen(true);
  };

  const handleConfirm = () => {
    if (!pendingValues) return;
    const payload: SendEmailRequest = {
      from: pendingValues.from,
      to: pendingValues.to.split(',').map((s) => s.trim()).filter(Boolean),
      subject: pendingValues.subject,
      body: pendingValues.body,
      tags: pendingValues.tags?.filter((t) => t?.name && t?.value) ?? [],
    };
    send(payload, {
      onSuccess: (data) => {
        setMessageId(data.messageId);
        setConfirmOpen(false);
        setResultOpen(true);
        form.resetFields();
      },
      onError: () => {
        setConfirmOpen(false);
      },
    });
  };

  return (
    <>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Row gutter={16}>
          <Col xs={24} sm={8}>
            <Form.Item
              label={<Text style={{ fontWeight: 500 }}>발송 테넌트</Text>}
              rules={[{ required: true, message: '테넌트를 선택하세요.' }]}
            >
              <Select
                placeholder="테넌트 선택"
                showSearch
                optionFilterProp="label"
                options={tenantsData?.tenants
                  ?.filter((t) => t.status === 'ACTIVE')
                  .map((t) => ({ label: `${t.tenantName} (${t.domain})`, value: t.tenantId }))}
                onChange={(v) => {
                  setSelectedTenantId(v);
                  form.setFieldValue('from', undefined);
                }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item
              name="from"
              label={<Text style={{ fontWeight: 500 }}>발신자 이메일</Text>}
              rules={[{ required: true, message: '발신자를 선택하세요.' }]}
            >
              <Select
                placeholder={selectedTenantId ? '발신자 선택' : '테넌트를 먼저 선택하세요'}
                disabled={!selectedTenantId}
                options={senders?.map((s) => ({
                  label: s.displayName ? `${s.displayName} <${s.email}>` : s.email,
                  value: s.email,
                }))}
                notFoundContent={selectedTenantId ? '등록된 발신자가 없습니다' : null}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item
              name="to"
              label={<Text style={{ fontWeight: 500 }}>수신자 이메일</Text>}
              rules={[{ required: true, message: '수신자 이메일을 입력하세요.' }]}
              extra={
                <Text style={{ fontSize: 12, color: '#9ca3af' }}>
                  Enter 또는 쉼표로 여러 주소 입력
                </Text>
              }
            >
              <EmailTagInput placeholder="recipient@example.com" />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="subject"
          label={<Text style={{ fontWeight: 500 }}>제목</Text>}
          rules={[{ required: true, message: '제목을 입력하세요.' }]}
        >
          <Input placeholder="이메일 제목" />
        </Form.Item>

        <Form.Item
          name="body"
          label={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
              <Text style={{ fontWeight: 500 }}>본문 (HTML)</Text>
              <Button
                type="link"
                size="small"
                icon={<CodeOutlined />}
                onClick={() => setHtmlPreview(!htmlPreview)}
                style={{ padding: 0, fontSize: 13 }}
              >
                {htmlPreview ? '편집기' : 'HTML 미리보기'}
              </Button>
            </div>
          }
          rules={[{ required: true, message: '본문을 입력하세요.' }]}
        >
          <TextArea rows={8} placeholder="<html><body>이메일 본문을 입력하세요</body></html>" />
        </Form.Item>

        {htmlPreview && bodyValue && (
          <div style={{ marginBottom: 16 }}>
            <Alert
              type="info"
              showIcon
              message="HTML 미리보기 (실제 렌더링과 다를 수 있습니다)"
              style={{ marginBottom: 8 }}
            />
            <div
              style={{
                border: '1px solid #e5e8ed',
                borderRadius: 8,
                padding: 16,
                minHeight: 80,
                background: '#fff',
              }}
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: bodyValue }}
            />
          </div>
        )}

        <Card
          size="small"
          title={
            <Space size={6}>
              <TagOutlined style={{ color: '#6b7280' }} />
              <Text style={{ fontSize: 14, fontWeight: 500 }}>태그 (선택사항)</Text>
            </Space>
          }
          style={{ marginBottom: 16, borderRadius: 8, border: '1px solid #e5e8ed' }}
          bodyStyle={{ padding: 16 }}
        >
          <TagListEditor name="tags" addLabel="태그 추가" />
        </Card>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            icon={<SendOutlined />}
            size="large"
            style={{ borderRadius: 8, minWidth: 120 }}
          >
            발송
          </Button>
        </Form.Item>
      </Form>

      <ConfirmSendModal
        open={confirmOpen}
        from={pendingValues?.from ?? ''}
        to={pendingValues?.to ?? ''}
        subject={pendingValues?.subject ?? ''}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmOpen(false)}
        loading={isPending}
      />
      <SuccessModal
        open={resultOpen}
        messageId={messageId}
        onClose={() => setResultOpen(false)}
      />
    </>
  );
}

// ─── 템플릿 이메일 폼 ─────────────────────────────────────────────────────────
function TemplatedEmailForm() {
  const [form] = Form.useForm();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);
  const [messageId, setMessageId] = useState('');
  const [selectedTenantId, setSelectedTenantId] = useState('');
  const { data: tenantsData } = useTenants({ size: 100 });
  const { data: senders } = useSenders(selectedTenantId);
  const [pendingValues, setPendingValues] = useState<{
    templateName: string;
    from: string;
    to: string;
    cc?: string;
    bcc?: string;
    templateData?: Array<{ key: string; value: string }>;
    tags?: Array<{ name: string; value: string }>;
  } | null>(null);
  const { data: templates, isLoading: isLoadingTemplates } = useTemplates();
  const { mutate: send, isPending } = useSendTemplatedEmail();

  const handleFinish = (values: typeof pendingValues & NonNullable<unknown>) => {
    setPendingValues(values);
    setConfirmOpen(true);
  };

  const handleConfirm = () => {
    if (!pendingValues) return;
    const templateData: Record<string, string> = {};
    pendingValues.templateData?.forEach((item) => {
      if (item?.key) templateData[item.key] = item.value ?? '';
    });

    const parseEmails = (v: string | undefined) =>
      v ? v.split(',').map((s) => s.trim()).filter(Boolean) : undefined;

    const payload: SendTemplatedEmailRequest = {
      templateName: pendingValues.templateName,
      from: pendingValues.from,
      to: parseEmails(pendingValues.to) ?? [],
      cc: parseEmails(pendingValues.cc),
      bcc: parseEmails(pendingValues.bcc),
      templateData,
      tags: pendingValues.tags?.filter((t) => t?.name && t?.value) ?? [],
    };
    send(payload, {
      onSuccess: (data) => {
        setMessageId(data.messageId);
        setConfirmOpen(false);
        setResultOpen(true);
        form.resetFields();
      },
      onError: () => {
        setConfirmOpen(false);
      },
    });
  };

  return (
    <>
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="templateName"
          label={<Text style={{ fontWeight: 500 }}>템플릿 선택</Text>}
          rules={[{ required: true, message: '템플릿을 선택하세요.' }]}
        >
          <Select
            placeholder="템플릿을 선택하세요"
            loading={isLoadingTemplates}
            showSearch
            optionFilterProp="label"
            options={templates?.map((t) => ({ label: t.name, value: t.name }))}
            size="large"
          />
        </Form.Item>

        <Row gutter={16}>
          <Col xs={24} sm={8}>
            <Form.Item
              label={<Text style={{ fontWeight: 500 }}>발송 테넌트</Text>}
              rules={[{ required: true, message: '테넌트를 선택하세요.' }]}
            >
              <Select
                placeholder="테넌트 선택"
                showSearch
                optionFilterProp="label"
                options={tenantsData?.tenants
                  ?.filter((t) => t.status === 'ACTIVE')
                  .map((t) => ({ label: `${t.tenantName} (${t.domain})`, value: t.tenantId }))}
                onChange={(v) => {
                  setSelectedTenantId(v);
                  form.setFieldValue('from', undefined);
                }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item
              name="from"
              label={<Text style={{ fontWeight: 500 }}>발신자 이메일</Text>}
              rules={[{ required: true, message: '발신자를 선택하세요.' }]}
            >
              <Select
                placeholder={selectedTenantId ? '발신자 선택' : '테넌트를 먼저 선택하세요'}
                disabled={!selectedTenantId}
                options={senders?.map((s) => ({
                  label: s.displayName ? `${s.displayName} <${s.email}>` : s.email,
                  value: s.email,
                }))}
                notFoundContent={selectedTenantId ? '등록된 발신자가 없습니다' : null}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item
              name="to"
              label={<Text style={{ fontWeight: 500 }}>수신자 이메일</Text>}
              rules={[{ required: true, message: '수신자 이메일을 입력하세요.' }]}
              extra={<Text style={{ fontSize: 12, color: '#9ca3af' }}>Enter 또는 쉼표로 구분</Text>}
            >
              <EmailTagInput placeholder="recipient@example.com" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="cc"
              label={<Text style={{ fontWeight: 500 }}>참조 (CC)</Text>}
              extra={<Text style={{ fontSize: 12, color: '#9ca3af' }}>Enter 또는 쉼표로 구분</Text>}
            >
              <EmailTagInput placeholder="cc@example.com" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item
              name="bcc"
              label={<Text style={{ fontWeight: 500 }}>숨은 참조 (BCC)</Text>}
              extra={<Text style={{ fontSize: 12, color: '#9ca3af' }}>Enter 또는 쉼표로 구분</Text>}
            >
              <EmailTagInput placeholder="bcc@example.com" />
            </Form.Item>
          </Col>
        </Row>

        <Card
          size="small"
          title={
            <Space size={6}>
              <CodeOutlined style={{ color: '#6b7280' }} />
              <Text style={{ fontSize: 14, fontWeight: 500 }}>템플릿 데이터</Text>
            </Space>
          }
          style={{ marginBottom: 12, borderRadius: 8, border: '1px solid #e5e8ed' }}
          bodyStyle={{ padding: 16 }}
        >
          <TagListEditor name="templateData" addLabel="데이터 추가" />
        </Card>

        <Card
          size="small"
          title={
            <Space size={6}>
              <TagOutlined style={{ color: '#6b7280' }} />
              <Text style={{ fontSize: 14, fontWeight: 500 }}>태그 (선택사항)</Text>
            </Space>
          }
          style={{ marginBottom: 16, borderRadius: 8, border: '1px solid #e5e8ed' }}
          bodyStyle={{ padding: 16 }}
        >
          <TagListEditor name="tags" addLabel="태그 추가" />
        </Card>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            icon={<SendOutlined />}
            size="large"
            style={{ borderRadius: 8, minWidth: 120 }}
          >
            발송
          </Button>
        </Form.Item>
      </Form>

      <ConfirmSendModal
        open={confirmOpen}
        from={pendingValues?.from ?? ''}
        to={pendingValues?.to ?? ''}
        subject={`템플릿: ${pendingValues?.templateName ?? ''}`}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmOpen(false)}
        loading={isPending}
      />
      <SuccessModal
        open={resultOpen}
        messageId={messageId}
        onClose={() => setResultOpen(false)}
      />
    </>
  );
}

// ─── 이메일 발송 페이지 ────────────────────────────────────────────────────────
export default function SendEmailPage() {
  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="이메일 발송"
        subtitle="직접 이메일을 작성하거나 템플릿을 사용하여 발송합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '이메일 발송' }]}
      />

      <Card style={{ borderRadius: 12, border: '1px solid #e5e8ed' }} bodyStyle={{ padding: 0 }}>
        <Tabs
          defaultActiveKey="text"
          size="large"
          style={{ padding: '0 24px' }}
          tabBarStyle={{ marginBottom: 0, borderBottom: '1px solid #f0f2f5' }}
          items={[
            {
              key: 'text',
              label: (
                <Space size={6}>
                  <MailOutlined />
                  텍스트 이메일
                </Space>
              ),
              children: (
                <div style={{ padding: '24px 0' }}>
                  <TextEmailForm />
                </div>
              ),
            },
            {
              key: 'templated',
              label: (
                <Space size={6}>
                  <CodeOutlined />
                  템플릿 이메일
                </Space>
              ),
              children: (
                <div style={{ padding: '24px 0' }}>
                  <TemplatedEmailForm />
                </div>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
