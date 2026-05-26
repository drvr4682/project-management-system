import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Search, Filter, ChevronLeft, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { useProjects } from '../hooks/useProjects'
import { ProjectList } from '../components/ProjectList'
import { EmptyProjectsState } from '../components/EmptyProjectsState'
import { DeleteProjectDialog } from '../components/DeleteProjectDialog'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import type { ProjectDto } from '../types/projectTypes'

export const ProjectsPage: React.FC = () => {
  const {
    projects,
    isLoading,
    totalElements,
    totalPages,
    currentPage,
    statusFilter,
    setStatusFilter,
    searchQuery,
    setSearchQuery,
    fetchProjects,
    deleteProject,
  } = useProjects()

  const [searchInput, setSearchInput] = useState('')
  const [deleteTarget, setDeleteTarget] = useState<ProjectDto | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Fetch initial data
  useEffect(() => {
    fetchProjects(0, searchInput, statusFilter)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter])

  // Trigger search on submit or enter
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setSearchQuery(searchInput)
    fetchProjects(0, searchInput, statusFilter)
  }

  // Handle status filter dropdown change
  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setStatusFilter(e.target.value)
  }

  // Handle pagination transition
  const handlePageChange = (page: number) => {
    fetchProjects(page, searchQuery, statusFilter)
  }

  // Action to perform actual deletion
  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setIsDeleting(true)
    try {
      await deleteProject(deleteTarget.id)
      toast.success(`Project "${deleteTarget.name}" deleted successfully.`)
      setDeleteTarget(null)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to delete project.'
      toast.error(msg)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Projects"
        subtitle={`Create, view, and manage your active team workspaces (${totalElements} total).`}
        actions={
          <Link to="/projects/create">
            <Button className="flex items-center space-x-2 w-full md:w-auto shadow-md rounded-xl font-bold">
              <Plus size={18} />
              <span>Create Project</span>
            </Button>
          </Link>
        }
      />

      {/* Filter and Search Bar */}
      <form
        onSubmit={handleSearchSubmit}
        className="flex flex-col sm:flex-row items-stretch sm:items-center space-y-3 sm:space-y-0 sm:space-x-4 bg-card/65 backdrop-blur-md border border-border/80 p-4 rounded-xl shadow-sm"
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search projects by name..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="pl-9 h-10"
          />
        </div>

        <div className="flex items-center space-x-3 sm:w-auto">
          <div className="relative flex items-center space-x-1.5 min-w-[140px]">
            <Filter size={15} className="text-muted-foreground" />
            <select
              value={statusFilter}
              onChange={handleStatusChange}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>

          <Button type="submit" variant="secondary" className="h-10 px-5">
            Search
          </Button>
        </div>
      </form>

      {/* Primary Projects Content */}
      {projects.length === 0 && !isLoading ? (
        <EmptyProjectsState />
      ) : (
        <div className="space-y-6">
          <ProjectList
            projects={projects}
            isLoading={isLoading}
            onDeleteClick={(p) => setDeleteTarget(p)}
          />

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-border/60 pt-4">
              <span className="text-xs text-muted-foreground font-semibold">
                Page {currentPage + 1} of {totalPages}
              </span>
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0 || isLoading}
                  className="flex items-center space-x-1"
                >
                  <ChevronLeft size={16} />
                  <span>Previous</span>
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === totalPages - 1 || isLoading}
                  className="flex items-center space-x-1"
                >
                  <span>Next</span>
                  <ChevronRight size={16} />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Delete Confirmation Overlay Dialog */}
      <DeleteProjectDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        projectName={deleteTarget?.name || ''}
        isLoading={isDeleting}
      />
    </div>
  )
}

export default ProjectsPage
