import React from 'react'
import type { TaskPriority } from '../types/taskTypes'

interface TaskPriorityBadgeProps {
  priority: TaskPriority
}

export const TaskPriorityBadge: React.FC<TaskPriorityBadgeProps> = ({ priority }) => {
  const getBadgeClass = () => {
    switch (priority) {
      case 'CRITICAL':
        return 'bg-red-500/10 text-red-500 border border-red-500/20'
      case 'HIGH':
        return 'bg-orange-500/10 text-orange-500 border border-orange-500/20'
      case 'MEDIUM':
        return 'bg-amber-500/10 text-amber-600 dark:text-amber-500 border border-amber-500/20'
      case 'LOW':
      default:
        return 'bg-muted text-muted-foreground border border-border'
    }
  }

  return (
    <span className={`text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded-full select-none ${getBadgeClass()}`}>
      {priority.toLowerCase()}
    </span>
  )
}

export default TaskPriorityBadge
