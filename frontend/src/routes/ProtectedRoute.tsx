import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'
import type { UserRole } from '@/features/auth/types/authTypes'

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles }) => {
  const { isAuthenticated, user } = useAppSelector(selectAuth)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <Outlet />
}
