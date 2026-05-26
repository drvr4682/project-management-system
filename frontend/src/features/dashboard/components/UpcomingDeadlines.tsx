import React from 'react'
import { Link } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { Calendar, ChevronRight, AlertCircle, Clock } from 'lucide-react'
import type { TaskDto } from '@/features/tasks/types/taskTypes'

interface UpcomingDeadlinesProps {
  tasks: TaskDto[]
}

export const UpcomingDeadlines: React.FC<UpcomingDeadlinesProps> = ({ tasks }) => {
  const formatDueDate = (epochMillis: number) => {
    if (!epochMillis) return 'No due date'
    return new Date(epochMillis).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  const getDeadlineStyle = (epochMillis: number) => {
    const diff = epochMillis - Date.now()
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24))

    if (days <= 2) {
      return {
        badge: 'bg-rose-500/10 text-rose-500 border border-rose-500/20',
        dot: 'bg-rose-500',
        text: 'Urgent',
        daysLabel: days === 0 ? 'Due today' : days === 1 ? 'Due tomorrow' : `${days} days left`,
      }
    } else if (days <= 5) {
      return {
        badge: 'bg-amber-500/10 text-amber-600 dark:text-amber-500 border border-amber-500/20',
        dot: 'bg-amber-500',
        text: 'Medium',
        daysLabel: `${days} days left`,
      }
    } else {
      return {
        badge: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20',
        dot: 'bg-emerald-500',
        text: 'Normal',
        daysLabel: `${days} days left`,
      }
    }
  }

  return (
    <Card className="border border-border/80 bg-card shadow-sm h-full flex flex-col justify-between">
      <CardHeader className="pb-3 border-b border-border/50">
        <div className="flex items-center space-x-2 text-primary font-bold mb-1">
          <Calendar size={14} />
          <span className="text-[10px] uppercase tracking-wider font-extrabold">Milestones</span>
        </div>
        <CardTitle className="text-lg font-bold tracking-tight">Upcoming Deadlines</CardTitle>
      </CardHeader>

      <CardContent className="pt-4 flex-1">
        {tasks.length === 0 ? (
          <div className="text-center py-8 text-xs text-muted-foreground font-semibold flex flex-col items-center justify-center space-y-1">
            <AlertCircle size={20} className="text-muted-foreground/60 mb-1" />
            <span>No upcoming deadlines.</span>
            <span className="text-[10px] font-normal">All clear for active sprints!</span>
          </div>
        ) : (
          <div className="space-y-3.5">
            {tasks.map((task) => {
              const style = getDeadlineStyle(task.dueDate)
              return (
                <div
                  key={task.id}
                  className="flex items-center justify-between p-3.5 border border-border/80 rounded-xl bg-background/50 hover:border-primary/20 transition-all duration-300 group"
                >
                  <div className="flex items-center space-x-3 min-w-0">
                    <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary shrink-0 relative">
                      <Clock size={16} />
                      <span className={`absolute -top-0.5 -right-0.5 h-2 w-2 rounded-full border border-card ${style.dot}`} />
                    </div>
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-foreground truncate group-hover:text-primary transition-colors">
                        {task.title}
                      </div>
                      <div className="flex items-center space-x-2 mt-1">
                        <span className="text-[10px] text-muted-foreground font-semibold flex items-center space-x-1">
                          <span>{formatDueDate(task.dueDate)}</span>
                        </span>
                        <span className={`text-[9px] font-extrabold px-1.5 py-0.5 rounded-md ${style.badge}`}>
                          {style.daysLabel}
                        </span>
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
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default UpcomingDeadlines
