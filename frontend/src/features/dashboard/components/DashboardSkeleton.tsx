import React from 'react'

export const DashboardSkeleton: React.FC = () => {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Page Header Skeleton */}
      <div className="space-y-2">
        <div className="h-8 w-64 bg-muted rounded-xl" />
        <div className="h-4 w-96 bg-muted/70 rounded-lg" />
      </div>

      {/* 5 Stats Cards Grid Skeleton */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-28 border border-border/50 bg-card/60 rounded-2xl p-6 flex items-center justify-between">
            <div className="space-y-2.5 flex-1">
              <div className="h-3 w-20 bg-muted rounded" />
              <div className="h-7 w-12 bg-muted rounded-lg" />
              <div className="h-2 w-28 bg-muted/65 rounded" />
            </div>
            <div className="h-10 w-10 bg-muted/80 rounded-xl" />
          </div>
        ))}
      </div>

      {/* Main Content Grid Skeleton */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Productivity Analytics Column */}
        <div className="h-[360px] border border-border/50 bg-card/60 rounded-2xl p-6 space-y-6">
          <div className="space-y-2">
            <div className="h-3 w-16 bg-muted rounded" />
            <div className="h-5 w-36 bg-muted rounded-md" />
          </div>
          <div className="space-y-6 pt-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="space-y-2">
                <div className="flex justify-between">
                  <div className="h-3 w-28 bg-muted rounded" />
                  <div className="h-3 w-8 bg-muted rounded" />
                </div>
                <div className="h-2 w-full bg-muted rounded-full" />
              </div>
            ))}
          </div>
        </div>

        {/* Recent Workspace Column */}
        <div className="h-[360px] border border-border/50 bg-card/60 rounded-2xl p-6 space-y-4">
          <div className="space-y-2">
            <div className="h-3 w-16 bg-muted rounded" />
            <div className="h-5 w-32 bg-muted rounded-md" />
          </div>
          <div className="space-y-3 pt-2">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-14 border border-border/40 rounded-xl bg-muted/30" />
            ))}
          </div>
        </div>

        {/* Sprint Tasks Column */}
        <div className="h-[360px] border border-border/50 bg-card/60 rounded-2xl p-6 space-y-4">
          <div className="space-y-2">
            <div className="h-3 w-16 bg-muted rounded" />
            <div className="h-5 w-28 bg-muted rounded-md" />
          </div>
          <div className="space-y-3 pt-2">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-14 border border-border/40 rounded-xl bg-muted/30" />
            ))}
          </div>
        </div>
      </div>

      {/* Upcoming Deadlines (Bottom Full Row) Skeleton */}
      <div className="h-[360px] border border-border/50 bg-card/60 rounded-2xl p-6 space-y-4">
        <div className="space-y-2">
          <div className="h-3 w-16 bg-muted rounded" />
          <div className="h-5 w-40 bg-muted rounded-md" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-2">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-14 border border-border/40 rounded-xl bg-muted/30" />
          ))}
        </div>
      </div>
    </div>
  )
}

export default DashboardSkeleton
