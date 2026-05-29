import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import type { AuthState, UserProfile } from '../types/authTypes'

const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  loading: false,
  error: null,
  isHydrated: false,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    hydrate: (state) => {
      try {
        const accessToken = localStorage.getItem('accessToken')
        const refreshToken = localStorage.getItem('refreshToken')
        const userJson = localStorage.getItem('user')

        if (accessToken && refreshToken && userJson) {
          // Hardened Token segment validation (accessToken must be a 3-segment JWT)
          if (accessToken.split('.').length !== 3 || !refreshToken) {
            throw new Error('Malformed token structure')
          }

          const user = JSON.parse(userJson) as UserProfile

          // Hardened UUID format validation
          const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
          if (!uuidRegex.test(user.id)) {
            throw new Error('Malformed UUID identity')
          }

          state.user = {
            id: user.id,
            userName: user.userName,
            email: user.email,
            role: user.role,
          }
          state.accessToken = accessToken
          state.refreshToken = refreshToken
          state.isAuthenticated = true
        }
      } catch {
        // Clear corrupt storage
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
      } finally {
        state.isHydrated = true
      }
    },
    setCredentials: (
      state,
      action: PayloadAction<{ user: UserProfile; accessToken: string; refreshToken: string }>
    ) => {
      const { user, accessToken, refreshToken } = action.payload
      const minimalUser: UserProfile = {
        id: user.id,
        userName: user.userName,
        email: user.email,
        role: user.role,
      }

      state.user = minimalUser
      state.accessToken = accessToken
      state.refreshToken = refreshToken
      state.isAuthenticated = true
      state.error = null

      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      localStorage.setItem('user', JSON.stringify(minimalUser))
    },
    logout: (state) => {
      state.user = null
      state.accessToken = null
      state.refreshToken = null
      state.isAuthenticated = false
      state.error = null

      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload
    },
  },
})

export const { hydrate, setCredentials, logout, setLoading, setError } = authSlice.actions
export default authSlice.reducer
export const selectAuth = (state: { auth: AuthState }) => state.auth
