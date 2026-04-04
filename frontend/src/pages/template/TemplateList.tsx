import { useState } from 'react';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Space,
  Tabs,
  Typography,
  message,
} from 'antd';
import {
  CodeOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  useCreateTemplate,
  useDeleteTemplate,
  useTemplates,
  useUpdateTemplate,
} from '@/hooks/useEmail';
import type { EmailTemplate, TemplateMetadata } from '@/types/email';
import PageHeader from '@/components/PageHeader';

const { TextArea } = Input;
const { Text } = Typography;

type ModalMode = 'create' | 'edit';

interface TemplateFormValues {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart: string;
}

// ─── 템플릿 생성/수정 모달 ─────────────────────────────────────────────────────
function TemplateModal({
  open,
  mode,
  initialValues,
  onClose,
}: {
  open: boolean;
  mode: ModalMode;
  initialValues?: EmailTemplate;
  onClose: () => void;
}) {
  const [form] = Form.useForm<TemplateFormValues>();
  const [htmlPreview, setHtmlPreview] = useState(initialValues?.htmlPart ?? '');
  const [activeTab, setActiveTab] = useState<string>('editor');
  const { mutate: createTemplate, isPending: isCreating } = useCreateTemplate();
  const { mutate: updateTemplate, isPending: isUpdating } = useUpdateTemplate();

  const isPending = isCreating || isUpdating;

  const handleOk = () => {
    void form.validateFields().then((values) => {
      if (mode === 'create') {
        createTemplate(values, {
          onSuccess: () => {
            void message.success('템플릿이 생성되었습니다.');
            onClose();
          },
        });
      } else {
        updateTemplate(values, {
          onSuccess: () => {
            void message.success('템플릿이 수정되었습니다.');
            onClose();
          },
        });
      }
    });
  };

  const handleAfterOpenChange = (visible: boolean) => {
    if (visible) {
      if (initialValues) {
        form.setFieldsValue(initialValues);
        setHtmlPreview(initialValues.htmlPart);
      } else {
        form.resetFields();
        setHtmlPreview('');
      }
      setActiveTab('editor');
    }
  };

  const htmlValue = Form.useWatch('htmlPart', form) as string | undefined;

  return (
    <Modal
      title={
        <Space>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: mode === 'create' ? 'rgba(22,119,255,0.1)' : 'rgba(247,144,9,0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {mode === 'create' ? (
              <PlusOutlined style={{ color: '#1677ff' }} />
            ) : (
              <EditOutlined style={{ color: '#f79009' }} />
            )}
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 15 }}>
              {mode === 'create' ? '새 템플릿 생성' : '템플릿 수정'}
            </div>
            <div style={{ fontSize: 13, color: '#9ca3af', fontWeight: 400 }}>
              {mode === 'edit' && '기존 내용을 새로 입력하여 덮어씁니다.'}
            </div>
          </div>
        </Space>
      }
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      okText={mode === 'create' ? '생성' : '수정'}
      cancelText="취소"
      confirmLoading={isPending}
      width={960}
      afterOpenChange={handleAfterOpenChange}
      destroyOnClose
      bodyStyle={{ padding: '16px 24px' }}
    >
      {mode === 'edit' && (
        <Alert
          type="warning"
          showIcon
          message="수정 시 기존 HTML/텍스트 내용을 새로 입력하여 덮어씁니다."
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      <Row gutter={16}>
        <Col span={14}>
          <Form form={form} layout="vertical" requiredMark={false}>
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item
                  name="templateName"
                  label={<Text style={{ fontWeight: 500 }}>템플릿 이름</Text>}
                  rules={[{ required: true, message: '템플릿 이름을 입력하세요.' }]}
                >
                  <Input
                    placeholder="my-template"
                    disabled={mode === 'edit'}
                    prefix={<FileTextOutlined style={{ color: '#9ca3af' }} />}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="subjectPart"
                  label={<Text style={{ fontWeight: 500 }}>이메일 제목</Text>}
                  rules={[{ required: true, message: '제목을 입력하세요.' }]}
                >
                  <Input placeholder="이메일 제목" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="htmlPart"
              label={<Text style={{ fontWeight: 500 }}>HTML 본문</Text>}
              rules={[{ required: true, message: 'HTML 본문을 입력하세요.' }]}
            >
              <TextArea
                rows={10}
                placeholder="<html><body>...</body></html>"
                onChange={(e) => setHtmlPreview(e.target.value)}
                style={{ fontFamily: 'monospace', fontSize: 12 }}
              />
            </Form.Item>

            <Form.Item
              name="textPart"
              label={<Text style={{ fontWeight: 500 }}>텍스트 본문</Text>}
              rules={[{ required: true, message: '텍스트 본문을 입력하세요.' }]}
            >
              <TextArea rows={4} placeholder="일반 텍스트 본문 (HTML을 지원하지 않는 클라이언트용)" />
            </Form.Item>
          </Form>
        </Col>

        <Col span={10}>
          <div
            style={{
              position: 'sticky',
              top: 0,
            }}
          >
            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              size="small"
              items={[
                {
                  key: 'editor',
                  label: (
                    <Space size={4}>
                      <EyeOutlined />
                      HTML 미리보기
                    </Space>
                  ),
                  children: (
                    <div
                      style={{
                        border: '1px solid #e5e8ed',
                        borderRadius: 8,
                        padding: 16,
                        minHeight: 300,
                        background: '#fff',
                        overflow: 'auto',
                        maxHeight: 420,
                      }}
                    >
                      {(htmlValue || htmlPreview) ? (
                        <div
                          // eslint-disable-next-line react/no-danger
                          dangerouslySetInnerHTML={{ __html: htmlValue ?? htmlPreview }}
                        />
                      ) : (
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            height: 200,
                            color: '#9ca3af',
                            fontSize: 14,
                          }}
                        >
                          <div style={{ textAlign: 'center' }}>
                            <CodeOutlined style={{ fontSize: 32, marginBottom: 8 }} />
                            <div>HTML 본문을 입력하면 미리보기가 표시됩니다.</div>
                          </div>
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
            />
          </div>
        </Col>
      </Row>
    </Modal>
  );
}

// ─── 템플릿 목록 페이지 ────────────────────────────────────────────────────────
export default function TemplateListPage() {
  const { data: templates, isLoading } = useTemplates();
  const { mutate: deleteTemplate, isPending: isDeleting } = useDeleteTemplate();
  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<ModalMode>('create');
  const [editTarget, setEditTarget] = useState<EmailTemplate | undefined>();

  const handleCreate = () => {
    setModalMode('create');
    setEditTarget(undefined);
    setModalOpen(true);
  };

  const handleEdit = (record: TemplateMetadata) => {
    setModalMode('edit');
    setEditTarget({
      templateName: record.name,
      subjectPart: '',
      htmlPart: '',
      textPart: '',
    });
    setModalOpen(true);
  };

  const handleDelete = (name: string) => {
    deleteTemplate(
      { templateName: name },
      {
        onSuccess: () => {
          void message.success(`"${name}" 템플릿이 삭제되었습니다.`);
        },
      },
    );
  };

  const columns: ProColumns<TemplateMetadata>[] = [
    {
      title: '템플릿 이름',
      dataIndex: 'name',
      ellipsis: true,
      render: (val) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div
            style={{
              width: 28,
              height: 28,
              borderRadius: 6,
              background: 'rgba(22,119,255,0.08)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <FileTextOutlined style={{ color: '#1677ff', fontSize: 12 }} />
          </div>
          <Text style={{ fontWeight: 500, color: '#111827' }}>{val as string}</Text>
        </div>
      ),
    },
    {
      title: '생성일',
      dataIndex: 'createdTimestamp',
      width: 180,
      render: (val) => {
        const str = val as string;
        return str ? (
          <Text style={{ fontSize: 13, color: '#6b7280' }}>
            {dayjs(str).format('YYYY-MM-DD HH:mm:ss')}
          </Text>
        ) : (
          '-'
        );
      },
    },
    {
      title: '액션',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            style={{ color: '#6b7280', borderRadius: 6 }}
          >
            수정
          </Button>
          <Popconfirm
            title="템플릿 삭제"
            description={`"${record.name}" 템플릿을 삭제하시겠습니까?`}
            onConfirm={() => handleDelete(record.name)}
            okText="삭제"
            cancelText="취소"
            okButtonProps={{ danger: true }}
          >
            <Button
              type="text"
              size="small"
              icon={<DeleteOutlined />}
              danger
              loading={isDeleting}
              style={{ borderRadius: 6 }}
            >
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="템플릿 관리"
        subtitle="이메일 발송에 사용할 HTML 템플릿을 관리합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '템플릿 관리' }]}
        actions={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
            style={{ borderRadius: 8 }}
          >
            템플릿 생성
          </Button>
        }
      />

      <Card bodyStyle={{ padding: 0 }} style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}>
        <ProTable<TemplateMetadata>
          rowKey="name"
          columns={columns}
          dataSource={templates ?? []}
          loading={isLoading}
          search={false}
          toolBarRender={false}
          pagination={{ showSizeChanger: true, showTotal: (total) => `총 ${total}건` }}
          locale={{ emptyText: '등록된 템플릿이 없습니다.' }}
        />
      </Card>

      <TemplateModal
        open={modalOpen}
        mode={modalMode}
        initialValues={editTarget}
        onClose={() => setModalOpen(false)}
      />
    </div>
  );
}
