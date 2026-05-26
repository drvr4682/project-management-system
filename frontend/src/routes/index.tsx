import { createBrowserRouter } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicRoute } from './PublicRoute'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage'
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage'
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import { HomePage } from '@/pages/HomePage'
import { UnauthorizedPage } from '@/pages/UnauthorizedPage'
import { projectRoutes } from '@/features/projects/routes/projectRoutes'
import { taskRoutes } from '@/features/tasks/routes/taskRoutes'

export const router = createBrowserRouter([
  // Protected Routes
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <HomePage />,
      },
    ],
  },
  ...projectRoutes,
  ...taskRoutes,
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
        element: <LoginPage />,
      },
      {
        path: '/register',
        element: <RegisterPage />,
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
    element: <HomePage />, // ProtectedRoute in the hierarchy will auto-redirect unauthorized to login
  },
])
export default router
