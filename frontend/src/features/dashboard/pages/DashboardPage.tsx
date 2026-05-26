import React, { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { useDashboard } from '../hooks/useDashboard'
import PageHeader from '@/components/ui/PageHeader'
import { Button } from '@/components/ui/Button'
import DashboardStats from '../components/DashboardStats'
import ProductivityOverview from '../components/ProductivityOverview'
import RecentProjects from '../components/RecentProjects'
import RecentTasks from '../components/RecentTasks'
import UpcomingDeadlines from '../components/UpcomingDeadlines'
import EmptyDashboardState from '../components/EmptyDashboardState'
import DashboardSkeleton from '../components/DashboardSkeleton'

export const DashboardPage: React.FC = () => {
  const { data, isLoading, error, fetchDashboard } = useDashboard()

  useEffect(() => {
    fetchDashboard()
  }, [fetchDashboard])

  if (isLoading) {
    return <DashboardSkeleton />
  }

  if (error) {
    return (
      <div className="space-y-6">
        <PageHeader title="Dashboard" subtitle="Overview of your workspace productivity metrics." />
        <div className="p-6 border border-rose-500/20 bg-rose-500/5 rounded-2xl text-center space-y-4">
          <p className="text-sm font-semibold text-rose-500">{error}</p>
          <Button onClick={fetchDashboard} className="font-bold rounded-xl">
            Retry Loading
          </Button>
        </div>
      </div>
    )
  }

  if (!data || data.stats.totalProjects === 0) {
    return <EmptyDashboardState />
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        subtitle="Workspace analytics, upcoming deadlines, and sprint metrics."
        actions={
          <Link to="/projects/create">
            <Button size="sm" className="rounded-xl font-bold flex items-center space-x-1.5 shadow-sm shadow-primary/10">
              <Plus size={14} />
              <span>New Project</span>
            </Button>
          </Link>
        }
      />

      {/* Grid of counter statistics */}
      <DashboardStats stats={data.stats} />

      {/* Analytics & Activity Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <ProductivityOverview stats={data.stats} />
        <RecentProjects projects={data.recentProjects} />
        <RecentTasks tasks={data.recentTasks} />
      </div>

      {/* Upcoming Deadlines */}
      <UpcomingDeadlines tasks={data.upcomingDeadlines} />
    </div>
  )
}

export default DashboardPage
