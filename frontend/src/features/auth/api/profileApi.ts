import axiosInstance from '@/api/axiosInstance'

export interface SocialLinkResponse {
  id: string
  profileId: string
  platform: string
  url: string
}

export interface UserProfileResponse {
  id?: string
  firstName?: string
  surname?: string
  username?: string
  bio?: string
  profileImageUrl?: string
  designation?: string
  timezone?: string
  statusMessage?: string
  active?: boolean
  profileCompleted: boolean
  socialLinks?: SocialLinkResponse[]
  createdAt?: string
  updatedAt?: string
}

export const profileApi = {
  getMyProfile: async (): Promise<UserProfileResponse> => {
    const response = await axiosInstance.get('/api/v1/users/me')
    return response.data
  },

  completeProfile: async (payload: { firstName: string; surname?: string }): Promise<UserProfileResponse> => {
    const response = await axiosInstance.post('/api/v1/users/me', payload)
    return response.data
  },

  updateProfile: async (payload: Partial<UserProfileResponse>): Promise<UserProfileResponse> => {
    const response = await axiosInstance.put('/api/v1/users/me', payload)
    return response.data
  },

  getProfileById: async (id: string): Promise<UserProfileResponse> => {
    const response = await axiosInstance.get(`/api/v1/users/${id}`)
    return response.data
  },

  addSocialLink: async (payload: { platform: string; url: string }): Promise<any> => {
    const response = await axiosInstance.post('/api/v1/social-links', payload)
    return response.data
  },

  deleteSocialLink: async (id: string): Promise<void> => {
    await axiosInstance.delete(`/api/v1/social-links/${id}`)
  },
}

export default profileApi
