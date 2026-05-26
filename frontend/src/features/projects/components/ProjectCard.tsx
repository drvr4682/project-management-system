import React from 'react'
import { Link } from 'react-router-dom'
import { Edit2, Trash2, Calendar, User, ChevronRight } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import type { ProjectDto } from '../types/projectTypes'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'

interface ProjectCardProps {
  project: ProjectDto
  onDeleteClick: (project: ProjectDto) => void
}

export const ProjectCard: React.FC<ProjectCardProps> = ({ project, onDeleteClick }) => {
  const { user } = useAppSelector(selectAuth)
  const isAdmin = user?.role === 'ADMIN'

  // Format created timestamp to readable date
  const formatDate = (epochMillis: number) => {
    return new Date(epochMillis).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  // Get status badge colors
  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20'
      case 'ARCHIVED':
        return 'bg-muted text-muted-foreground border border-border'
      case 'ACTIVE':
      default:
        return 'bg-primary/10 text-primary border border-primary/20'
    }
  }

  return (
    <Card className="border border-border/80 bg-card hover:border-primary/30 transition-all duration-300 shadow-sm hover:shadow-md flex flex-col justify-between group h-full">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between space-x-2">
          <Link
            to={`/projects/${project.id}`}
            className="hover:text-primary transition-colors flex-1"
          >
            <CardTitle className="text-xl font-bold tracking-tight line-clamp-1">
              {project.name}
            </CardTitle>
          </Link>
          <span className={`text-xs px-2.5 py-1 rounded-full font-bold uppercase tracking-wider select-none ${getStatusBadgeClass(project.status)}`}>
            {project.status.toLowerCase()}
          </span>
        </div>
        <CardDescription className="line-clamp-2 min-h-[40px] mt-2 text-muted-foreground text-sm font-medium">
          {project.description || 'No description provided.'}
        </CardDescription>
      </CardHeader>

      <CardContent className="pb-3 text-xs text-muted-foreground space-y-2 mt-auto">
        <div className="flex items-center space-x-2 font-medium">
          <Calendar size={14} className="text-muted-foreground/80" />
          <span>Created: {formatDate(project.createdAt)}</span>
        </div>
        <div className="flex items-center space-x-2 font-medium">
          <User size={14} className="text-muted-foreground/80" />
          <span>Owner: {project.owner}</span>
        </div>
      </CardContent>

      <CardFooter className="pt-3 border-t border-border/50 flex items-center justify-between space-x-2">
        <Link to={`/projects/${project.id}`} className="inline-flex items-center text-xs text-primary font-bold hover:underline">
          View details
          <ChevronRight size={14} className="ml-0.5 group-hover:translate-x-0.5 transition-transform" />
        </Link>

        <div className="flex items-center space-x-1.5">
          <Link to={`/projects/${project.id}/edit`}>
            <Button
              variant="outline"
              size="sm"
              className="p-2 h-8 w-8 rounded-lg"
              title="Edit project"
            >
              <Edit2 size={13} />
            </Button>
          </Link>

          {isAdmin && (
            <Button
              variant="destructive"
              size="sm"
              className="p-2 h-8 w-8 rounded-lg"
              onClick={() => onDeleteClick(project)}
              title="Delete project"
            >
              <Trash2 size={13} />
            </Button>
          )}
        </div>
      </CardFooter>
    </Card>
  )
}

export default ProjectCard
