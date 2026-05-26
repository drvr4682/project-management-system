import type { RouteObject } from 'react-router-dom'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { ProjectsPage } from '../pages/ProjectsPage'
import { CreateProjectPage } from '../pages/CreateProjectPage'
import { EditProjectPage } from '../pages/EditProjectPage'
import { ProjectDetailsPage } from '../pages/ProjectDetailsPage'
import { DashboardLayout } from '@/layouts/DashboardLayout'

export const projectRoutes: RouteObject[] = [
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          {
            path: '/projects',
            element: <ProjectsPage />,
          },
          {
            path: '/projects/create',
            element: <CreateProjectPage />,
          },
          {
            path: '/projects/:id',
            element: <ProjectDetailsPage />,
          },
          {
            path: '/projects/:id/edit',
            element: <EditProjectPage />,
          },
        ],
      },
    ],
  },
]

export default projectRoutes
