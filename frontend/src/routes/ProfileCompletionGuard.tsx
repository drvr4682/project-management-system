import React, { useEffect } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { selectProfile, setProfileCompleted, setProfileLoading, setProfileError, setProfileData } from '@/features/auth/store/profileSlice'
import { profileApi } from '@/features/auth/api/profileApi'

export const ProfileCompletionGuard: React.FC = () => {
  const dispatch = useAppDispatch()
  const { profileCompleted, loading } = useAppSelector(selectProfile)

  useEffect(() => {
    if (profileCompleted === null && !loading) {
      const checkProfile = async () => {
        dispatch(setProfileLoading(true))
        try {
          const profile = await profileApi.getMyProfile()
          dispatch(setProfileData(profile))
        } catch (err: unknown) {
          dispatch(setProfileError('Failed to fetch profile onboarding status'))
          // Default to false if missing profile endpoint errors out
          dispatch(setProfileCompleted(false))
        } finally {
          dispatch(setProfileLoading(false))
        }
      }
      checkProfile()
    }
  }, [profileCompleted, loading, dispatch])

  if (profileCompleted === null || loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background text-foreground">
        <div className="flex flex-col items-center space-y-4">
          <div className="relative w-16 h-16">
            <div className="absolute w-full h-full border-4 border-primary/20 rounded-full"></div>
            <div className="absolute w-full h-full border-4 border-t-primary rounded-full animate-spin"></div>
          </div>
          <p className="text-muted-foreground font-medium text-sm animate-pulse">
            Verifying workspace profile...
          </p>
        </div>
      </div>
    )
  }

  if (!profileCompleted) {
    return <Navigate to="/complete-profile" replace />
  }

  return <Outlet />
}

export default ProfileCompletionGuard
