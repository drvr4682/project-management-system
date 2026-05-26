import React, { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, Calendar, User, FileText, LayoutList, Users, ClipboardList, Trash2, Edit2 } from 'lucide-react'
import { toast } from 'sonner'
import { projectApi } from '../api/projectApi'
import { useProjects } from '../hooks/useProjects'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'
import { DeleteProjectDialog } from '../components/DeleteProjectDialog'
import type { ProjectDto } from '../types/projectTypes'

export const ProjectDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const projectId = id ? parseInt(id, 10) : NaN
  const navigate = useNavigate()

  const { deleteProject } = useProjects()
  const { user } = useAppSelector(selectAuth)
  const isAdmin = user?.role === 'ADMIN'

  const [project, setProject] = useState<ProjectDto | null>(null)
  const [isFetching, setIsFetching] = useState(true)
  const [fetchError, setFetchError] = useState<string | null>(null)
  
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [activeTab, setActiveTab] = useState<'tasks' | 'members' | 'activity'>('tasks')

  // Fetch project details on mount
  useEffect(() => {
    if (isNaN(projectId)) {
      setFetchError('Invalid project ID')
      setIsFetching(false)
      return
    }

    const fetchDetails = async () => {
      try {
        const fetched = await projectApi.getById(projectId)
        setProject(fetched)
      } catch (e: any) {
        const msg = e.response?.data?.message || 'Failed to fetch project details.'
        setFetchError(msg)
        toast.error(msg)
      } finally {
        setIsFetching(false)
      }
    }

    fetchDetails()
  }, [projectId])

  const handleDeleteConfirm = async () => {
    if (isNaN(projectId)) return
    setIsDeleting(true)
    try {
      await deleteProject(projectId)
      toast.success('Project deleted successfully.')
      navigate('/projects')
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to delete project.'
      toast.error(msg)
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }

  const formatDate = (epochMillis: number) => {
    return new Date(epochMillis).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  }

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
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Back button and quick actions */}
      <div className="flex items-center justify-between">
        <Link
          to="/projects"
          className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group"
        >
          <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
          <span>Back to Projects</span>
        </Link>

        {project && (
          <div className="flex items-center space-x-2">
            <Link to={`/projects/${project.id}/edit`}>
              <Button variant="outline" size="sm" className="flex items-center space-x-1.5 h-9">
                <Edit2 size={14} />
                <span className="hidden sm:inline">Edit Project</span>
              </Button>
            </Link>

            {isAdmin && (
              <Button
                variant="destructive"
                size="sm"
                className="flex items-center space-x-1.5 h-9"
                onClick={() => setShowDeleteDialog(true)}
              >
                <Trash2 size={14} />
                <span className="hidden sm:inline">Delete</span>
              </Button>
            )}
          </div>
        )}
      </div>

      {isFetching ? (
        // Premium Pulse Loading Skeletons
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-pulse">
          <div className="lg:col-span-2 space-y-6">
            <div className="h-40 bg-card border border-border rounded-xl"></div>
            <div className="h-64 bg-card border border-border rounded-xl"></div>
          </div>
          <div className="h-64 bg-card border border-border rounded-xl"></div>
        </div>
      ) : (
        <>
          {fetchError && !project && (
            <Card className="border-destructive/20 bg-destructive/5 text-destructive p-6 text-center max-w-xl mx-auto shadow-md">
              <div className="font-bold text-lg mb-2">Failed to Load Project</div>
              <p className="text-sm font-medium text-destructive/80 mb-4">{fetchError}</p>
              <Link to="/projects">
                <Button variant="outline" size="sm">Return to Projects List</Button>
              </Link>
            </Card>
          )}

          {project && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
              {/* Left Column: Details + Placeholders */}
              <div className="lg:col-span-2 space-y-6">
                {/* Project Header card */}
                <Card className="border border-border/80 bg-card shadow-sm">
                  <CardHeader className="space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`text-xs px-2.5 py-0.5 rounded-full font-bold uppercase tracking-wider ${getStatusBadgeClass(project.status)}`}>
                        {project.status.toLowerCase()}
                      </span>
                    </div>
                    <CardTitle className="text-3xl font-extrabold tracking-tight text-foreground">
                      {project.name}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center space-x-2 text-primary font-semibold text-sm">
                      <FileText size={16} />
                      <span>Project Overview</span>
                    </div>
                    <p className="text-muted-foreground text-sm leading-relaxed font-medium">
                      {project.description || 'No description provided for this project.'}
                    </p>
                  </CardContent>
                </Card>

                {/* Prepared area for future Board / Tasks / Activity modules */}
                <div className="bg-card border border-border/80 rounded-xl overflow-hidden shadow-sm">
                  {/* Glassmorphic Navigation Tabs */}
                  <div className="flex border-b border-border bg-muted/40 px-4">
                    <button
                      onClick={() => setActiveTab('tasks')}
                      className={`flex items-center space-x-2 py-3 px-4 border-b-2 font-bold text-xs uppercase tracking-wider transition-colors focus:outline-none ${
                        activeTab === 'tasks'
                          ? 'border-primary text-primary'
                          : 'border-transparent text-muted-foreground hover:text-foreground'
                      }`}
                    >
                      <LayoutList size={14} />
                      <span>Tasks Board</span>
                    </button>
                    <button
                      onClick={() => setActiveTab('members')}
                      className={`flex items-center space-x-2 py-3 px-4 border-b-2 font-bold text-xs uppercase tracking-wider transition-colors focus:outline-none ${
                        activeTab === 'members'
                          ? 'border-primary text-primary'
                          : 'border-transparent text-muted-foreground hover:text-foreground'
                      }`}
                    >
                      <Users size={14} />
                      <span>Members</span>
                    </button>
                    <button
                      onClick={() => setActiveTab('activity')}
                      className={`flex items-center space-x-2 py-3 px-4 border-b-2 font-bold text-xs uppercase tracking-wider transition-colors focus:outline-none ${
                        activeTab === 'activity'
                          ? 'border-primary text-primary'
                          : 'border-transparent text-muted-foreground hover:text-foreground'
                      }`}
                    >
                      <ClipboardList size={14} />
                      <span>Activity Timeline</span>
                    </button>
                  </div>

                  <div className="p-8 text-center py-16">
                    {activeTab === 'tasks' && (
                      <div className="space-y-4 py-6">
                        <div className="mx-auto w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-2">
                          <LayoutList size={24} />
                        </div>
                        <h4 className="text-xl font-bold text-foreground">Project Tasks Board</h4>
                        <p className="text-muted-foreground text-sm font-medium max-w-md mx-auto leading-relaxed">
                          Manage, filter, and track all your active task deliverables, priorities, and milestones for <strong>{project.name}</strong>.
                        </p>
                        <div className="pt-2">
                          <Link to={`/projects/${project.id}/tasks`}>
                            <Button className="flex items-center space-x-2 mx-auto font-semibold shadow-md">
                              <LayoutList size={16} />
                              <span>Open Tasks Board</span>
                            </Button>
                          </Link>
                        </div>
                      </div>
                    )}

                    {activeTab === 'members' && (
                      <div className="space-y-3">
                        <h4 className="text-lg font-bold text-foreground">Team Management Integration</h4>
                        <p className="text-muted-foreground text-sm font-medium max-w-md mx-auto">
                          Add, search, and assign project members, roles, and fine-grained permissions to this workspace soon.
                        </p>
                      </div>
                    )}

                    {activeTab === 'activity' && (
                      <div className="space-y-3">
                        <h4 className="text-lg font-bold text-foreground">Real-time Activity Feed</h4>
                        <p className="text-muted-foreground text-sm font-medium max-w-md mx-auto">
                          Follow project updates, task creations, member sign-ons, and workspace modifications inside this visual audit feed.
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Right Column: Metadata Sidebar */}
              <div className="space-y-6">
                <Card className="border border-border/80 bg-card shadow-sm">
                  <CardHeader className="pb-3 border-b border-border/50">
                    <CardTitle className="text-sm uppercase tracking-wider font-extrabold text-muted-foreground">
                      Workspace Details
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="pt-4 space-y-4">
                    <div className="space-y-1">
                      <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                        Assigned Owner
                      </span>
                      <div className="flex items-center space-x-2 text-foreground font-semibold text-sm">
                        <User size={16} className="text-primary/70" />
                        <span>{project.owner}</span>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                        Created Date
                      </span>
                      <div className="flex items-center space-x-2 text-foreground font-semibold text-sm">
                        <Calendar size={16} className="text-primary/70" />
                        <span>{formatDate(project.createdAt)}</span>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                        Last Updated
                      </span>
                      <div className="flex items-center space-x-2 text-foreground font-semibold text-sm">
                        <Calendar size={16} className="text-primary/70" />
                        <span>{formatDate(project.updatedAt)}</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          )}
        </>
      )}

      {/* Delete Confirmation Overlay Dialog */}
      <DeleteProjectDialog
        isOpen={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        onConfirm={handleDeleteConfirm}
        projectName={project?.name || ''}
        isLoading={isDeleting}
      />
    </div>
  )
}

export default ProjectDetailsPage
