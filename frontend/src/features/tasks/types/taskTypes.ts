export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface TaskDto {
  id: number
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueDate: number // milliseconds epoch
  projectId: number
  createdBy: string
  assignedTo: string | null
  createdAt: number // milliseconds epoch
  updatedAt: number // milliseconds epoch
}

export interface CreateTaskRequest {
  title: string
  description?: string
  status?: TaskStatus
  priority?: TaskPriority
  dueDate?: string // LocalDateTime format ISO string "YYYY-MM-DDTHH:mm:ss" or just "YYYY-MM-DD"
  projectId: number
}

export interface UpdateTaskRequest {
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string // ISO format string
  projectId: number
}
