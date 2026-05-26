import type { RouteObject } from 'react-router-dom'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { DashboardPage } from '../pages/DashboardPage'

export const dashboardRoutes: RouteObject[] = [
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          {
            path: '/',
            element: <DashboardPage />,
          },
          {
            path: '/dashboard',
            element: <DashboardPage />,
          },
        ],
      },
    ],
  },
]

export default dashboardRoutes
