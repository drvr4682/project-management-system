import axiosInstance from '@/api/axiosInstance'
import type {
  LoginResponse,
  RegisterResponse,
} from '../types/authTypes'

export const authApi = {
  login: async (payload: Record<string, string>): Promise<LoginResponse> => {
    const response = await axiosInstance.post('/api/v1/auth/login', payload)
    return response.data
  },

  register: async (payload: Record<string, string>): Promise<RegisterResponse> => {
    const response = await axiosInstance.post('/api/v1/auth/register', payload)
    return response.data
  },

  forgotPassword: async (email: string): Promise<{ message: string }> => {
    const response = await axiosInstance.post('/api/v1/auth/forgot-password', { email })
    return response.data
  },

  resetPassword: async (payload: Record<string, string>): Promise<{ message: string }> => {
    const response = await axiosInstance.post('/api/v1/auth/reset-password', payload)
    return response.data
  },

  verifyEmail: async (token: string): Promise<{ message: string }> => {
    const response = await axiosInstance.get(`/api/v1/auth/verify?token=${token}`)
    return response.data
  },

  resendVerification: async (email: string): Promise<{ message: string }> => {
    const response = await axiosInstance.post('/api/v1/auth/resend-verification', { email })
    return response.data
  },
}
export default authApi
