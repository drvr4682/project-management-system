import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'
import type { UserRole } from '@/features/auth/types/authTypes'

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles }) => {
  const { isAuthenticated, user, isHydrated } = useAppSelector(selectAuth)

  if (!isHydrated) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background text-foreground">
        <div className="flex flex-col items-center space-y-4">
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

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <Outlet />
}
