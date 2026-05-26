import type { RouteObject } from 'react-router-dom'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { ProjectMembersPage } from '../pages/ProjectMembersPage'
import { MyTasksPage } from '../pages/MyTasksPage'

export const collaborationRoutes: RouteObject[] = [
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          {
            path: '/projects/:projectId/members',
            element: <ProjectMembersPage />,
          },
          {
            path: '/my-tasks',
            element: <MyTasksPage />,
          },
        ],
      },
    ],
  },
]

export default collaborationRoutes
