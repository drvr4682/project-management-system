import React from 'react'
import { Card, CardContent } from '@/components/ui/Card'
import { cn } from '@/lib/utils'

interface StatsCardProps {
  title: string
  value: number | string
  icon: React.ReactNode
  description?: string
  trendType?: 'positive' | 'negative' | 'neutral'
  className?: string
}

export const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  icon,
  description,
  trendType = 'neutral',
  className,
}) => {
  const getTrendClass = () => {
    switch (trendType) {
      case 'positive':
        return 'text-emerald-500 bg-emerald-500/10'
      case 'negative':
        return 'text-rose-500 bg-rose-500/10'
      case 'neutral':
      default:
        return 'text-muted-foreground bg-muted'
    }
  }

  return (
    <Card className={cn('border border-border/80 bg-card shadow-sm hover:border-primary/20 transition-all duration-300 group', className)}>
      <CardContent className="p-6 flex items-center justify-between">
        <div className="space-y-1.5 min-w-0">
          <span className="text-xs uppercase font-extrabold tracking-wider text-muted-foreground block">
            {title}
          </span>
          <div className="text-3xl font-extrabold tracking-tight text-foreground truncate">
            {value}
          </div>
          {description && (
            <span className="text-[11px] text-muted-foreground font-semibold leading-none block truncate">
              {description}
            </span>
          )}
        </div>
        <div className={cn('p-3 rounded-xl shrink-0 flex items-center justify-center transition-all duration-300 group-hover:scale-105', getTrendClass())}>
          {icon}
        </div>
      </CardContent>
    </Card>
  )
}

export default StatsCard
