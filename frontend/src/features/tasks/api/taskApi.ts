import axiosInstance from '@/api/axiosInstance'
import type { PageResponse } from '@/features/projects/api/projectApi'

export interface TaskResponseDTO {
  id: number
  title: string
  description: string
  status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  dueDate: number | null // timestamp
  projectId: number
  createdBy: string // UUID
  assignedTo: string | null // UUID
  createdAt: number
  updatedAt: number
}

export interface TaskRequestDTO {
  title: string
  description?: string
  status?: string
  priority?: string
  dueDate?: string | null // ISO string or null
  projectId: number
}

export const taskApi = {
  getTasks: async (params: {
    projectId: number
    status?: string
    priority?: string
    assignedTo?: string
    search?: string
    page?: number
    size?: number
    sortBy?: string
    direction?: string
  }): Promise<PageResponse<TaskResponseDTO>> => {
    const response = await axiosInstance.get('/api/v1/tasks', { params })
    return response.data
  },

  getTaskById: async (taskId: number): Promise<TaskResponseDTO> => {
    const response = await axiosInstance.get(`/api/v1/tasks/${taskId}`)
    return response.data
  },

  createTask: async (payload: TaskRequestDTO): Promise<TaskResponseDTO> => {
    const response = await axiosInstance.post('/api/v1/tasks', payload)
    return response.data
  },

  updateTask: async (taskId: number, payload: TaskRequestDTO): Promise<TaskResponseDTO> => {
    const response = await axiosInstance.put(`/api/v1/tasks/${taskId}`, payload)
    return response.data
  },

  deleteTask: async (taskId: number): Promise<string> => {
    const response = await axiosInstance.delete(`/api/v1/tasks/${taskId}`)
    return response.data
  },

  assignTask: async (taskId: number, assigneeId: string): Promise<TaskResponseDTO> => {
    const response = await axiosInstance.put(`/api/v1/tasks/${taskId}/assign`, { assigneeId })
    return response.data
  },

  removeAssignee: async (taskId: number): Promise<TaskResponseDTO> => {
    const response = await axiosInstance.delete(`/api/v1/tasks/${taskId}/assign`)
    return response.data
  },
}

export default taskApi
