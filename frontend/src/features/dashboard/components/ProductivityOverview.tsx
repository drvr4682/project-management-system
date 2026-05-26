import React from 'react'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { Sparkles, CheckSquare, Target, Activity } from 'lucide-react'
import type { DashboardStatsDto } from '../types/dashboardTypes'

interface ProductivityOverviewProps {
  stats: DashboardStatsDto
}

export const ProductivityOverview: React.FC<ProductivityOverviewProps> = ({ stats }) => {
  return (
    <Card className="border border-border/80 bg-card shadow-sm h-full flex flex-col justify-between">
      <CardHeader className="pb-3 border-b border-border/50">
        <div className="flex items-center space-x-2 text-primary font-bold mb-1">
          <Sparkles size={14} />
          <span className="text-[10px] uppercase tracking-wider font-extrabold">Analytics Core</span>
        </div>
        <CardTitle className="text-lg font-bold tracking-tight">Productivity Engine</CardTitle>
      </CardHeader>

      <CardContent className="pt-4 flex-1 space-y-6">
        {/* Task Completion Rate (Progress Bar) */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-foreground">
            <span className="flex items-center space-x-1.5">
              <CheckSquare size={14} className="text-muted-foreground/80" />
              <span>Task Completion Rate</span>
            </span>
            <span className="text-primary">{stats.taskCompletionRate}%</span>
          </div>
          <div className="w-full h-2 rounded-full bg-muted overflow-hidden">
            <div
              className="h-full bg-primary rounded-full transition-all duration-500 ease-out"
              style={{ width: `${stats.taskCompletionRate}%` }}
            ></div>
          </div>
        </div>

        {/* Project Completion Ratio */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-bold text-foreground">
            <span className="flex items-center space-x-1.5">
              <Target size={14} className="text-muted-foreground/80" />
              <span>Project Completion Ratio</span>
            </span>
            <span className="px-2 py-0.5 rounded-md bg-secondary text-secondary-foreground">
              {stats.projectCompletionRatio}
            </span>
          </div>
          <p className="text-[10px] text-muted-foreground font-semibold">
            Ratios of completed team workspaces against overall active projects.
          </p>
        </div>

        {/* Active Workload Summary */}
        <div className="space-y-2 border-t border-border/50 pt-4">
          <div className="flex items-center space-x-1.5 text-xs font-bold text-foreground">
            <Activity size={14} className="text-muted-foreground/80" />
            <span>Active Workload Digest</span>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed font-semibold">
            {stats.pendingWorkloadSummary}
          </p>
        </div>
      </CardContent>
    </Card>
  )
}

export default ProductivityOverview
