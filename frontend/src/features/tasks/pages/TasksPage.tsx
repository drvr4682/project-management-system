import React, { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Plus, Search, Filter, ArrowLeft, ArrowUpDown } from 'lucide-react'
import { toast } from 'sonner'
import { useTasks } from '../hooks/useTasks'
import { projectApi } from '@/features/projects/api/projectApi'
import { TaskList } from '../components/TaskList'
import { EmptyTasksState } from '../components/EmptyTasksState'
import { DeleteTaskDialog } from '../components/DeleteTaskDialog'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import type { TaskDto } from '../types/taskTypes'
import type { ProjectDto } from '@/features/projects/types/projectTypes'

export const TasksPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>()
  const parsedProjectId = projectId ? parseInt(projectId, 10) : NaN

  const {
    tasks,
    isLoading,
    statusFilter,
    setStatusFilter,
    priorityFilter,
    setPriorityFilter,
    setSearchQuery,
    sortBy,
    setSortBy,
    sortDirection,
    setSortDirection,
    fetchTasks,
    deleteTask,
  } = useTasks()

  const [project, setProject] = useState<ProjectDto | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [deleteTarget, setDeleteTarget] = useState<TaskDto | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Fetch task and project data on mount
  useEffect(() => {
    if (isNaN(parsedProjectId)) return

    const loadData = async () => {
      try {
        const fetchedProject = await projectApi.getById(parsedProjectId)
        setProject(fetchedProject)
        await fetchTasks(parsedProjectId)
      } catch (e: any) {
        toast.error('Failed to load project details or task board.')
      }
    }

    loadData()
  }, [parsedProjectId, fetchTasks])

  // Trigger search on submit or enter
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setSearchQuery(searchInput)
  }

  // Handle status filter change
  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setStatusFilter(e.target.value)
  }

  // Handle priority filter change
  const handlePriorityChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setPriorityFilter(e.target.value)
  }

  // Toggle sort order
  const triggerSort = (field: 'dueDate' | 'priority' | 'createdAt') => {
    if (sortBy === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortBy(field)
      setSortDirection('desc')
    }
  }

  // Confirm delete handler
  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setIsDeleting(true)
    try {
      await deleteTask(deleteTarget.id, parsedProjectId)
      toast.success(`Task "${deleteTarget.title}" deleted successfully.`)
      setDeleteTarget(null)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to delete task.'
      toast.error(msg)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Breadcrumbs and Actions */}
      <div className="space-y-2">
        <Link
          to={`/projects/${parsedProjectId}`}
          className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group"
        >
          <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
          <span>Back to Project Overview</span>
        </Link>

        <div className="flex flex-col md:flex-row md:items-center md:justify-between space-y-4 md:space-y-0">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl">
              {project ? `${project.name} Tasks` : 'Project Tasks'}
            </h1>
            <p className="text-muted-foreground text-sm font-medium mt-1">
              Organize, assign, and outline active milestones and workspace due dates.
            </p>
          </div>

          <Link to={`/tasks/create?projectId=${parsedProjectId}`}>
            <Button className="flex items-center space-x-2 w-full md:w-auto shadow-md">
              <Plus size={18} />
              <span>Create Task</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* Filter, Search & Sorting Controls */}
      <div className="space-y-3 bg-card/65 backdrop-blur-md border border-border/80 p-4 rounded-xl shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex flex-col sm:flex-row items-stretch sm:items-center space-y-3 sm:space-y-0 sm:space-x-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search tasks by title or description..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="pl-9 h-10"
            />
          </div>

          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-1.5 min-w-[130px]">
              <Filter size={14} className="text-muted-foreground" />
              <select
                value={statusFilter}
                onChange={handleStatusChange}
                className="flex h-10 w-full rounded-md border border-input bg-background px-2.5 py-1.5 text-xs font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="">All Statuses</option>
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="DONE">Completed</option>
                <option value="BLOCKED">Blocked</option>
              </select>
            </div>

            <div className="flex items-center space-x-1.5 min-w-[130px]">
              <select
                value={priorityFilter}
                onChange={handlePriorityChange}
                className="flex h-10 w-full rounded-md border border-input bg-background px-2.5 py-1.5 text-xs font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="">All Priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <Button type="submit" variant="secondary" className="h-10 px-4 text-xs font-semibold">
              Apply
            </Button>
          </div>
        </form>

        {/* Sort Options Buttons Row */}
        <div className="flex flex-wrap gap-2 items-center pt-2 border-t border-border/50 text-xs">
          <span className="text-muted-foreground font-semibold flex items-center space-x-1 mr-1">
            <ArrowUpDown size={13} />
            <span>Sort by:</span>
          </span>
          <Button
            variant={sortBy === 'createdAt' ? 'secondary' : 'ghost'}
            size="sm"
            onClick={() => triggerSort('createdAt')}
            className="h-7 text-xs px-2.5"
          >
            Created Date {sortBy === 'createdAt' && (sortDirection === 'asc' ? '↑' : '↓')}
          </Button>
          <Button
            variant={sortBy === 'dueDate' ? 'secondary' : 'ghost'}
            size="sm"
            onClick={() => triggerSort('dueDate')}
            className="h-7 text-xs px-2.5"
          >
            Due Date {sortBy === 'dueDate' && (sortDirection === 'asc' ? '↑' : '↓')}
          </Button>
          <Button
            variant={sortBy === 'priority' ? 'secondary' : 'ghost'}
            size="sm"
            onClick={() => triggerSort('priority')}
            className="h-7 text-xs px-2.5"
          >
            Priority {sortBy === 'priority' && (sortDirection === 'asc' ? '↑' : '↓')}
          </Button>
        </div>
      </div>

      {/* Main Task List Content */}
      {tasks.length === 0 && !isLoading ? (
        <EmptyTasksState />
      ) : (
        <TaskList
          tasks={tasks}
          isLoading={isLoading}
          onDeleteClick={(t) => setDeleteTarget(t)}
        />
      )}

      {/* Delete Confirmation Overlay Dialog */}
      <DeleteTaskDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        taskTitle={deleteTarget?.title || ''}
        isLoading={isDeleting}
      />
    </div>
  )
}

export default TasksPage
