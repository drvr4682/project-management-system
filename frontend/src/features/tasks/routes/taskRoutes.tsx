import type { RouteObject } from 'react-router-dom'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { TasksPage } from '../pages/TasksPage'
import { CreateTaskPage } from '../pages/CreateTaskPage'
import { EditTaskPage } from '../pages/EditTaskPage'
import { TaskDetailsPage } from '../pages/TaskDetailsPage'
import { DashboardLayout } from '@/layouts/DashboardLayout'

export const taskRoutes: RouteObject[] = [
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          {
            path: '/projects/:projectId/tasks',
            element: <TasksPage />,
          },
          {
            path: '/tasks/create',
            element: <CreateTaskPage />,
          },
          {
            path: '/tasks/:id',
            element: <TaskDetailsPage />,
          },
          {
            path: '/tasks/:id/edit',
            element: <EditTaskPage />,
          },
        ],
      },
    ],
  },
]

export default taskRoutes
