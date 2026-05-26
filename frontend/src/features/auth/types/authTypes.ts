export type UserRole = 'ADMIN' | 'USER'

export interface UserProfile {
  id: number
  name: string
  email: string
  role: UserRole
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export interface AuthState {
  user: UserProfile | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  loading: boolean
  error: string | null
  isHydrated: boolean
}

export interface LoginResponse {
  token: string
  refreshToken: string
  email: string
  role: string
  id?: number
  name?: string
}

export interface RegisterResponse {
  id: number
  name: string
  email: string
  role: string
}

export interface RefreshTokenResponse {
  token: string
  refreshToken: string
}
