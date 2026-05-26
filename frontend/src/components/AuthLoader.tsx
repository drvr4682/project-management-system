import * as React from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { hydrate, selectAuth } from '@/features/auth/store/authSlice'

export interface AuthLoaderProps {
  children: React.ReactNode
}

export const AuthLoader: React.FC<AuthLoaderProps> = ({ children }) => {
  const dispatch = useAppDispatch()
  const { isHydrated } = useAppSelector(selectAuth)

  React.useEffect(() => {
    dispatch(hydrate())
  }, [dispatch])

  if (!isHydrated) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background text-foreground">
        <div className="flex flex-col items-center space-y-4">
          {/* Glassmorphic spinner */}
          <div className="relative w-16 h-16">
            <div className="absolute w-full h-full border-4 border-primary/20 rounded-full"></div>
            <div className="absolute w-full h-full border-4 border-t-primary rounded-full animate-spin"></div>
          </div>
          <p className="text-muted-foreground font-medium text-sm animate-pulse">
            Loading session...
          </p>
        </div>
      </div>
    )
  }

  return <>{children}</>
}
