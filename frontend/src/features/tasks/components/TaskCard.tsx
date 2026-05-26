import React from 'react'
import { Link } from 'react-router-dom'
import { Edit2, Trash2, Calendar, ChevronRight } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { TaskStatusBadge } from './TaskStatusBadge'
import { TaskPriorityBadge } from './TaskPriorityBadge'
import AssigneeBadge from '@/features/collaboration/components/AssigneeBadge'
import type { TaskDto } from '../types/taskTypes'

interface TaskCardProps {
  task: TaskDto
  onDeleteClick: (task: TaskDto) => void
}

export const TaskCard: React.FC<TaskCardProps> = ({ task, onDeleteClick }) => {
  // Format due date to readable string
  const formatDueDate = (epochMillis: number) => {
    if (!epochMillis) return 'No due date'
    return new Date(epochMillis).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <Card className="border border-border/80 bg-card hover:border-primary/30 transition-all duration-300 shadow-sm hover:shadow-md flex flex-col justify-between group h-full">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between space-x-2">
          <Link
            to={`/tasks/${task.id}`}
            className="hover:text-primary transition-colors flex-1"
          >
            <CardTitle className="text-lg font-bold tracking-tight line-clamp-1">
              {task.title}
            </CardTitle>
          </Link>
          <div className="flex flex-col items-end space-y-1.5 shrink-0">
            <TaskStatusBadge status={task.status} />
            <TaskPriorityBadge priority={task.priority} />
          </div>
        </div>
        <CardDescription className="line-clamp-2 min-h-[36px] mt-2 text-muted-foreground text-xs font-medium">
          {task.description || 'No description provided.'}
        </CardDescription>
      </CardHeader>

      <CardContent className="pb-3 text-[11px] text-muted-foreground space-y-2 mt-auto">
        <div className="flex items-center space-x-2 font-medium">
          <Calendar size={13} className="text-muted-foreground/80" />
          <span>Due: {formatDueDate(task.dueDate)}</span>
        </div>
        <div className="flex items-center space-x-2">
          <span className="font-semibold text-[10px] text-muted-foreground/75 shrink-0 select-none">Assignee:</span>
          <AssigneeBadge assigneeId={task.assignedTo} />
        </div>
      </CardContent>

      <CardFooter className="pt-3 border-t border-border/50 flex items-center justify-between space-x-2">
        <Link
          to={`/tasks/${task.id}`}
          className="inline-flex items-center text-xs text-primary font-bold hover:underline"
        >
          <span>View details</span>
          <ChevronRight size={14} className="ml-0.5 group-hover:translate-x-0.5 transition-transform" />
        </Link>

        <div className="flex items-center space-x-1.5">
          <Link to={`/tasks/${task.id}/edit`}>
            <Button
              variant="outline"
              size="sm"
              className="p-2 h-8 w-8 rounded-lg"
              title="Edit task"
            >
              <Edit2 size={13} />
            </Button>
          </Link>

          <Button
            variant="destructive"
            size="sm"
            className="p-2 h-8 w-8 rounded-lg"
            onClick={() => onDeleteClick(task)}
            title="Delete task"
          >
            <Trash2 size={13} />
          </Button>
        </div>
      </CardFooter>
    </Card>
  )
}

export default TaskCard
