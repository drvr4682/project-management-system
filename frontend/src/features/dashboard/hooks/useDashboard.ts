import { useState, useCallback } from 'react'
import { dashboardApi } from '../api/dashboardApi'
import type { DashboardData } from '../types/dashboardTypes'
import { toast } from 'sonner'

export const useDashboard = () => {
  const [data, setData] = useState<DashboardData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchDashboard = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const res = await dashboardApi.getDashboardData()
      setData(res)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to compile dashboard metrics.'
      setError(msg)
      toast.error(msg)
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    data,
    isLoading,
    error,
    fetchDashboard,
  }
}

export default useDashboard
