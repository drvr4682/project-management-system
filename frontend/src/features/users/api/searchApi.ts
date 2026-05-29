import axiosInstance from '@/api/axiosInstance'

export interface UserSearchResponse {
  id: string
  firstName: string
  surname: string
  username: string
  designation?: string
  profileImageUrl?: string
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

export const searchApi = {
  searchProfiles: async (
    query: string,
    params?: {
      page?: number
      size?: number
      sortBy?: string
      direction?: string
    },
    signal?: AbortSignal
  ): Promise<PageResponse<UserSearchResponse>> => {
    const response = await axiosInstance.get('/api/v1/users/search', {
      params: {
        q: query,
        page: params?.page || 0,
        size: params?.size || 10,
        sortBy: params?.sortBy || 'firstName',
        direction: params?.direction || 'asc',
      },
      signal,
    })
    return response.data
  },
}

export default searchApi
