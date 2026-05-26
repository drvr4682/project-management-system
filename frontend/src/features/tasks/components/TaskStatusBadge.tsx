import React from 'react'
import type { TaskStatus } from '../types/taskTypes'

interface TaskStatusBadgeProps {
  status: TaskStatus
}

export const TaskStatusBadge: React.FC<TaskStatusBadgeProps> = ({ status }) => {
  const getBadgeClass = () => {
    switch (status) {
      case 'DONE':
        return 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20'
      case 'IN_PROGRESS':
        return 'bg-blue-500/10 text-blue-500 border border-blue-500/20'
      case 'BLOCKED':
        return 'bg-rose-500/10 text-rose-500 border border-rose-500/20'
      case 'TODO':
      default:
        return 'bg-muted text-muted-foreground border border-border'
    }
  }

  const getStatusLabel = () => {
    if (status === 'IN_PROGRESS') return 'In Progress'
    return status.toLowerCase()
  }

  return (
    <span className={`text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded-full select-none ${getBadgeClass()}`}>
      {getStatusLabel()}
    </span>
  )
}

export default TaskStatusBadge
