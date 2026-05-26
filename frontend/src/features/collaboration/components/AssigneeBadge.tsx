import React from 'react'
import { UserPlus } from 'lucide-react'
import UserAvatar from './UserAvatar'
import { cn } from '@/lib/utils'

interface AssigneeBadgeProps {
  assigneeId?: string | null // email or username
  onClick?: () => void
  interactive?: boolean
  className?: string
}

export const AssigneeBadge: React.FC<AssigneeBadgeProps> = ({
  assigneeId,
  onClick,
  interactive = false,
  className,
}) => {
  const isAssigned = !!assigneeId

  const content = isAssigned ? (
    <div className="flex items-center space-x-1.5 py-0.5 pl-0.5 pr-2 rounded-full bg-primary/5 border border-primary/10 text-xs font-bold text-foreground">
      <UserAvatar nameOrEmail={assigneeId} size="xs" />
      <span className="truncate max-w-[120px]">{assigneeId}</span>
    </div>
  ) : (
    <div className="flex items-center space-x-1.5 py-1 px-2.5 rounded-full border border-dashed border-muted-foreground/35 bg-transparent text-xs font-semibold text-muted-foreground group-hover:border-primary/45 group-hover:text-primary transition-all duration-300">
      <UserPlus size={12} className="text-muted-foreground/75 group-hover:text-primary transition-colors" />
      <span>Unassigned</span>
    </div>
  )

  if (interactive) {
    return (
      <button
        type="button"
        onClick={onClick}
        className={cn(
          'group focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-full transition-all duration-200 shrink-0 text-left',
          className
        )}
      >
        {content}
      </button>
    )
  }

  return (
    <div className={cn('shrink-0 select-none pointer-events-none', className)}>
      {content}
    </div>
  )
}

export default AssigneeBadge
