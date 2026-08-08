import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/providers/AuthProvider';
import { ProtectedRoute } from '@/routes/ProtectedRoute';
import { ToastContainer } from '@/components/ui/Toast';
import { LoginPage } from '@/pages/auth/LoginPage';
import { SignupPage } from '@/pages/auth/SignupPage';
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage';
import { OAuthRedirectPage } from '@/pages/auth/OAuthRedirectPage';
import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import { WorkspacePage } from '@/pages/workspace/WorkspacePage';
// 1. ADD THE IMPORT HERE
import { PlaybackPage } from '@/pages/playback/PlaybackPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastContainer />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/workspace/:workspaceId" element={<WorkspacePage />} />
            {/* 2. ADD THE ROUTE HERE */}
            <Route path="/dashboard/session/:sessionId/playback" element={<PlaybackPage />} />
          </Route>

          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}