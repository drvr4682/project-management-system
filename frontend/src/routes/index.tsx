import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicRoute } from './PublicRoute'
import { ProfileCompletionGuard } from './ProfileCompletionGuard'
import { UnifiedAuthPage } from '@/features/auth/pages/UnifiedAuthPage'
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage'
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage'
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import { CompleteProfilePage } from '@/features/auth/pages/CompleteProfilePage'
import { AppShell } from '@/components/AppShell'
import { DashboardPage } from '@/pages/DashboardPage'
import { ProjectsPage } from '@/pages/ProjectsPage'
import { AllTasksPage } from '@/pages/AllTasksPage'
import { ProjectDetailPage } from '@/pages/ProjectDetailPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { UnauthorizedPage } from '@/pages/UnauthorizedPage'

export const router = createBrowserRouter([
  // Protected landing profile page
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <ProfileCompletionGuard />,
        children: [
          {
            element: <AppShell />,
            children: [
              {
                path: '/',
                element: <DashboardPage />,
              },
              {
                path: '/projects',
                element: <ProjectsPage />,
              },
              {
                path: '/projects/:projectId',
                element: <ProjectDetailPage />,
              },
              {
                path: '/tasks',
                element: <AllTasksPage />,
              },
              {
                path: '/profile',
                element: <ProfilePage />,
              },
            ],
          },
        ],
      },
      {
        path: '/complete-profile',
        element: <CompleteProfilePage />,
      },
    ],
  },
  {
    path: '/unauthorized',
    element: <UnauthorizedPage />,
  },
  // Public Routes (Only accessible when not logged in)
  {
    element: <PublicRoute />,
    children: [
      {
        path: '/login',
        element: <UnifiedAuthPage />,
      },
      {
        path: '/register',
        element: <UnifiedAuthPage />,
      },
      {
        path: '/forgot-password',
        element: <ForgotPasswordPage />,
      },
      {
        path: '/reset-password',
        element: <ResetPasswordPage />,
      },
    ],
  },
  // Public route open to trigger token email verification
  {
    path: '/verify-email',
    element: <VerifyEmailPage />,
  },
  // Catch-all
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
])
export default router
