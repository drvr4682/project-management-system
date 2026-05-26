import { projectApi } from '@/features/projects/api/projectApi'
import { taskApi } from '@/features/tasks/api/taskApi'
import type { DashboardData } from '../types/dashboardTypes'
import type { TaskDto } from '@/features/tasks/types/taskTypes'

export const dashboardApi = {
  getDashboardData: async (): Promise<DashboardData> => {
    // 1. Fetch all user projects (max 100 for roster)
    const projectsResponse = await projectApi.getAll({ size: 100 })
    const projects = projectsResponse.content

    if (projects.length === 0) {
      return {
        stats: {
          totalProjects: 0,
          totalTasks: 0,
          completedTasks: 0,
          pendingTasks: 0,
          overdueTasks: 0,
          taskCompletionRate: 0,
          projectCompletionRatio: '0/0',
          pendingWorkloadSummary: 'Create your first project to get started!',
        },
        recentProjects: [],
        recentTasks: [],
        upcomingDeadlines: [],
      }
    }

    // 2. Fetch tasks for each project in parallel
    const taskPromises = projects.map((p) =>
      taskApi
        .getAll({ projectId: p.id, size: 100 })
        .then((res) => res.content)
        .catch(() => [] as TaskDto[])
    )
    const tasksNested = await Promise.all(taskPromises)
    const allTasks = tasksNested.flat()

    // 3. Compute stats
    const totalProjects = projects.length
    const totalTasks = allTasks.length
    const completedTasks = allTasks.filter((t) => t.status === 'DONE').length
    const pendingTasks = allTasks.filter((t) => t.status !== 'DONE').length

    const now = Date.now()
    const overdueTasks = allTasks.filter(
      (t) => t.status !== 'DONE' && t.dueDate && t.dueDate < now
    ).length

    const taskCompletionRate =
      totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0
    const completedProjects = projects.filter((p) => p.status === 'COMPLETED').length
    const projectCompletionRatio = `${completedProjects}/${totalProjects}`

    // Compute workload text summary
    let pendingWorkloadSummary = 'No active pending tasks. Excellent work!'
    if (pendingTasks > 0) {
      const criticalCount = allTasks.filter(
        (t) => t.status !== 'DONE' && t.priority === 'CRITICAL'
      ).length
      const highCount = allTasks.filter(
        (t) => t.status !== 'DONE' && t.priority === 'HIGH'
      ).length

      if (criticalCount > 0) {
        pendingWorkloadSummary = `${pendingTasks} pending tasks, including ${criticalCount} critical items.`
      } else if (highCount > 0) {
        pendingWorkloadSummary = `${pendingTasks} pending tasks, including ${highCount} high priorities.`
      } else {
        pendingWorkloadSummary = `${pendingTasks} active tasks to maintain project velocity.`
      }
    }

    // Sort and slice top 3 recent projects by updatedAt
    const recentProjects = [...projects]
      .sort((a, b) => b.updatedAt - a.updatedAt)
      .slice(0, 3)

    // Sort and slice top 5 recent tasks by updatedAt
    const recentTasks = [...allTasks]
      .sort((a, b) => b.updatedAt - a.updatedAt)
      .slice(0, 5)

    // Sort and slice top 5 upcoming deadlines (pending tasks with future due date)
    const upcomingDeadlines = allTasks
      .filter((t) => t.status !== 'DONE' && t.dueDate && t.dueDate >= now)
      .sort((a, b) => a.dueDate - b.dueDate)
      .slice(0, 5)

    return {
      stats: {
        totalProjects,
        totalTasks,
        completedTasks,
        pendingTasks,
        overdueTasks,
        taskCompletionRate,
        projectCompletionRatio,
        pendingWorkloadSummary,
      },
      recentProjects,
      recentTasks,
      upcomingDeadlines,
    }
  },
}

export default dashboardApi
