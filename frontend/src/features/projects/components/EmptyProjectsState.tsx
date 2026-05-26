import React from 'react'
import { Link } from 'react-router-dom'
import { Plus, FolderOpen } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export const EmptyProjectsState: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-center p-8 text-center bg-card/60 backdrop-blur-md rounded-xl border border-border/80 shadow-md py-16">
      <div className="mx-auto w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center text-primary mb-5">
        <FolderOpen size={32} />
      </div>
      <h3 className="text-2xl font-extrabold tracking-tight text-foreground mb-2">No Projects Yet</h3>
      <p className="text-muted-foreground max-w-md mb-6 font-medium">
        Organize your tasks, workspaces, and teams by creating your first DRVRHub project.
      </p>
      <Link to="/projects/create">
        <Button className="flex items-center space-x-2">
          <Plus size={18} />
          <span>Create Project</span>
        </Button>
      </Link>
    </div>
  )
}

export default EmptyProjectsState
