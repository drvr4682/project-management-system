import type { ProjectDto } from '@/features/projects/types/projectTypes'
import type { TaskDto } from '@/features/tasks/types/taskTypes'

export interface DashboardStatsDto {
  totalProjects: number
  totalTasks: number
  completedTasks: number
  pendingTasks: number
  overdueTasks: number
  taskCompletionRate: number // percentage (0 - 100)
  projectCompletionRatio: string // e.g. "3/10"
  pendingWorkloadSummary: string // description text
}

export interface DashboardData {
  stats: DashboardStatsDto
  recentProjects: ProjectDto[]
  recentTasks: TaskDto[]
  upcomingDeadlines: TaskDto[]
}
