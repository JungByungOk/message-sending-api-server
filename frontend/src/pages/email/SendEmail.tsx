import React, { useId, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  DatePicker,
  Form,
  Input,
  Modal,
  Radio,
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
  ClockCircleOutlined,
  CloseOutlined,
  CodeOutlined,
  CopyOutlined,
  InfoCircleOutlined,
  MailOutlined,
  MinusCircleOutlined,
  PlusOutlined,
  SendOutlined,
  TagOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { useSendEmail, useSendTemplatedEmail, useTemplate, useTemplates } from '@/hooks/useEmail';
import { useCreateJob } from '@/hooks/useScheduler';
import { useTenants, useSenders } from '@/hooks/useTenants';
import type { SendEmailRequest, SendTemplatedEmailRequest } from '@/types/email';
import type { ScheduleJobRequest } from '@/types/scheduler';
import PageHeader from '@/components/PageHeader';

const { Text } = Typography;
const { TextArea } = Input;

// ─── 이메일 태그 입력 컴포넌트 ───────────────────────────────────────────────
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function EmailTagInput({
  value,
  onChange,
  placeholder,
  inputId: inputIdProp,
}: {
  value?: string;
  onChange?: (v: string) => void;
  placeholder?: string;
  inputId?: string;
}) {
  const generatedId = useId();
  const inputId = inputIdProp ?? generatedId;
  const [inputValue, setInputValue] = useState('');
  const emails = value ? value.split(',').map((s) => s.trim()).filter(Boolean) : [];

  const addEmails = (input: string) => {
    const candidates = input.split(/[,;\s]+/).map((s) => s.trim()).filter(Boolean);
    const valid: string[] = [];
    const invalid: string[] = [];
    for (const c of candidates) {
      if (EMAIL_REGEX.test(c)) {
        if (!emails.includes(c) && !valid.includes(c)) valid.push(c);
      } else {
        invalid.push(c);
      }
    }
    if (invalid.length > 0) {
      void message.warning(`올바른 이메일 형식이 아닙니다: ${invalid.slice(0, 3).join(', ')}${invalid.length > 3 ? ` 외 ${invalid.length - 3}건` : ''}`);
    }
    if (valid.length > 0) {
      const next = [...emails, ...valid].join(', ');
      onChange?.(next);
    }
    setInputValue('');
  };

  const handleAdd = () => {
    const trimmed = inputValue.trim();
    if (!trimmed) return;
    addEmails(trimmed);
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
      onClick={() => document.getElementById(inputId)?.focus()}
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
        id={inputId}
        value={inputValue}
        onChange={(e) => setInputValue(e.target.value)}
        onPaste={(e) => {
          const pasted = e.clipboardData.getData('text');
          if (pasted.includes(',') || pasted.includes(';') || pasted.includes('\n')) {
            e.preventDefault();
            addEmails(pasted);
          }
        }}
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
  scheduleTime,
  onConfirm,
  onCancel,
  loading,
}: {
  open: boolean;
  from: string;
  to: string;
  subject: string;
  scheduleTime?: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading: boolean;
}) {
  const isScheduled = !!scheduleTime;
  return (
    <Modal
      centered
      title={
        <Space>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: isScheduled ? 'rgba(114,46,209,0.1)' : 'rgba(22,119,255,0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {isScheduled
              ? <ClockCircleOutlined style={{ color: '#722ed1' }} />
              : <SendOutlined style={{ color: '#1677ff' }} />}
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 15 }}>
              {isScheduled ? '이메일 예약 발송 확인' : '이메일 발송 확인'}
            </div>
            <div style={{ fontSize: 13, color: '#9ca3af', fontWeight: 400 }}>
              {isScheduled
                ? '아래 내용으로 이메일을 예약합니다.'
                : '아래 내용으로 이메일을 발송합니다.'}
            </div>
          </div>
        </Space>
      }
      open={open}
      onOk={onConfirm}
      onCancel={onCancel}
      okText={isScheduled ? '예약하기' : '지금 보내기'}
      cancelText="취소"
      confirmLoading={loading}
      okButtonProps={{
        icon: isScheduled ? <ClockCircleOutlined /> : <SendOutlined />,
        type: 'primary',
        style: isScheduled ? { background: '#722ed1', borderColor: '#722ed1' } : undefined,
      }}
      width={440}
    >
      <div style={{ marginTop: 16 }}>
        {[
          { label: '발신자', value: from },
          { label: '수신자', value: to },
          { label: '제목', value: subject },
          ...(isScheduled
            ? [{ label: '발송 시점', value: scheduleTime }]
            : [{ label: '발송 시점', value: '즉시 (지금)' }]),
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
            <Text style={{ width: 70, color: '#6b7280', fontSize: 14, flexShrink: 0 }}>
              {item.label}
            </Text>
            <Text
              style={{
                fontSize: 14,
                color: item.label === '발송 시점' && isScheduled ? '#722ed1' : '#111827',
                fontWeight: 500,
              }}
            >
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
  scheduleTime,
  onClose,
}: {
  open: boolean;
  messageId: string;
  scheduleTime?: string;
  onClose: () => void;
}) {
  const navigate = useNavigate();
  const isScheduled = !!scheduleTime;

  return (
    <Modal centered open={open} onOk={onClose} onCancel={onClose} footer={null} width={520}>
      <Result
        icon={
          isScheduled
            ? <ClockCircleOutlined style={{ color: '#722ed1', fontSize: 48 }} />
            : <CheckCircleFilled style={{ color: '#12b76a', fontSize: 48 }} />
        }
        title={isScheduled ? '이메일 예약 완료' : '이메일 발송 완료'}
        subTitle={
          isScheduled
            ? `${scheduleTime}에 발송 예정입니다.`
            : '이메일이 성공적으로 발송되었습니다.'
        }
        extra={[
          !isScheduled && (
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
                  fontSize: 13,
                  color: '#374151',
                  flex: 1,
                  whiteSpace: 'nowrap',
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
            </div>
          ),
          <Button
            key="results"
            type="primary"
            block
            onClick={() => {
              onClose();
              navigate(isScheduled ? '/scheduler' : `/email/results?messageId=${encodeURIComponent(messageId)}`);
            }}
            style={{
              borderRadius: 8,
              marginBottom: 8,
              ...(isScheduled ? { background: '#722ed1', borderColor: '#722ed1' } : {}),
            }}
          >
            {isScheduled ? '예약 작업 목록 보기' : '결과 조회하기'}
          </Button>,
          <Button key="close" block onClick={onClose} style={{ borderRadius: 8 }}>
            확인
          </Button>,
        ].filter(Boolean)}
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
  const [adPrefix, setAdPrefix] = useState(false);
  const subjectValue = Form.useWatch('subject', form) || '';
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
    const finalSubject = adPrefix ? `(광고) ${values.subject}` : values.subject;
    setPendingValues({ ...values, subject: finalSubject });
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
    const selectedTenant = tenantsData?.tenants?.find((t) => t.tenantId === selectedTenantId);
    send({ payload, tenantApiKey: selectedTenant?.apiKey }, {
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
              name="tenantId"
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
                  form.setFieldsValue({ tenantId: v });
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
              <EmailTagInput placeholder="recipient@example.com" inputId="text-email-to" />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="subject"
          label={<Text style={{ fontWeight: 500 }}>제목</Text>}
          rules={[{ required: true, message: '제목을 입력하세요.' }]}
        >
          <Input
            placeholder="이메일 제목"
            prefix={adPrefix ? <Text style={{ color: '#9ca3af' }}>(광고)</Text> : undefined}
          />
        </Form.Item>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: -16, marginBottom: 16 }}>
          <Checkbox
            checked={adPrefix}
            onChange={(e) => setAdPrefix(e.target.checked)}
          >
            <Text style={{ fontSize: 12, color: '#6b7280' }}>"(광고)" 표시하기</Text>
          </Checkbox>
          <Text style={{ fontSize: 12, color: '#9ca3af' }}>
            {((adPrefix ? '(광고) ' : '') + subjectValue).length}자
          </Text>
        </div>

        <Form.Item
          name="body"
          label={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
              <Text style={{ fontWeight: 500, marginRight: 12 }}>본문 (HTML)</Text>
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
              <Tooltip
                title={
                  <div style={{ fontSize: 12, lineHeight: 1.8 }}>
                    <strong>이메일 발송 목적을 분류하는 라벨</strong>입니다.<br />
                    이메일 발송 이벤트(오픈, 클릭, 반송 등) 발생 시 태그 기반으로 통계를 분석할 수 있습니다.<br /><br />
                    <strong>예시</strong><br />
                    <span style={{ fontFamily: 'monospace', background: 'rgba(255,255,255,0.15)', padding: '1px 4px', borderRadius: 3 }}>campaign: 2026-april-promo</span><br />
                    <span style={{ fontFamily: 'monospace', background: 'rgba(255,255,255,0.15)', padding: '1px 4px', borderRadius: 3 }}>type: marketing</span><br /><br />
                    위처럼 설정하면 캠페인별 오픈율, 클릭률 등을 추적할 수 있습니다.<br />
                    테스트 발송 시에는 비워도 무방합니다.
                  </div>
                }
                overlayStyle={{ maxWidth: 360 }}
              >
                <InfoCircleOutlined style={{ color: '#9ca3af', cursor: 'help' }} />
              </Tooltip>
            </Space>
          }
          style={{ marginBottom: 16, borderRadius: 8, border: '1px solid #e5e8ed' }}
          styles={{ body: { padding: 16 } }}
        >
          <TagListEditor name="tags" addLabel="태그 추가" />
        </Card>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            icon={<SendOutlined />}
            size="large"
            loading={isPending}
            disabled={isPending}
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
        onClose={() => {
          setResultOpen(false);
          setSelectedTenantId('');
        }}
      />
    </>
  );
}

// ─── 템플릿 이메일 폼 ─────────────────────────────────────────────────────────
// HTML/텍스트에서 {{변수명}} 패턴 추출
function extractTemplateVariables(html?: string, text?: string): string[] {
  const combined = (html ?? '') + (text ?? '');
  const matches = combined.match(/\{\{([^}]+)\}\}/g);
  if (!matches) return [];
  const vars = [...new Set(matches.map((m) => m.replace(/\{\{|\}\}/g, '').trim()))];
  return vars;
}

function TemplatedEmailForm() {
  const [form] = Form.useForm();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);
  const [messageId, setMessageId] = useState('');
  const [selectedTenantId, setSelectedTenantId] = useState('');
  const [sendMode, setSendMode] = useState<'now' | 'schedule'>('now');
  const [scheduleDate, setScheduleDate] = useState<Dayjs | null>(null);
  const [selectedTemplateName, setSelectedTemplateName] = useState<string | null>(null);
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
  const { data: templateDetail } = useTemplate(selectedTemplateName);
  const { mutate: send, isPending: isSendPending } = useSendTemplatedEmail();
  const { mutate: createJob, isPending: isSchedulePending } = useCreateJob();
  const isPending = isSendPending || isSchedulePending;

  const scheduleTimeFormatted = scheduleDate?.format('YYYY-MM-DD HH:mm:ss') ?? '';

  // 템플릿 내용이 로드되면 변수를 추출하여 templateData 폼에 자동 채움
  React.useEffect(() => {
    if (templateDetail) {
      const vars = extractTemplateVariables(templateDetail.htmlPart, templateDetail.textPart);
      if (vars.length > 0) {
        const existing = (form.getFieldValue('templateData') as Array<{ key: string; value: string }>) ?? [];
        const existingKeys = new Set(existing.map((e) => e?.key).filter(Boolean));
        const newEntries = vars
          .filter((v) => !existingKeys.has(v))
          .map((v) => ({ key: v, value: '' }));
        if (newEntries.length > 0) {
          form.setFieldsValue({ templateData: [...existing, ...newEntries] });
        }
      }
    }
  }, [templateDetail, form]);

  const handleFinish = (values: typeof pendingValues & NonNullable<unknown>) => {
    if (sendMode === 'schedule' && !scheduleDate) {
      void message.warning('예약 발송 시간을 선택하세요.');
      return;
    }
    if (sendMode === 'schedule' && scheduleDate && scheduleDate.isBefore(dayjs())) {
      void message.warning('예약 시간은 현재 시간 이후여야 합니다.');
      return;
    }
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

    if (sendMode === 'schedule' && scheduleDate) {
      // 예약 발송 - Scheduler API 호출
      const jobName = `email-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
      const toList = parseEmails(pendingValues.to) ?? [];
      const payload: ScheduleJobRequest = {
        jobName,
        jobGroup: 'DEFAULT',
        description: `예약 발송: ${pendingValues.templateName}`,
        startDateAt: scheduleDate.utc().format('YYYY-MM-DDTHH:mm:ss'),
        templateName: pendingValues.templateName,
        from: pendingValues.from,
        templatedEmailList: toList.map((email) => ({
          to: [email],
          cc: parseEmails(pendingValues.cc),
          bcc: parseEmails(pendingValues.bcc),
          templateParameters: templateData,
        })),
        tags: pendingValues.tags?.filter((t) => t?.name && t?.value) ?? [],
      };
      const selectedTenantForSchedule = tenantsData?.tenants?.find((t) => t.tenantId === selectedTenantId);
      createJob({ payload, tenantApiKey: selectedTenantForSchedule?.apiKey }, {
        onSuccess: () => {
          setMessageId('');
          setConfirmOpen(false);
          setResultOpen(true);
        },
        onError: () => {
          setConfirmOpen(false);
        },
      });
    } else {
      // 즉시 발송
      const payload: SendTemplatedEmailRequest = {
        templateName: pendingValues.templateName,
        from: pendingValues.from,
        to: parseEmails(pendingValues.to) ?? [],
        cc: parseEmails(pendingValues.cc),
        bcc: parseEmails(pendingValues.bcc),
        subject: templateDetail?.subjectPart,
        templateData,
        tags: pendingValues.tags?.filter((t) => t?.name && t?.value) ?? [],
      };
      const selectedTenant = tenantsData?.tenants?.find((t) => t.tenantId === selectedTenantId);
      send({ payload, tenantApiKey: selectedTenant?.apiKey }, {
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
    }
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
            onChange={(v) => {
              setSelectedTemplateName(v);
              form.setFieldsValue({ templateData: [] });
            }}
          />
        </Form.Item>

        <Row gutter={16}>
          <Col xs={24} sm={8}>
            <Form.Item
              name="tenantId"
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
                  form.setFieldsValue({ tenantId: v });
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
              <EmailTagInput placeholder="recipient@example.com" inputId="templated-email-to" />
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
              <EmailTagInput placeholder="cc@example.com" inputId="templated-email-cc" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item
              name="bcc"
              label={<Text style={{ fontWeight: 500 }}>숨은 참조 (BCC)</Text>}
              extra={<Text style={{ fontSize: 12, color: '#9ca3af' }}>Enter 또는 쉼표로 구분</Text>}
            >
              <EmailTagInput placeholder="bcc@example.com" inputId="templated-email-bcc" />
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
          styles={{ body: { padding: 16 } }}
        >
          <TagListEditor name="templateData" addLabel="데이터 추가" />
        </Card>

        <Card
          size="small"
          title={
            <Space size={6}>
              <TagOutlined style={{ color: '#6b7280' }} />
              <Text style={{ fontSize: 14, fontWeight: 500 }}>태그 (선택사항)</Text>
              <Tooltip
                title={
                  <div style={{ fontSize: 12, lineHeight: 1.8 }}>
                    <strong>이메일 발송 목적을 분류하는 라벨</strong>입니다.<br />
                    이메일 발송 이벤트(오픈, 클릭, 반송 등) 발생 시 태그 기반으로 통계를 분석할 수 있습니다.<br /><br />
                    <strong>예시</strong><br />
                    <span style={{ fontFamily: 'monospace', background: 'rgba(255,255,255,0.15)', padding: '1px 4px', borderRadius: 3 }}>campaign: 2026-april-promo</span><br />
                    <span style={{ fontFamily: 'monospace', background: 'rgba(255,255,255,0.15)', padding: '1px 4px', borderRadius: 3 }}>type: marketing</span><br /><br />
                    위처럼 설정하면 캠페인별 오픈율, 클릭률 등을 추적할 수 있습니다.<br />
                    테스트 발송 시에는 비워도 무방합니다.
                  </div>
                }
                overlayStyle={{ maxWidth: 360 }}
              >
                <InfoCircleOutlined style={{ color: '#9ca3af', cursor: 'help' }} />
              </Tooltip>
            </Space>
          }
          style={{ marginBottom: 16, borderRadius: 8, border: '1px solid #e5e8ed' }}
          styles={{ body: { padding: 16 } }}
        >
          <TagListEditor name="tags" addLabel="태그 추가" />
        </Card>

        <Card
          size="small"
          style={{ marginBottom: 16, borderRadius: 8, border: '1px solid #e5e8ed' }}
          styles={{ body: { padding: 16 } }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <Text style={{ fontWeight: 500, fontSize: 14 }}>발송 시점</Text>
            <Radio.Group
              value={sendMode}
              onChange={(e) => {
                setSendMode(e.target.value);
                if (e.target.value === 'now') setScheduleDate(null);
              }}
            >
              <Radio value="now">
                <Space size={4}>
                  <SendOutlined style={{ color: '#1677ff' }} />
                  즉시 발송
                </Space>
              </Radio>
              <Radio value="schedule">
                <Space size={4}>
                  <ClockCircleOutlined style={{ color: '#722ed1' }} />
                  예약 발송
                </Space>
              </Radio>
            </Radio.Group>
            {sendMode === 'schedule' && (
              <div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 10 }}>
                  {[
                    { label: '현재시간', minutes: 0 },
                    { label: '10분 후', minutes: 10 },
                    { label: '30분 후', minutes: 30 },
                    { label: '1시간 후', minutes: 60 },
                    { label: '6시간 후', minutes: 360 },
                    { label: '12시간 후', minutes: 720 },
                  ].map((opt) => (
                    <Button
                      key={opt.label}
                      size="small"
                      type={scheduleDate && Math.abs(scheduleDate.diff(dayjs().add(opt.minutes, 'minute'), 'minute')) < 2 ? 'primary' : 'default'}
                      onClick={() => setScheduleDate(dayjs().add(opt.minutes, 'minute'))}
                      style={{ borderRadius: 6, fontSize: 12 }}
                    >
                      {opt.label}
                    </Button>
                  ))}
                </div>
                <DatePicker
                  showTime={{ format: 'HH:mm' }}
                  format="YYYY-MM-DD HH:mm"
                  placeholder="발송 날짜와 시간을 선택하세요"
                  value={scheduleDate}
                  onChange={(date) => setScheduleDate(date)}
                  disabledDate={(current) => current && current < dayjs().startOf('day')}
                  style={{ width: '100%', maxWidth: 300 }}
                  size="middle"
                />
                <Text style={{ fontSize: 12, color: '#9ca3af', display: 'block', marginTop: 4 }}>
                  현재 시간 이후만 선택 가능합니다.
                </Text>
              </div>
            )}
          </div>
        </Card>

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            icon={sendMode === 'schedule' ? <ClockCircleOutlined /> : <SendOutlined />}
            size="large"
            loading={isPending}
            disabled={isPending}
            style={{
              borderRadius: 8,
              minWidth: 140,
              ...(sendMode === 'schedule' ? { background: '#722ed1', borderColor: '#722ed1' } : {}),
            }}
          >
            {sendMode === 'schedule' ? '예약하기' : '발송하기'}
          </Button>
        </Form.Item>
      </Form>

      <ConfirmSendModal
        open={confirmOpen}
        from={pendingValues?.from ?? ''}
        to={pendingValues?.to ?? ''}
        subject={templateDetail?.subjectPart || `템플릿: ${pendingValues?.templateName ?? ''}`}
        scheduleTime={sendMode === 'schedule' ? scheduleTimeFormatted : undefined}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmOpen(false)}
        loading={isPending}
      />
      <SuccessModal
        open={resultOpen}
        messageId={messageId}
        scheduleTime={sendMode === 'schedule' ? scheduleTimeFormatted : undefined}
        onClose={() => {
          setResultOpen(false);
          setSelectedTenantId('');
          form.resetFields();
          setScheduleDate(null);
          setSendMode('now');
        }}
      />
    </>
  );
}

// ─── 이메일 발송 페이지 ────────────────────────────────────────────────────────
export default function SendEmailPage() {
  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="테스트 이메일 발송"
        subtitle="직접 이메일을 작성하거나 템플릿을 사용하여 발송 테스트를 합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '테스트 이메일 발송' }]}
      />

      <Card style={{ borderRadius: 12, border: '1px solid #e5e8ed' }} styles={{ body: { padding: 0 } }}>
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
