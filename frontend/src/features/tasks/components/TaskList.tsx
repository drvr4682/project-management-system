import React from 'react'
import TaskCard from './TaskCard'
import type { TaskDto } from '../types/taskTypes'

interface TaskListProps {
  tasks: TaskDto[]
  isLoading: boolean
  onDeleteClick: (task: TaskDto) => void
}

export const TaskList: React.FC<TaskListProps> = ({
  tasks,
  isLoading,
  onDeleteClick,
}) => {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {[...Array(6)].map((_, i) => (
          <div
            key={i}
            className="border border-border/70 rounded-xl p-5 space-y-4 bg-card/60 backdrop-blur-md animate-pulse"
          >
            <div className="flex justify-between items-start">
              <div className="h-5 w-1/2 bg-muted rounded-md"></div>
              <div className="space-y-1.5 flex flex-col items-end">
                <div className="h-4 w-12 bg-muted rounded-full"></div>
                <div className="h-4 w-10 bg-muted rounded-full"></div>
              </div>
            </div>
            <div className="space-y-2">
              <div className="h-3 w-full bg-muted rounded-md"></div>
              <div className="h-3 w-4/5 bg-muted rounded-md"></div>
            </div>
            <div className="pt-2 flex flex-col space-y-1.5 border-t border-border/40">
              <div className="h-3 w-1/3 bg-muted rounded-md"></div>
            </div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in duration-300">
      {tasks.map((task) => (
        <div key={task.id} className="h-full">
          <TaskCard task={task} onDeleteClick={onDeleteClick} />
        </div>
      ))}
    </div>
  )
}

export default TaskList
