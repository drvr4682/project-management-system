import axiosInstance from '@/api/axiosInstance'
import type {
  ProjectMemberDto,
  AddMemberRequest,
  UserSummaryDto,
} from '../types/collaborationTypes'
import type { TaskDto } from '@/features/tasks/types/taskTypes'

export const collaborationApi = {
  getMembers: async (projectId: number): Promise<ProjectMemberDto[]> => {
    const response = await axiosInstance.get(`/api/v1/projects/${projectId}/members`)
    return response.data
  },

  addMember: async (projectId: number, payload: AddMemberRequest): Promise<string> => {
    const response = await axiosInstance.post(`/api/v1/projects/${projectId}/members`, payload)
    return response.data
  },

  removeMember: async (projectId: number, userId: string): Promise<string> => {
    const response = await axiosInstance.delete(`/api/v1/projects/${projectId}/members/${userId}`)
    return response.data
  },

  searchUsers: async (query: string): Promise<UserSummaryDto[]> => {
    const response = await axiosInstance.get('/api/v1/auth/users', {
      params: { query },
    })
    return response.data
  },

  assignTask: async (taskId: number, assigneeId: string): Promise<TaskDto> => {
    const response = await axiosInstance.put(`/api/v1/tasks/${taskId}/assign`, {
      assigneeId,
    })
    return response.data
  },

  unassignTask: async (taskId: number): Promise<TaskDto> => {
    const response = await axiosInstance.delete(`/api/v1/tasks/${taskId}/assign`)
    return response.data
  },
}

export default collaborationApi
