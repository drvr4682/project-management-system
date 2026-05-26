import React, { useEffect, useState, useMemo } from 'react'
import { ClipboardList, Search, Filter, ArrowUpDown, Folder } from 'lucide-react'
import { toast } from 'sonner'
import { projectApi } from '@/features/projects/api/projectApi'
import { taskApi } from '@/features/tasks/api/taskApi'
import TaskCard from '@/features/tasks/components/TaskCard'
import DeleteTaskDialog from '@/features/tasks/components/DeleteTaskDialog'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'
import type { TaskDto } from '@/features/tasks/types/taskTypes'
import type { ProjectDto } from '@/features/projects/types/projectTypes'

export const MyTasksPage: React.FC = () => {
  const { user } = useAppSelector(selectAuth)
  const userEmail = user?.email || ''

  const [tasks, setTasks] = useState<TaskDto[]>([])
  const [projects, setProjects] = useState<ProjectDto[]>([])
  const [isLoading, setIsLoading] = useState(true)

  // Local filter/sort states
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [projectFilter, setProjectFilter] = useState('')
  const [sortBy, setSortBy] = useState<'dueDate' | 'priority' | 'createdAt'>('dueDate')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc')

  const [deleteTarget, setDeleteTarget] = useState<TaskDto | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const loadAllMyTasks = async () => {
    if (!userEmail) return
    setIsLoading(true)
    try {
      // 1. Fetch all user projects (max 100 for roster)
      const projectResponse = await projectApi.getAll({ size: 100 })
      const activeProjects = projectResponse.content
      setProjects(activeProjects)

      // 2. Fetch tasks for each project where assignee matches current user in parallel
      const tasksPromises = activeProjects.map((p) =>
        taskApi
          .getAll({
            projectId: p.id,
            assignedTo: userEmail,
            size: 100,
          })
          .then((res) => res.content)
          .catch(() => [] as TaskDto[])
      )

      const nestedTasks = await Promise.all(tasksPromises)
      const flattenedTasks = nestedTasks.flat()
      setTasks(flattenedTasks)
    } catch (e: any) {
      toast.error('Failed to load your personal tasks.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadAllMyTasks()
  }, [userEmail])

  // Search submit
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setSearchQuery(searchInput)
  }

  // Handle task deletion success
  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setIsDeleting(true)
    try {
      await taskApi.delete(deleteTarget.id)
      toast.success(`Task "${deleteTarget.title}" deleted successfully.`)
      setDeleteTarget(null)
      await loadAllMyTasks()
    } catch (e: any) {
      toast.error(e.response?.data?.message || 'Failed to delete task.')
    } finally {
      setIsDeleting(false)
    }
  }

  // Toggle sort order
  const triggerSort = (field: 'dueDate' | 'priority' | 'createdAt') => {
    if (sortBy === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortBy(field)
      setSortDirection(field === 'dueDate' ? 'asc' : 'desc')
    }
  }

  // Frontend filter and sort mapping
  const filteredAndSortedTasks = useMemo(() => {
    let result = [...tasks]

    // 1. Search Query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      result = result.filter(
        (t) =>
          t.title.toLowerCase().includes(q) ||
          t.description.toLowerCase().includes(q)
      )
    }

    // 2. Status
    if (statusFilter) {
      result = result.filter((t) => t.status === statusFilter)
    }

    // 3. Priority
    if (priorityFilter) {
      result = result.filter((t) => t.priority === priorityFilter)
    }

    // 4. Project
    if (projectFilter) {
      result = result.filter((t) => t.projectId === parseInt(projectFilter, 10))
    }

    // 5. Sort
    result.sort((a, b) => {
      let comparison = 0
      if (sortBy === 'dueDate') {
        const valA = a.dueDate || 9999999999999
        const valB = b.dueDate || 9999999999999
        comparison = valA - valB
      } else if (sortBy === 'priority') {
        const weights = { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 }
        comparison = weights[a.priority] - weights[b.priority]
      } else {
        comparison = a.createdAt - b.createdAt
      }
      return sortDirection === 'asc' ? comparison : -comparison
    })

    return result
  }, [tasks, searchQuery, statusFilter, priorityFilter, projectFilter, sortBy, sortDirection])

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <div className="flex items-center space-x-2 text-primary font-bold mb-1">
          <ClipboardList size={16} />
          <span className="text-xs uppercase tracking-wider font-extrabold">Personal Core</span>
        </div>
        <h1 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl">
          My Delegated Tasks
        </h1>
        <p className="text-muted-foreground text-sm font-medium mt-1">
          Review, filter, and complete all action deliverables assigned to your account.
        </p>
      </div>

      {/* Filter and Search Controls */}
      <div className="space-y-3 bg-card/65 backdrop-blur-md border border-border/80 p-4 rounded-xl shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex flex-col lg:flex-row items-stretch lg:items-center space-y-3 lg:space-y-0 lg:space-x-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search my tasks by title or description..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="pl-9 h-10"
            />
          </div>

          <div className="grid grid-cols-2 sm:flex sm:items-center gap-3">
            <div className="flex items-center space-x-1.5 min-w-[130px]">
              <Filter size={13} className="text-muted-foreground shrink-0" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-2 py-1.5 text-xs font-semibold text-foreground focus-visible:outline-none"
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
                onChange={(e) => setPriorityFilter(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-2 py-1.5 text-xs font-semibold text-foreground focus-visible:outline-none"
              >
                <option value="">All Priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div className="flex items-center space-x-1.5 min-w-[140px] col-span-2 sm:col-span-1">
              <Folder size={13} className="text-muted-foreground shrink-0" />
              <select
                value={projectFilter}
                onChange={(e) => setProjectFilter(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-2 py-1.5 text-xs font-semibold text-foreground focus-visible:outline-none"
              >
                <option value="">All Projects</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <Button type="submit" variant="secondary" className="h-10 px-4 text-xs font-semibold col-span-2 sm:col-span-1">
              Apply
            </Button>
          </div>
        </form>

        {/* Sort Options Button Row */}
        <div className="flex flex-wrap gap-2 items-center pt-2 border-t border-border/50 text-xs">
          <span className="text-muted-foreground font-semibold flex items-center space-x-1 mr-1">
            <ArrowUpDown size={13} />
            <span>Sort by:</span>
          </span>
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
          <Button
            variant={sortBy === 'createdAt' ? 'secondary' : 'ghost'}
            size="sm"
            onClick={() => triggerSort('createdAt')}
            className="h-7 text-xs px-2.5"
          >
            Created Date {sortBy === 'createdAt' && (sortDirection === 'asc' ? '↑' : '↓')}
          </Button>
        </div>
      </div>

      {/* Grid Content */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="border border-border/70 rounded-xl p-5 space-y-4 bg-card/60 animate-pulse">
              <div className="flex justify-between items-start">
                <div className="h-5 w-1/2 bg-muted rounded"></div>
                <div className="h-4 w-12 bg-muted rounded-full"></div>
              </div>
              <div className="space-y-2">
                <div className="h-3 w-full bg-muted rounded"></div>
                <div className="h-3 w-4/5 bg-muted rounded"></div>
              </div>
            </div>
          ))}
        </div>
      ) : filteredAndSortedTasks.length === 0 ? (
        <div className="flex flex-col items-center justify-center p-8 text-center bg-card/60 backdrop-blur-md rounded-xl border border-border/80 shadow-md py-16">
          <div className="mx-auto w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center text-primary mb-5">
            <ClipboardList size={32} />
          </div>
          <h3 className="text-2xl font-extrabold tracking-tight text-foreground mb-2">No Tasks Assigned</h3>
          <p className="text-muted-foreground max-w-sm mb-4 font-medium text-sm leading-relaxed">
            You do not have any tasks currently assigned to your account. Delegate deliverables to your team or create tasks to begin!
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in duration-300">
          {filteredAndSortedTasks.map((task) => (
            <div key={task.id} className="h-full">
              <TaskCard task={task} onDeleteClick={(t) => setDeleteTarget(t)} />
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation Modal Overlay */}
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

export default MyTasksPage
