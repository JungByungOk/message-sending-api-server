import { Button, Card, Col, Row, Statistic, Table, Tag, Timeline, Typography } from 'antd';
import {
  ArrowRightOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  MailOutlined,
  PlayCircleFilled,
  PlusOutlined,
  RocketOutlined,
  TeamOutlined,
  ThunderboltFilled,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { useTenants } from '@/hooks/useTenants';
import { useJobs } from '@/hooks/useScheduler';
import type { JobInfo } from '@/types/scheduler';
import StatusTag from '@/components/StatusTag';

const { Title, Text } = Typography;

// ─── 통계 카드 ────────────────────────────────────────────────────────────────
interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
  bgColor: string;
  suffix?: string;
}

function StatCard({ title, value, icon, color, bgColor, suffix }: StatCardProps) {
  return (
    <Card
      style={{
        borderRadius: 12,
        border: '1px solid #e5e8ed',
        overflow: 'hidden',
        position: 'relative',
      }}
      bodyStyle={{ padding: '20px 24px' }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <Text
            style={{ fontSize: 14, color: '#6b7280', fontWeight: 500, display: 'block', marginBottom: 8 }}
          >
            {title}
          </Text>
          <Statistic
            value={value}
            suffix={suffix}
            valueStyle={{ fontSize: 28, fontWeight: 700, color: '#111827', lineHeight: 1.2 }}
          />
        </div>
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: 10,
            background: bgColor,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 20,
            color,
            flexShrink: 0,
          }}
        >
          {icon}
        </div>
      </div>
    </Card>
  );
}

// ─── 최근 작업 컬럼 ───────────────────────────────────────────────────────────
const recentJobColumns = [
  {
    title: '작업 이름',
    dataIndex: 'jobName',
    key: 'jobName',
    ellipsis: true,
    render: (name: string) => (
      <Text style={{ fontWeight: 500, color: '#111827' }}>{name}</Text>
    ),
  },
  {
    title: '그룹',
    dataIndex: 'groupName',
    key: 'groupName',
    width: 140,
    render: (group: string) => (
      <Tag style={{ borderRadius: 6, fontSize: 12 }}>{group}</Tag>
    ),
  },
  {
    title: '상태',
    dataIndex: 'jobStatus',
    key: 'jobStatus',
    width: 110,
    render: (status: string) => <StatusTag type="job" status={status} />,
  },
  {
    title: '다음 실행',
    dataIndex: 'nextFireTime',
    key: 'nextFireTime',
    width: 170,
    render: (val: string | null) =>
      val ? (
        <Text style={{ fontSize: 13, color: '#6b7280' }}>
          {dayjs(val).format('MM/DD HH:mm')}
        </Text>
      ) : (
        <Text type="secondary" style={{ fontSize: 13 }}>-</Text>
      ),
  },
];

// ─── 대시보드 페이지 ──────────────────────────────────────────────────────────
export default function DashboardPage() {
  const navigate = useNavigate();
  const { data: tenantsData } = useTenants();
  const { data: jobsData } = useJobs();

  const activeTenantCount =
    tenantsData?.tenants.filter((t) => t.status === 'ACTIVE').length ?? 0;
  const totalTenantCount = tenantsData?.tenants.length ?? 0;
  const scheduledJobCount = jobsData?.numOfAllJobs ?? 0;
  const runningJobCount = jobsData?.numOfRunningJobs ?? 0;

  const recentJobs: JobInfo[] = jobsData?.jobs.slice(0, 5) ?? [];

  const now = dayjs();

  return (
    <div style={{ padding: 24 }}>
      {/* ─── 환영 배너 ─── */}
      <Card
        style={{
          marginBottom: 24,
          background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)',
          border: 'none',
          borderRadius: 16,
          overflow: 'hidden',
        }}
        bodyStyle={{ padding: '28px 32px' }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
              <ThunderboltFilled style={{ color: 'rgba(255,255,255,0.8)', fontSize: 18 }} />
              <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 14, fontWeight: 500 }}>
                Joins EMS 관리 콘솔
              </Text>
            </div>
            <Title
              level={2}
              style={{ color: '#ffffff', margin: 0, fontWeight: 700, letterSpacing: '-0.5px' }}
            >
              안녕하세요, 관리자님
            </Title>
            <Text style={{ color: 'rgba(255,255,255,0.75)', fontSize: 14, marginTop: 4, display: 'block' }}>
              {now.format('YYYY년 MM월 DD일 dddd')} · {now.format('HH:mm')} 현재
            </Text>
          </div>
          <RocketOutlined
            style={{ fontSize: 64, color: 'rgba(255,255,255,0.12)', flexShrink: 0 }}
          />
        </div>
      </Card>

      {/* ─── 통계 카드 행 ─── */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="활성 테넌트"
            value={activeTenantCount}
            icon={<TeamOutlined />}
            color="#12b76a"
            bgColor="rgba(18,183,106,0.1)"
            suffix={`/ ${totalTenantCount}`}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="실행 중 작업"
            value={runningJobCount}
            icon={<PlayCircleFilled />}
            color="#1677ff"
            bgColor="rgba(22,119,255,0.1)"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="전체 예약 작업"
            value={scheduledJobCount}
            icon={<CalendarOutlined />}
            color="#f79009"
            bgColor="rgba(247,144,9,0.1)"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="작업 그룹"
            value={jobsData?.numOfGroups ?? 0}
            icon={<ClockCircleOutlined />}
            color="#7c3aed"
            bgColor="rgba(124,58,237,0.1)"
          />
        </Col>
      </Row>

      {/* ─── 하단 두 컬럼 ─── */}
      <Row gutter={[16, 16]}>
        {/* 최근 작업 */}
        <Col xs={24} lg={16}>
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <ClockCircleOutlined style={{ color: '#1677ff' }} />
                <span style={{ fontWeight: 600 }}>최근 작업</span>
              </div>
            }
            extra={
              <Button
                type="link"
                size="small"
                icon={<ArrowRightOutlined />}
                onClick={() => navigate('/scheduler')}
                style={{ padding: 0, color: '#6b7280', fontSize: 14 }}
              >
                전체 보기
              </Button>
            }
            bodyStyle={{ padding: 0 }}
            style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
          >
            <Table<JobInfo>
              dataSource={recentJobs}
              columns={recentJobColumns}
              rowKey="jobName"
              pagination={false}
              size="small"
              style={{ borderRadius: 12, overflow: 'hidden' }}
              locale={{ emptyText: '등록된 작업이 없습니다.' }}
            />
          </Card>
        </Col>

        {/* 빠른 실행 + 최근 활동 */}
        <Col xs={24} lg={8}>
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <RocketOutlined style={{ color: '#7c3aed' }} />
                <span style={{ fontWeight: 600 }}>빠른 실행</span>
              </div>
            }
            style={{ marginBottom: 16, borderRadius: 12, border: '1px solid #e5e8ed' }}
            bodyStyle={{ padding: 16 }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                { label: '새 이메일 발송', path: '/email/send', color: '#1677ff', bg: 'rgba(22,119,255,0.08)', icon: <MailOutlined /> },
                { label: '테넌트 생성', path: '/tenant/create', color: '#12b76a', bg: 'rgba(18,183,106,0.08)', icon: <TeamOutlined /> },
                { label: '온보딩 시작', path: '/onboarding', color: '#7c3aed', bg: 'rgba(124,58,237,0.08)', icon: <PlusOutlined /> },
              ].map((item) => (
                <Button
                  key={item.path}
                  block
                  icon={item.icon}
                  onClick={() => navigate(item.path)}
                  style={{
                    textAlign: 'left',
                    height: 40,
                    borderRadius: 8,
                    border: 'none',
                    background: item.bg,
                    color: item.color,
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                  }}
                >
                  {item.label}
                </Button>
              ))}
            </div>
          </Card>

          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <CheckCircleFilled style={{ color: '#12b76a' }} />
                <span style={{ fontWeight: 600 }}>최근 활동</span>
              </div>
            }
            style={{ borderRadius: 12, border: '1px solid #e5e8ed' }}
            bodyStyle={{ padding: '16px 20px' }}
          >
            <Timeline
              style={{ marginTop: 8 }}
              items={
                recentJobs.length > 0
                  ? recentJobs.slice(0, 4).map((job) => ({
                      color:
                        job.jobStatus === 'RUNNING'
                          ? 'blue'
                          : job.jobStatus === 'COMPLETE'
                          ? 'green'
                          : job.jobStatus === 'PAUSED'
                          ? 'orange'
                          : 'gray',
                      children: (
                        <div>
                          <Text style={{ fontSize: 14, fontWeight: 500 }}>{job.jobName}</Text>
                          <br />
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {job.lastFiredTime
                              ? dayjs(job.lastFiredTime).format('MM/DD HH:mm')
                              : '실행 대기 중'}
                          </Text>
                        </div>
                      ),
                    }))
                  : [
                      {
                        color: 'gray',
                        children: (
                          <Text type="secondary" style={{ fontSize: 14 }}>
                            최근 활동이 없습니다.
                          </Text>
                        ),
                      },
                    ]
              }
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
