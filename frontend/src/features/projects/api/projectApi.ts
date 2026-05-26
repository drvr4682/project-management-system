import axiosInstance from '@/api/axiosInstance'
import type {
  ProjectDto,
  CreateProjectRequest,
  UpdateProjectRequest,
  PageResponse,
} from '../types/projectTypes'

export const projectApi = {
  getAll: async (params?: {
    status?: string
    search?: string
    page?: number
    size?: number
  }): Promise<PageResponse<ProjectDto>> => {
    const response = await axiosInstance.get('/api/v1/projects', { params })
    return response.data
  },

  getById: async (id: number): Promise<ProjectDto> => {
    const response = await axiosInstance.get(`/api/v1/projects/${id}`)
    return response.data
  },

  create: async (payload: CreateProjectRequest): Promise<ProjectDto> => {
    const response = await axiosInstance.post('/api/v1/projects', payload)
    return response.data
  },

  update: async (id: number, payload: UpdateProjectRequest): Promise<ProjectDto> => {
    const response = await axiosInstance.put(`/api/v1/projects/${id}`, payload)
    return response.data
  },

  delete: async (id: number): Promise<string> => {
    const response = await axiosInstance.delete(`/api/v1/projects/${id}`)
    return response.data
  },
}

export default projectApi
