import axiosInstance from '@/api/axiosInstance'

export interface ProjectResponseDTO {
  id: number
  name: string
  description: string
  owner: string
  status: string
  createdAt: number
  updatedAt: number
}

export interface ProjectMemberResponseDTO {
  userId: string
  role: string
  username?: string // Resolved on frontend if needed or displayed
}

export interface PageResponse<T> {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export const projectApi = {
  getProjects: async (params: {
    status?: string
    search?: string
    page?: number
    size?: number
    sortBy?: string
    direction?: string
  }): Promise<PageResponse<ProjectResponseDTO>> => {
    const response = await axiosInstance.get('/api/v1/projects', { params })
    return response.data
  },

  getProjectById: async (id: number): Promise<ProjectResponseDTO> => {
    const response = await axiosInstance.get(`/api/v1/projects/${id}`)
    return response.data
  },

  createProject: async (payload: { name: string; description: string; status?: string }): Promise<ProjectResponseDTO> => {
    const response = await axiosInstance.post('/api/v1/projects', payload)
    return response.data
  },

  updateProject: async (id: number, payload: { name: string; description: string; status?: string }): Promise<ProjectResponseDTO> => {
    const response = await axiosInstance.put(`/api/v1/projects/${id}`, payload)
    return response.data
  },

  deleteProject: async (id: number): Promise<string> => {
    const response = await axiosInstance.delete(`/api/v1/projects/${id}`)
    return response.data
  },

  getMembers: async (projectId: number): Promise<ProjectMemberResponseDTO[]> => {
    const response = await axiosInstance.get(`/api/v1/projects/${projectId}/members`)
    return response.data
  },

  addMember: async (projectId: number, payload: { userId: string; role: string }): Promise<string> => {
    const response = await axiosInstance.post(`/api/v1/projects/${projectId}/members`, payload)
    return response.data
  },

  removeMember: async (projectId: number, userId: string): Promise<string> => {
    const response = await axiosInstance.delete(`/api/v1/projects/${projectId}/members/${userId}`)
    return response.data
  },
}

export default projectApi
