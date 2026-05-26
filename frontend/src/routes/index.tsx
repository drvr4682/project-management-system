import { createBrowserRouter, Navigate } from 'react-router-dom'
import { PublicRoute } from './PublicRoute'
import { UnifiedAuthPage } from '@/features/auth/pages/UnifiedAuthPage'
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage'
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage'
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import { UnauthorizedPage } from '@/pages/UnauthorizedPage'
import { projectRoutes } from '@/features/projects/routes/projectRoutes'
import { taskRoutes } from '@/features/tasks/routes/taskRoutes'
import { collaborationRoutes } from '@/features/collaboration/routes/collaborationRoutes'
import { dashboardRoutes } from '@/features/dashboard/routes/dashboardRoutes'

export const router = createBrowserRouter([
  ...dashboardRoutes,
  ...projectRoutes,
  ...taskRoutes,
  ...collaborationRoutes,
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
