import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'

export const PublicRoute: React.FC = () => {
  const { isAuthenticated } = useAppSelector(selectAuth)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
