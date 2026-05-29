import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import type { UserProfileResponse } from '../api/profileApi'

export interface ProfileState {
  profileCompleted: boolean | null
  profileData: UserProfileResponse | null
  loading: boolean
  error: string | null
}

const initialState: ProfileState = {
  profileCompleted: null,
  profileData: null,
  loading: false,
  error: null,
}

const profileSlice = createSlice({
  name: 'profile',
  initialState,
  reducers: {
    setProfileData: (state, action: PayloadAction<UserProfileResponse>) => {
      state.profileData = action.payload
      state.profileCompleted = action.payload.profileCompleted
      state.error = null
    },
    setProfileCompleted: (state, action: PayloadAction<boolean>) => {
      state.profileCompleted = action.payload
      if (!action.payload) {
        state.profileData = null
      }
      state.error = null
    },
    setProfileLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload
    },
    setProfileError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload
    },
    clearProfileState: (state) => {
      state.profileCompleted = null
      state.profileData = null
      state.loading = false
      state.error = null
    },
  },
})

export const { setProfileData, setProfileCompleted, setProfileLoading, setProfileError, clearProfileState } = profileSlice.actions
export default profileSlice.reducer
export const selectProfile = (state: { profile: ProfileState }) => state.profile
