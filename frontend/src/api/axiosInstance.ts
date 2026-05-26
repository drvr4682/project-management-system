import axios, { type InternalAxiosRequestConfig } from 'axios'
import { store } from '@/app/store'
import { logout, setCredentials } from '@/features/auth/store/authSlice'
import { isTokenExpired } from '@/features/auth/utils/tokenUtils'
import type { UserProfile } from '@/features/auth/types/authTypes'

// Centralized Axios Instance mapped exclusively to the API Gateway
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Public Endpoints that MUST NEVER trigger refresh logic
const PUBLIC_WHITELIST = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/forgot-password',
  '/api/v1/auth/reset-password',
  '/api/v1/auth/verify',
  '/api/v1/auth/refresh',
  '/api/v1/auth/resend-verification',
]

function isPublicEndpoint(url?: string): boolean {
  if (!url) return false
  // Strip query params for checking
  const path = url.split('?')[0]
  return PUBLIC_WHITELIST.some((whitelistPath) => path.endsWith(whitelistPath))
}

// Global active refresh promise to deduplicate simultaneous requests
let refreshPromise: Promise<{ accessToken: string; refreshToken: string }> | null = null

async function executeTokenRefresh(currentRefreshToken: string) {
  try {
    const response = await axios.post(
      `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/v1/auth/refresh`,
      { refreshToken: currentRefreshToken }
    )
    const { token, refreshToken: newRefreshToken } = response.data
    return { accessToken: token, refreshToken: newRefreshToken }
  } catch (error) {
    store.dispatch(logout())
    return Promise.reject(error)
  }
}

// Request Interceptor: Proactive Expiration Check
axiosInstance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // 1. Skip check if the path is a public whitelist endpoint
    if (isPublicEndpoint(config.url)) {
      return config
    }

    const state = store.getState()
    let token = state.auth.accessToken || localStorage.getItem('accessToken')
    const currentRefreshToken = state.auth.refreshToken || localStorage.getItem('refreshToken')

    if (token) {
      // 2. Check if the access token is proactively expired
      if (isTokenExpired(token) && currentRefreshToken) {
        logMessage('[Network] Token proactively expired. Initiating silent refresh.')
        try {
          if (!refreshPromise) {
            refreshPromise = executeTokenRefresh(currentRefreshToken)
          }

          const { accessToken, refreshToken } = await refreshPromise
          refreshPromise = null

          // Safely parse user from storage to keep profiles hydrated
          const rawUser = localStorage.getItem('user')
          const user: UserProfile = rawUser ? JSON.parse(rawUser) : { id: 0, name: '', email: '', role: 'USER' }

          store.dispatch(setCredentials({ user, accessToken, refreshToken }))
          token = accessToken
        } catch (e) {
          refreshPromise = null
          logMessage('[Network] Proactive token refresh failed. User logged out.')
          return Promise.reject(e)
        }
      }

      // 3. Inject token as standard Bearer header
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor: Fallback 401 Retry Guard
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // 1. Process fallback token rotation ONLY on 401 errors, non-whitelisted calls, and only ONCE
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isPublicEndpoint(originalRequest.url)
    ) {
      logMessage('[Network] Request failed with 401. Triggering fallback token rotation.')
      originalRequest._retry = true

      const currentRefreshToken = store.getState().auth.refreshToken || localStorage.getItem('refreshToken')

      if (currentRefreshToken) {
        try {
          if (!refreshPromise) {
            refreshPromise = executeTokenRefresh(currentRefreshToken)
          }

          const { accessToken, refreshToken } = await refreshPromise
          refreshPromise = null

          const rawUser = localStorage.getItem('user')
          const user: UserProfile = rawUser ? JSON.parse(rawUser) : { id: 0, name: '', email: '', role: 'USER' }

          store.dispatch(setCredentials({ user, accessToken, refreshToken }))

          // Update headers and retry the original failed HTTP request
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return axiosInstance(originalRequest)
        } catch (refreshError) {
          refreshPromise = null
          logMessage('[Network] Fallback token rotation failed. Clearing session state.')
          return Promise.reject(refreshError)
        }
      }
    }

    return Promise.reject(error)
  }
)

function logMessage(msg: string) {
  // eslint-disable-next-line no-console
  console.log(msg)
}

export default axiosInstance
