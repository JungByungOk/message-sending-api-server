import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import AuthGuard from '@/components/AuthGuard';
import LoginPage from '@/pages/auth/LoginPage';
import DashboardPage from '@/pages/dashboard/index';
import TenantList from '@/pages/tenant/TenantList';
import TenantCreate from '@/pages/tenant/TenantCreate';
import TenantDetail from '@/pages/tenant/TenantDetail';
import SendEmailPage from '@/pages/email/SendEmail';
import TemplateListPage from '@/pages/template/TemplateList';
import SchedulerPage from '@/pages/scheduler/SchedulerPage';
import SuppressionList from '@/pages/suppression/SuppressionList';
import SettingsPage from '@/pages/settings/index';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
      retry: 1,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <AuthGuard>
                <MainLayout />
              </AuthGuard>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="/tenant" element={<TenantList />} />
            <Route path="/tenant/create" element={<TenantCreate />} />
            <Route path="/tenant/:id" element={<TenantDetail />} />
            <Route path="/email/send" element={<SendEmailPage />} />
            <Route path="/template" element={<TemplateListPage />} />
            <Route path="/scheduler" element={<SchedulerPage />} />
            <Route path="/suppression" element={<SuppressionList />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
