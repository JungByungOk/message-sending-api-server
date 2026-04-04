import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import {
  Button,
  Card,
  Col,
  Empty,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Typography,
} from 'antd';
import {
  ClockCircleOutlined,
  DeleteOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  StopOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  useDeleteAllJobs,
  useDeleteJob,
  useJobs,
  usePauseJob,
  useResumeJob,
  useStopJob,
} from '@/hooks/useScheduler';
import type { JobInfo } from '@/types/scheduler';
import StatusTag from '@/components/StatusTag';
import PageHeader from '@/components/PageHeader';

const { Text } = Typography;

const formatDateTime = (value: string | null): React.ReactNode => {
  if (!value) return <Text type="secondary" style={{ fontSize: 13 }}>-</Text>;
  return (
    <Text style={{ fontSize: 13, color: '#6b7280' }}>
      {dayjs(value).format('MM/DD HH:mm:ss')}
    </Text>
  );
};

export default function SchedulerPage() {
  const { data, isLoading } = useJobs();
  const { mutate: pauseJob, isPending: isPausing } = usePauseJob();
  const { mutate: resumeJob, isPending: isResuming } = useResumeJob();
  const { mutate: stopJob, isPending: isStopping } = useStopJob();
  const { mutate: deleteJob, isPending: isDeleting } = useDeleteJob();
  const { mutate: deleteAllJobs, isPending: isDeletingAll } = useDeleteAllJobs();

  const columns: ProColumns<JobInfo>[] = [
    {
      title: '작업명',
      dataIndex: 'jobName',
      ellipsis: true,
      render: (val) => (
        <Text style={{ fontWeight: 500, color: '#111827' }}>{val as string}</Text>
      ),
    },
    {
      title: '그룹',
      dataIndex: 'groupName',
      ellipsis: true,
      width: 130,
      render: (val) => (
        <Text
          style={{
            fontSize: 12,
            background: '#f8fafc',
            padding: '2px 8px',
            borderRadius: 4,
            border: '1px solid #e5e8ed',
            color: '#6b7280',
          }}
        >
          {val as string}
        </Text>
      ),
    },
    {
      title: '상태',
      dataIndex: 'jobStatus',
      width: 120,
      render: (_, record) => <StatusTag type="job" status={record.jobStatus} />,
    },
    {
      title: '예약 시간',
      dataIndex: 'scheduleTime',
      width: 150,
      render: (val) => formatDateTime(val as string),
    },
    {
      title: '마지막 실행',
      dataIndex: 'lastFiredTime',
      width: 150,
      render: (val) => formatDateTime(val as string | null),
    },
    {
      title: '다음 실행',
      dataIndex: 'nextFireTime',
      width: 150,
      render: (val) => formatDateTime(val as string | null),
    },
    {
      title: '액션',
      width: 160,
      render: (_, record) => {
        const { jobName, groupName, jobStatus } = record;
        const req = { jobName, jobGroup: groupName };

        return (
          <Space size={4} wrap>
            {jobStatus === 'SCHEDULED' && (
              <>
                <Popconfirm
                  title="작업을 일시 정지하시겠습니까?"
                  onConfirm={() => pauseJob(req)}
                  okText="확인"
                  cancelText="취소"
                >
                  <Button
                    type="text"
                    size="small"
                    icon={<PauseCircleOutlined />}
                    loading={isPausing}
                    style={{ color: '#f79009', borderRadius: 6 }}
                  >
                    정지
                  </Button>
                </Popconfirm>
                <Popconfirm
                  title="작업을 삭제하시겠습니까?"
                  onConfirm={() => deleteJob(req)}
                  okText="삭제"
                  cancelText="취소"
                  okButtonProps={{ danger: true }}
                >
                  <Button
                    type="text"
                    size="small"
                    icon={<DeleteOutlined />}
                    loading={isDeleting}
                    danger
                    style={{ borderRadius: 6 }}
                  >
                    삭제
                  </Button>
                </Popconfirm>
              </>
            )}

            {jobStatus === 'RUNNING' && (
              <Popconfirm
                title="실행 중인 작업을 중지하시겠습니까?"
                onConfirm={() => stopJob(req)}
                okText="중지"
                cancelText="취소"
                okButtonProps={{ danger: true }}
              >
                <Button
                  type="text"
                  size="small"
                  icon={<StopOutlined />}
                  loading={isStopping}
                  danger
                  style={{ borderRadius: 6 }}
                >
                  중지
                </Button>
              </Popconfirm>
            )}

            {jobStatus === 'PAUSED' && (
              <>
                <Popconfirm
                  title="작업을 재개하시겠습니까?"
                  onConfirm={() => resumeJob(req)}
                  okText="확인"
                  cancelText="취소"
                >
                  <Button
                    type="text"
                    size="small"
                    icon={<PlayCircleOutlined />}
                    loading={isResuming}
                    style={{ color: '#12b76a', borderRadius: 6 }}
                  >
                    재개
                  </Button>
                </Popconfirm>
                <Popconfirm
                  title="작업을 삭제하시겠습니까?"
                  onConfirm={() => deleteJob(req)}
                  okText="삭제"
                  cancelText="취소"
                  okButtonProps={{ danger: true }}
                >
                  <Button
                    type="text"
                    size="small"
                    icon={<DeleteOutlined />}
                    loading={isDeleting}
                    danger
                    style={{ borderRadius: 6 }}
                  >
                    삭제
                  </Button>
                </Popconfirm>
              </>
            )}

            {jobStatus === 'COMPLETE' && (
              <Popconfirm
                title="완료된 작업을 삭제하시겠습니까?"
                onConfirm={() => deleteJob(req)}
                okText="삭제"
                cancelText="취소"
                okButtonProps={{ danger: true }}
              >
                <Button
                  type="text"
                  size="small"
                  icon={<DeleteOutlined />}
                  loading={isDeleting}
                  danger
                  style={{ borderRadius: 6 }}
                >
                  삭제
                </Button>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  const hasJobs = (data?.jobs.length ?? 0) > 0;

  return (
    <div style={{ padding: 24 }}>
      <PageHeader
        title="스케줄러"
        subtitle="예약된 이메일 발송 작업을 관리합니다."
        breadcrumbs={[{ title: '홈', href: '/' }, { title: '스케줄러' }]}
      />

      {/* ─── 통계 카드 ─── */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          {
            title: '전체 작업',
            value: data?.numOfAllJobs ?? 0,
            icon: <ClockCircleOutlined />,
            color: '#374151',
            bg: '#f8fafc',
          },
          {
            title: '실행 중',
            value: data?.numOfRunningJobs ?? 0,
            icon: <PlayCircleOutlined />,
            color: '#1677ff',
            bg: 'rgba(22,119,255,0.08)',
          },
          {
            title: '작업 그룹',
            value: data?.numOfGroups ?? 0,
            icon: <ThunderboltOutlined />,
            color: '#7c3aed',
            bg: 'rgba(124,58,237,0.08)',
          },
        ].map((item) => (
          <Col xs={24} sm={8} key={item.title}>
            <Card
              style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
              bodyStyle={{ padding: '20px 24px' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <Text style={{ fontSize: 13, color: '#6b7280', fontWeight: 500, display: 'block', marginBottom: 6 }}>
                    {item.title}
                  </Text>
                  <Statistic
                    value={item.value}
                    valueStyle={{ fontSize: 26, fontWeight: 700, color: '#111827' }}
                  />
                </div>
                <div
                  style={{
                    width: 44,
                    height: 44,
                    borderRadius: 10,
                    background: item.bg,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 20,
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

      {/* ─── 작업 목록 ─── */}
      <Card
        bodyStyle={{ padding: 0 }}
        style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
      >
        <ProTable<JobInfo>
          headerTitle={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <ClockCircleOutlined style={{ color: '#1677ff' }} />
              <span style={{ fontWeight: 600 }}>작업 목록</span>
            </div>
          }
          rowKey={(record) => `${record.groupName}-${record.jobName}`}
          columns={columns}
          dataSource={data?.jobs ?? []}
          loading={isLoading}
          search={false}
          pagination={{ showSizeChanger: true, showTotal: (total) => `총 ${total}건` }}
          toolBarRender={() => [
            <Popconfirm
              key="delete-all"
              title="전체 작업 삭제"
              description="모든 작업을 삭제합니다. 이 작업은 되돌릴 수 없습니다."
              onConfirm={() => deleteAllJobs()}
              okText="전체 삭제"
              cancelText="취소"
              okButtonProps={{ danger: true }}
              disabled={!hasJobs}
            >
              <Button
                danger
                icon={<DeleteOutlined />}
                loading={isDeletingAll}
                disabled={!hasJobs}
                style={{ borderRadius: 8 }}
              >
                전체 삭제
              </Button>
            </Popconfirm>,
          ]}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <Text type="secondary" style={{ fontSize: 14 }}>
                    등록된 작업이 없습니다.
                  </Text>
                }
              />
            ),
          }}
        />
      </Card>
    </div>
  );
}
