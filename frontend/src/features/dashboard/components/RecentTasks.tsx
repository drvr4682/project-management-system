import React from 'react'
import { Link } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { ClipboardList, ChevronRight } from 'lucide-react'
import { TaskStatusBadge } from '@/features/tasks/components/TaskStatusBadge'
import { TaskPriorityBadge } from '@/features/tasks/components/TaskPriorityBadge'
import type { TaskDto } from '@/features/tasks/types/taskTypes'

interface RecentTasksProps {
  tasks: TaskDto[]
}

export const RecentTasks: React.FC<RecentTasksProps> = ({ tasks }) => {
  return (
    <Card className="border border-border/80 bg-card shadow-sm h-full flex flex-col justify-between">
      <CardHeader className="pb-3 border-b border-border/50">
        <div className="flex items-center space-x-2 text-primary font-bold mb-1">
          <ClipboardList size={14} />
          <span className="text-[10px] uppercase tracking-wider font-extrabold">Sprint Activity</span>
        </div>
        <CardTitle className="text-lg font-bold tracking-tight">Recent Tasks</CardTitle>
      </CardHeader>

      <CardContent className="pt-4 flex-1">
        {tasks.length === 0 ? (
          <div className="text-center py-8 text-xs text-muted-foreground font-semibold">
            No active deliverables found.
          </div>
        ) : (
          <div className="space-y-3.5">
            {tasks.map((task) => (
              <div
                key={task.id}
                className="flex items-center justify-between p-3.5 border border-border/80 rounded-xl bg-background/50 hover:border-primary/20 transition-all duration-300 group"
              >
                <div className="flex items-center space-x-3 min-w-0">
                  <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary shrink-0">
                    <ClipboardList size={16} />
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-bold text-foreground truncate group-hover:text-primary transition-colors">
                      {task.title}
                    </div>
                    <div className="flex items-center space-x-2 mt-1.5 flex-wrap gap-y-1">
                      <TaskStatusBadge status={task.status} />
                      <TaskPriorityBadge priority={task.priority} />
                    </div>
                  </div>
                </div>

                <Link
                  to={`/tasks/${task.id}`}
                  className="p-1 rounded-md text-muted-foreground group-hover:text-foreground group-hover:bg-muted transition-all duration-300 shrink-0"
                  title="View task details"
                >
                  <ChevronRight size={16} className="group-hover:translate-x-0.5 transition-transform" />
                </Link>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default RecentTasks
