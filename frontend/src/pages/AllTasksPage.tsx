import React, { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { fetchProjects, selectProjects } from '@/features/projects/store/projectSlice'
import taskApi, { type TaskResponseDTO } from '@/features/tasks/api/taskApi'
import profileApi from '@/features/auth/api/profileApi'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import {
  Search,
  Filter,
  Calendar,
  User,
  ListTodo,
  Briefcase,
  AlertCircle,
  ExternalLink,
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export const AllTasksPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  // Retrieve projects
  const { items: projects, initialized: projectsInitialized } = useAppSelector(selectProjects)

  // Local state for all tasks fetched across projects
  const [allTasks, setAllTasks] = useState<(TaskResponseDTO & { projectName: string })[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filter and search states
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [projectFilter, setProjectFilter] = useState('')

  // User profiles cache for assignees
  const [resolvedAssignees, setResolvedAssignees] = useState<Record<string, any>>({})

  // Fetch all projects if not already initialized
  useEffect(() => {
    dispatch(fetchProjects({ page: 0, size: 100 }))
  }, [dispatch])

  // Fetch tasks for each project
  useEffect(() => {
    if (projects.length === 0 && projectsInitialized) return

    const loadAllTasks = async () => {
      setLoading(true)
      setError(null)
      try {
        const promises = projects.map(async (project) => {
          try {
            const res = await taskApi.getTasks({ projectId: project.id, size: 100 })
            return res.content.map((task: any) => ({
              ...task,
              projectName: project.name,
            }))
          } catch {
            return []
          }
        })

        const results = await Promise.all(promises)
        setAllTasks(results.flat())
      } catch (err: any) {
        setError('Failed to aggregate tasks from your workspaces.')
      } finally {
        setLoading(false)
      }
    }

    loadAllTasks()
  }, [projects, projectsInitialized])

  // Background assignee profile name resolution
  useEffect(() => {
    if (allTasks.length === 0) return

    const resolveAssignees = async () => {
      const pendingIds = allTasks
        .map((t) => t.assignedTo)
        .filter((id): id is string => !!id && !resolvedAssignees[id])
      
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

        const updated = { ...resolvedAssignees }
        resolved.forEach((item) => {
          if (item.profile) {
            updated[item.id] = item.profile
          }
        })
        setResolvedAssignees(updated)
      } catch (err) {
        console.error('Failed to resolve task assignees', err)
      }
    }

    resolveAssignees()
  }, [allTasks, resolvedAssignees])

  // Filter tasks locally based on filters
  const filteredTasks = allTasks.filter((task) => {
    const matchesSearch = task.title.toLowerCase().includes(searchTerm.toLowerCase()) || 
      (task.description && task.description.toLowerCase().includes(searchTerm.toLowerCase()))
    const matchesStatus = statusFilter ? task.status === statusFilter : true
    const matchesPriority = priorityFilter ? task.priority === priorityFilter : true
    const matchesProject = projectFilter ? task.projectId.toString() === projectFilter : true

    return matchesSearch && matchesStatus && matchesPriority && matchesProject
  })

  const formatDate = (epochMillis: number | null | undefined) => {
    if (!epochMillis) return 'No due date'
    const date = new Date(epochMillis)
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  return (
    <div className="max-w-7xl mx-auto px-4 md:px-8 py-8 space-y-8 animate-in fade-in duration-500">
      
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl font-outfit">
          Global Task Board
        </h1>
        <p className="text-muted-foreground text-sm font-semibold mt-1">
          Complete, search, and filter all tasks across your collaborative project workspaces ({filteredTasks.length} tasks).
        </p>
      </div>

      {/* Advanced Filters */}
      <div className="bg-card/25 backdrop-blur-sm p-5 rounded-2xl border border-border/50 space-y-4">
        <div className="relative">
          <Input
            placeholder="Search tasks by title or summary..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-11 h-11"
          />
          <Search className="w-5 h-5 text-muted-foreground absolute left-4 top-3.5" />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {/* Project Filter */}
          <div className="relative">
            <select
              value={projectFilter}
              onChange={(e) => setProjectFilter(e.target.value)}
              className="w-full h-11 pl-10 pr-4 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
            >
              <option value="">All Workspaces</option>
              {projects.map((p) => (
                <option key={p.id} value={p.id.toString()}>
                  {p.name}
                </option>
              ))}
            </select>
            <Briefcase className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
          </div>

          {/* Status Filter */}
          <div className="relative">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full h-11 pl-10 pr-4 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
            >
              <option value="">All Statuses</option>
              <option value="TODO">To Do</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="DONE">Completed</option>
              <option value="BLOCKED">Blocked</option>
            </select>
            <Filter className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
          </div>

          {/* Priority Filter */}
          <div className="relative">
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              className="w-full h-11 pl-10 pr-4 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
            >
              <option value="">All Priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
            <AlertCircle className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
          </div>
        </div>
      </div>

      {/* Main task list */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-20 rounded-xl border border-border bg-card animate-pulse" />
          ))}
        </div>
      ) : error ? (
        <div className="p-8 rounded-2xl border border-red-500/10 bg-red-500/5 text-center space-y-4">
          <div className="flex justify-center text-red-500">
            <AlertCircle className="w-12 h-12" />
          </div>
          <h3 className="text-lg font-bold text-foreground">Failed to aggregate tasks</h3>
          <p className="text-sm text-muted-foreground">{error}</p>
        </div>
      ) : filteredTasks.length === 0 ? (
        <div className="py-16 text-center border border-dashed border-border/80 rounded-3xl bg-card/10 backdrop-blur-sm space-y-4">
          <div className="flex justify-center text-muted-foreground/60">
            <ListTodo className="w-16 h-16 stroke-[1.2]" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-foreground">No tasks found</h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              There are no tasks matching your filters. Try adjusting your search term or select another workspace.
            </p>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredTasks.map((task) => {
            const assigneeName = task.assignedTo
              ? `${resolvedAssignees[task.assignedTo]?.firstName || ''} ${resolvedAssignees[task.assignedTo]?.surname || ''}`.trim() || resolvedAssignees[task.assignedTo]?.username || 'Assigned'
              : 'Unassigned'
            const initials = task.assignedTo
              ? `${resolvedAssignees[task.assignedTo]?.firstName?.[0] || ''}${resolvedAssignees[task.assignedTo]?.surname?.[0] || ''}`.toUpperCase() || '?'
              : ''

            return (
              <Card
                key={task.id}
                className="border border-border hover:border-primary/30 bg-card hover:bg-card/75 transition-all p-5 shadow-sm rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-2 min-w-0">
                  <div className="flex items-center space-x-2.5">
                    <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-md bg-muted text-muted-foreground font-mono">
                      #{task.id}
                    </span>
                    <span
                      onClick={() => navigate(`/projects/${task.projectId}`)}
                      className="text-xs text-primary font-bold hover:underline cursor-pointer flex items-center shrink-0"
                    >
                      <Briefcase className="w-3.5 h-3.5 mr-1" />
                      {task.projectName}
                    </span>
                  </div>

                  <h3 className="font-bold text-base text-foreground truncate max-w-md sm:max-w-xl">
                    {task.title}
                  </h3>

                  {task.description && (
                    <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                      {task.description}
                    </p>
                  )}
                </div>

                <div className="flex flex-wrap items-center gap-3 shrink-0 pt-2 md:pt-0 border-t md:border-t-0 border-border/40">
                  {/* Priority Badge */}
                  <span
                    className={`text-[9px] font-extrabold uppercase px-2.5 py-0.5 rounded-full ${
                      task.priority === 'HIGH'
                        ? 'bg-red-500/10 text-red-500 border border-red-500/15'
                        : task.priority === 'MEDIUM'
                          ? 'bg-blue-500/10 text-blue-500 border border-blue-500/15'
                          : 'bg-slate-500/10 text-slate-500 border border-slate-500/15'
                    }`}
                  >
                    {task.priority}
                  </span>

                  {/* Status Badge */}
                  <span
                    className={`text-[9px] font-extrabold uppercase px-2.5 py-0.5 rounded-full ${
                      task.status === 'DONE'
                        ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/15'
                        : task.status === 'IN_PROGRESS'
                          ? 'bg-amber-500/10 text-amber-500 border border-amber-500/15'
                          : task.status === 'BLOCKED'
                            ? 'bg-red-500/10 text-red-500 border border-red-500/15'
                            : 'bg-muted text-muted-foreground border border-border'
                    }`}
                  >
                    {task.status.replace('_', ' ')}
                  </span>

                  {/* Calendar / Due Date */}
                  <div className="flex items-center space-x-1.5 text-xs text-muted-foreground">
                    <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                    <span>{formatDate(task.dueDate)}</span>
                  </div>

                  {/* Assignee Avatar */}
                  <div className="flex items-center space-x-1.5 pl-2 border-l border-border/50">
                    {task.assignedTo ? (
                      <div
                        className="w-7 h-7 rounded-full bg-primary/15 border border-primary/20 flex items-center justify-center font-extrabold text-primary text-[9px]"
                        title={`Assigned to: ${assigneeName}`}
                      >
                        {initials}
                      </div>
                    ) : (
                      <div
                        className="w-7 h-7 rounded-full bg-muted border border-border flex items-center justify-center text-muted-foreground"
                        title="Unassigned"
                      >
                        <User className="w-3.5 h-3.5" />
                      </div>
                    )}
                  </div>

                  {/* Navigation trigger button */}
                  <Button
                    onClick={() => navigate(`/projects/${task.projectId}`)}
                    variant="outline"
                    className="h-8 w-8 p-0 rounded-lg flex items-center justify-center shrink-0 border-border"
                    title="View project workspace"
                  >
                    <ExternalLink className="w-3.5 h-3.5 text-muted-foreground hover:text-primary" />
                  </Button>
                </div>
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default AllTasksPage
