import axiosInstance from '@/api/axiosInstance'
import type {
  TaskDto,
  CreateTaskRequest,
  UpdateTaskRequest,
} from '../types/taskTypes'
import type { PageResponse } from '@/features/projects/types/projectTypes'

export const taskApi = {
  getAll: async (params: {
    projectId: number
    status?: string
    priority?: string
    search?: string
    page?: number
    size?: number
  }): Promise<PageResponse<TaskDto>> => {
    const response = await axiosInstance.get('/api/v1/tasks', { params })
    return response.data
  },

  getById: async (id: number): Promise<TaskDto> => {
    const response = await axiosInstance.get(`/api/v1/tasks/${id}`)
    return response.data
  },

  create: async (payload: CreateTaskRequest): Promise<TaskDto> => {
    const response = await axiosInstance.post('/api/v1/tasks', payload)
    return response.data
  },

  update: async (id: number, payload: UpdateTaskRequest): Promise<TaskDto> => {
    const response = await axiosInstance.put(`/api/v1/tasks/${id}`, payload)
    return response.data
  },

  delete: async (id: number): Promise<string> => {
    const response = await axiosInstance.delete(`/api/v1/tasks/${id}`)
    return response.data
  },
}

export default taskApi
