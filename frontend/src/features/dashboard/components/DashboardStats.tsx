import React from 'react'
import { Folder, ClipboardList, CheckCircle, Clock, AlertTriangle } from 'lucide-react'
import StatsCard from './StatsCard'
import type { DashboardStatsDto } from '../types/dashboardTypes'

interface DashboardStatsProps {
  stats: DashboardStatsDto
}

export const DashboardStats: React.FC<DashboardStatsProps> = ({ stats }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
      <StatsCard
        title="Total Workspaces"
        value={stats.totalProjects}
        icon={<Folder size={20} />}
        description="Active team workspaces"
        trendType="neutral"
      />
      <StatsCard
        title="Total Deliverables"
        value={stats.totalTasks}
        icon={<ClipboardList size={20} />}
        description="Global action items"
        trendType="neutral"
      />
      <StatsCard
        title="Completed Items"
        value={stats.completedTasks}
        icon={<CheckCircle size={20} />}
        description="Actions resolved successfully"
        trendType="positive"
      />
      <StatsCard
        title="Active Workload"
        value={stats.pendingTasks}
        icon={<Clock size={20} />}
        description="Tasks currently pending"
        trendType="neutral"
      />
      <StatsCard
        title="Overdue Alerts"
        value={stats.overdueTasks}
        icon={<AlertTriangle size={20} />}
        description="Deliverables past deadlines"
        trendType={stats.overdueTasks > 0 ? 'negative' : 'neutral'}
      />
    </div>
  )
}

export default DashboardStats
