import React, { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import {
  fetchProjects,
  setProjectFilters,
  selectProjects,
  deleteProject,
} from '@/features/projects/store/projectSlice'
import { ProjectForm } from '@/features/projects/components/ProjectForm'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import {
  Plus,
  Search,
  Filter,
  Trash2,
  Calendar,
  User,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import profileApi from '@/features/auth/api/profileApi'

// Sleek Custom Pencil icon
const PencilIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg
    viewBox="0 0 24 24"
    width="18"
    height="18"
    stroke="currentColor"
    strokeWidth="2"
    fill="none"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={props.className}
  >
    <path d="M12 20h9" />
    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
  </svg>
)

export const ProjectsPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  
  const { items, pagination, loading, error, initialized } = useAppSelector(selectProjects)

  // Local state for modals and search/filters
  const [formOpen, setFormOpen] = useState(false)
  const [editingProject, setEditingProject] = useState<any>(null)
  const [searchInput, setSearchInput] = useState('')
  const [statusFilter, setStatusFilter] = useState('ACTIVE')
  
  // Background profile resolver cache
  const [resolvedOwners, setResolvedOwners] = useState<Record<string, any>>({})

  // Trigger loading projects on mount and filters change
  useEffect(() => {
    dispatch(fetchProjects({ page: 0 }))
  }, [dispatch])

  // Fetch Owner usernames in background
  useEffect(() => {
    if (items.length === 0) return

    const resolveOwners = async () => {
      const pendingIds = items
        .map((p) => p.owner)
        .filter((id): id is string => !!id && !resolvedOwners[id])
      
      if (pendingIds.length === 0) return

      try {
        const resolved = await Promise.all(
          pendingIds.map(async (id) => {
            try {
              const profile = await profileApi.getProfileById(id)
              return { id, profile }
            } catch {
              return { id, profile: null }
            }
          })
        )

        const updated = { ...resolvedOwners }
        resolved.forEach((item) => {
          if (item.profile) {
            updated[item.id] = item.profile
          }
        })
        setResolvedOwners(updated)
      } catch (err) {
        console.error('Failed to resolve project owners', err)
      }
    }

    resolveOwners()
  }, [items, resolvedOwners])

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    dispatch(setProjectFilters({ 
      search: searchInput || undefined,
      status: statusFilter || undefined
    }))
    dispatch(fetchProjects({ page: 0 }))
  }

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < pagination.totalPages) {
      dispatch(fetchProjects({ page: newPage }))
    }
  }

  const handleDeleteProject = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation()
    if (window.confirm('Are you sure you want to delete this project workspace?')) {
      try {
        await dispatch(deleteProject(id)).unwrap()
        toast.success('Project workspace deleted')
      } catch (err: any) {
        toast.error(err || 'Failed to delete project')
      }
    }
  }

  const handleEditProject = (e: React.MouseEvent, project: any) => {
    e.stopPropagation()
    setEditingProject(project)
    setFormOpen(true)
  }

  const handleCreateNewProject = () => {
    setEditingProject(null)
    setFormOpen(true)
  }

  const formatDate = (epochMillis: number) => {
    if (!epochMillis) return 'N/A'
    const date = new Date(epochMillis)
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  const totalLabel = initialized ? `${pagination.totalElements} total` : 'undefined total'

  return (
    <div className="max-w-7xl mx-auto px-4 md:px-8 py-8 space-y-8 animate-in fade-in duration-500">
      
      {/* Header section with Create Project */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl font-outfit">
            Project Directory
          </h1>
          <p className="text-muted-foreground text-sm font-semibold mt-1">
            Create, view, and manage your active projects ({totalLabel}).
          </p>
        </div>
        <Button onClick={handleCreateNewProject} className="rounded-xl h-11 px-5 font-bold space-x-2 shrink-0 shadow-md">
          <Plus className="w-5 h-5" />
          <span>Create Project</span>
        </Button>
      </div>

      {/* Filter panel */}
      <form onSubmit={handleSearchSubmit} className="flex flex-col sm:flex-row items-center gap-4 bg-card/25 backdrop-blur-sm p-4 rounded-2xl border border-border/50">
        <div className="relative flex-1 w-full">
          <Input
            placeholder="Search workspaces by name..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="pl-11 h-11"
          />
          <Search className="w-5 h-5 text-muted-foreground absolute left-4 top-3" />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto shrink-0">
          <div className="relative w-full sm:w-44">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full h-11 pl-10 pr-4 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="ARCHIVED">Archived</option>
            </select>
            <Filter className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
          </div>

          <Button type="submit" variant="outline" className="h-11 px-6 rounded-xl font-bold bg-muted/40 hover:bg-muted text-foreground border border-border shrink-0">
            Search
          </Button>
        </div>
      </form>

      {/* Grid view of projects */}
      {error ? (
        <div className="p-8 rounded-2xl border border-red-500/10 bg-red-500/5 text-center space-y-4">
          <div className="flex justify-center text-red-500">
            <AlertCircle className="w-12 h-12" />
          </div>
          <h3 className="text-lg font-bold text-foreground">Failed to Load Workspaces</h3>
          <p className="text-sm text-muted-foreground max-w-md mx-auto">{error}</p>
          <Button onClick={() => dispatch(fetchProjects({ page: 0 }))} variant="outline" className="rounded-xl h-10 border-red-500/20 hover:bg-red-500/10 text-red-500 font-bold">
            Retry Connection
          </Button>
        </div>
      ) : loading && !initialized ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-56 rounded-2xl border border-border bg-card animate-pulse p-6 space-y-4">
              <div className="h-6 w-2/3 bg-muted rounded-md" />
              <div className="h-4 w-full bg-muted rounded-md" />
              <div className="h-4 w-1/2 bg-muted rounded-md" />
              <div className="h-4 w-1/3 bg-muted rounded-md" />
            </div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="py-16 text-center border border-dashed border-border/80 rounded-3xl bg-card/10 backdrop-blur-sm space-y-4">
          <div className="flex justify-center text-muted-foreground/60">
            <Search className="w-16 h-16 stroke-[1.2]" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-foreground">No workspaces found</h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              We couldn't find any projects matching your search term. Get started by creating your very first workspace!
            </p>
          </div>
          <Button onClick={handleCreateNewProject} className="rounded-xl font-bold">
            Create First Workspace
          </Button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {items.map((project) => {
              const ownerEmail = resolvedOwners[project.owner]?.username || resolvedOwners[project.owner]?.email || project.owner || 'System'
              return (
                <div
                  key={project.id}
                  onClick={() => navigate(`/projects/${project.id}`)}
                  className="group relative rounded-2xl border border-border hover:border-primary/40 bg-card hover:bg-card/70 transition-all duration-300 p-6 flex flex-col justify-between cursor-pointer shadow-sm hover:shadow-md animate-in fade-in"
                >
                  <div className="space-y-4">
                    <div className="flex items-start justify-between">
                      <h3 className="font-bold text-lg text-foreground group-hover:text-primary transition-colors truncate pr-4">
                        {project.name}
                      </h3>
                      <span className="text-[10px] font-extrabold uppercase px-2.5 py-1 rounded-full bg-primary/10 text-primary border border-primary/20 shrink-0">
                        {project.status || 'ACTIVE'}
                      </span>
                    </div>

                    <p className="text-xs text-muted-foreground line-clamp-3 leading-relaxed">
                      {project.description || 'No description provided.'}
                    </p>

                    <div className="space-y-2 pt-2 border-t border-border/40 text-xs text-muted-foreground">
                      <div className="flex items-center space-x-2">
                        <Calendar className="w-4 h-4 text-muted-foreground" />
                        <span>Created: {formatDate(project.createdAt)}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <User className="w-4 h-4 text-muted-foreground" />
                        <span className="truncate">Owner: {ownerEmail}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-4 mt-4 border-t border-border/40">
                    <span className="text-xs font-bold text-primary group-hover:underline flex items-center">
                      View details <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
                    </span>

                    <div className="flex items-center space-x-2 shrink-0">
                      <button
                        onClick={(e) => handleEditProject(e, project)}
                        className="p-1.5 rounded-lg text-muted-foreground hover:text-primary hover:bg-primary/10 border border-border bg-muted/20 transition-all"
                        title="Edit Project"
                      >
                        <PencilIcon className="w-4 h-4" />
                      </button>
                      <button
                        onClick={(e) => handleDeleteProject(e, project.id)}
                        className="p-1.5 rounded-lg text-muted-foreground hover:text-red-500 hover:bg-red-500/10 border border-red-500/10 bg-red-500/5 transition-all"
                        title="Delete Workspace"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>

          {/* Pagination */}
          {pagination.totalPages > 1 && (
            <div className="flex items-center justify-between pt-6 border-t border-border/50">
              <span className="text-xs text-muted-foreground font-semibold">
                Showing {items.length} of {pagination.totalElements} workspaces
              </span>

              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(pagination.page - 1)}
                  disabled={pagination.page === 0}
                  className="rounded-xl h-9 w-9 p-0 flex items-center justify-center"
                >
                  <ChevronLeft className="w-5 h-5" />
                </Button>
                <span className="text-xs font-bold font-outfit text-foreground">
                  Page {pagination.page + 1} of {pagination.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(pagination.page + 1)}
                  disabled={pagination.page >= pagination.totalPages - 1}
                  className="rounded-xl h-9 w-9 p-0 flex items-center justify-center"
                >
                  <ChevronRight className="w-5 h-5" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Project form modal */}
      <ProjectForm
        isOpen={formOpen}
        onClose={() => {
          setFormOpen(false)
          setEditingProject(null)
        }}
        initialData={editingProject}
      />
    </div>
  )
}

export default ProjectsPage
