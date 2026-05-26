import React from 'react'
import { Link, useParams } from 'react-router-dom'
import { ClipboardList, Plus } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export const EmptyTasksState: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>()

  return (
    <div className="flex flex-col items-center justify-center p-8 text-center bg-card/60 backdrop-blur-md rounded-xl border border-border/80 shadow-md py-16">
      <div className="mx-auto w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center text-primary mb-5">
        <ClipboardList size={32} />
      </div>
      <h3 className="text-2xl font-extrabold tracking-tight text-foreground mb-2">No Tasks Found</h3>
      <p className="text-muted-foreground max-w-md mb-6 font-medium">
        There are no tasks assigned to this workspace. Create a task to outline sprint milestones and deadlines.
      </p>
      <Link to={projectId ? `/tasks/create?projectId=${projectId}` : '/tasks/create'}>
        <Button className="flex items-center space-x-2">
          <Plus size={18} />
          <span>Create Task</span>
        </Button>
      </Link>
    </div>
  )
}

export default EmptyTasksState
