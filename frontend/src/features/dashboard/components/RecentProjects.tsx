import React from 'react'
import { Link } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { Folder, ChevronRight, Activity } from 'lucide-react'
import type { ProjectDto } from '@/features/projects/types/projectTypes'

interface RecentProjectsProps {
  projects: ProjectDto[]
}

export const RecentProjects: React.FC<RecentProjectsProps> = ({ projects }) => {
  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-500/10 text-emerald-500'
      case 'ARCHIVED':
        return 'bg-muted text-muted-foreground'
      case 'ACTIVE':
      default:
        return 'bg-primary/10 text-primary'
    }
  }

  return (
    <Card className="border border-border/80 bg-card shadow-sm h-full flex flex-col justify-between">
      <CardHeader className="pb-3 border-b border-border/50">
        <div className="flex items-center space-x-2 text-primary font-bold mb-1">
          <Activity size={14} />
          <span className="text-[10px] uppercase tracking-wider font-extrabold">Active Areas</span>
        </div>
        <CardTitle className="text-lg font-bold tracking-tight">Recent Projects</CardTitle>
      </CardHeader>

      <CardContent className="pt-4 flex-1">
        {projects.length === 0 ? (
          <div className="text-center py-8 text-xs text-muted-foreground font-semibold">
            No projects found. Create one to begin collaborating!
          </div>
        ) : (
          <div className="space-y-3.5">
            {projects.map((project) => (
              <div
                key={project.id}
                className="flex items-center justify-between p-3.5 border border-border/80 rounded-xl bg-background/50 hover:border-primary/20 transition-all duration-300 group"
              >
                <div className="flex items-center space-x-3 min-w-0">
                  <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary shrink-0">
                    <Folder size={16} />
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-bold text-foreground truncate group-hover:text-primary transition-colors">
                      {project.name}
                    </div>
                    <div className="flex items-center space-x-1.5 mt-1">
                      <span
                        className={`text-[9px] font-extrabold uppercase tracking-wider px-1.5 py-0.5 rounded-md ${getStatusBadgeClass(
                          project.status
                        )}`}
                      >
                        {project.status.toLowerCase()}
                      </span>
                    </div>
                  </div>
                </div>

                <Link
                  to={`/projects/${project.id}`}
                  className="p-1 rounded-md text-muted-foreground group-hover:text-foreground group-hover:bg-muted transition-all duration-300"
                  title="View workspace"
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

export default RecentProjects
