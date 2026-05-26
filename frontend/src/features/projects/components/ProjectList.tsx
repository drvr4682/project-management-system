import React from 'react'
import ProjectCard from './ProjectCard'
import type { ProjectDto } from '../types/projectTypes'

interface ProjectListProps {
  projects: ProjectDto[]
  isLoading: boolean
  onDeleteClick: (project: ProjectDto) => void
}

export const ProjectList: React.FC<ProjectListProps> = ({
  projects,
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
            <div className="flex justify-between items-center">
              <div className="h-6 w-3/5 bg-muted rounded-md"></div>
              <div className="h-5 w-16 bg-muted rounded-full"></div>
            </div>
            <div className="space-y-2">
              <div className="h-4 w-full bg-muted rounded-md"></div>
              <div className="h-4 w-4/5 bg-muted rounded-md"></div>
            </div>
            <div className="pt-2 flex flex-col space-y-2 border-t border-border/40">
              <div className="h-3 w-1/3 bg-muted rounded-md"></div>
              <div className="h-3 w-1/4 bg-muted rounded-md"></div>
            </div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in duration-300">
      {projects.map((project) => (
        <div key={project.id} className="h-full">
          <ProjectCard project={project} onDeleteClick={onDeleteClick} />
        </div>
      ))}
    </div>
  )
}

export default ProjectList
