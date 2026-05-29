import React, { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import { fetchProjects, selectProjects } from '@/features/projects/store/projectSlice'
import taskApi, { type TaskResponseDTO } from '@/features/tasks/api/taskApi'
import { Card, CardHeader, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { ProjectForm } from '@/features/projects/components/ProjectForm'
import {
  Folder,
  ClipboardList,
  CheckCircle2,
  Clock,
  AlertTriangle,
  ChevronRight,
  TrendingUp,
  Plus,
  AlertCircle,
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export const DashboardPage: React.FC = () => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  // Retrieve projects
  const { items: projects, initialized: projectsInitialized } = useAppSelector(selectProjects)

  // Local state for all tasks fetched across projects
  const [allTasks, setAllTasks] = useState<(TaskResponseDTO & { projectName: string })[]>([])
  const [formOpen, setFormOpen] = useState(false)

  // Fetch all projects on mount
  useEffect(() => {
    dispatch(fetchProjects({ page: 0, size: 100 }))
  }, [dispatch])

  // Fetch tasks for each project
  useEffect(() => {
    if (projects.length === 0 && projectsInitialized) return

    const loadAllTasks = async () => {
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
        console.error('Failed to load dashboard tasks', err)
      }
    }

    loadAllTasks()
  }, [projects, projectsInitialized])

  // Calculated Stats
  const totalProjects = projects.length
  const totalTasks = allTasks.length
  const completedTasks = allTasks.filter((t) => t.status === 'DONE').length
  const pendingTasks = allTasks.filter((t) => t.status !== 'DONE').length
  
  // Tasks with due dates in past that are not completed (DONE)
  const overdueTasks = allTasks.filter((t) => {
    if (t.status === 'DONE' || !t.dueDate) return false
    return new Date(t.dueDate) < new Date()
  }).length

  const taskCompletionRate = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0
  const completedProjectsCount = projects.filter((p) => p.status === 'COMPLETED').length

  // Recent Projects (up to 3)
  const recentProjects = [...projects].slice(0, 3)

  // Recent Tasks (up to 5)
  const recentTasks = [...allTasks]
    .sort((a, b) => b.id - a.id) // Sort by descending ID for recency
    .slice(0, 5)

  // Upcoming Milestones (tasks with due dates in future, sorted soonest first)
  const upcomingDeadlines = allTasks
    .filter((t) => t.status !== 'DONE' && t.dueDate && new Date(t.dueDate) >= new Date())
    .sort((a, b) => new Date(a.dueDate!).getTime() - new Date(b.dueDate!).getTime())
    .slice(0, 5)

  // Pending Tasks Workload (non-completed tasks)
  const pendingTasksWorkload = allTasks.filter((t) => t.status !== 'DONE').slice(0, 3)

  const handleCreateNewProject = () => {
    setFormOpen(true)
  }

  return (
    <div className="max-w-7xl mx-auto px-4 md:px-8 py-8 space-y-8 animate-in fade-in duration-500">
      
      {/* Title Header with Purple "+ New Project" Action button */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl font-outfit">
            Workspace Dashboard
          </h1>
          <p className="text-muted-foreground text-sm font-semibold mt-1">
            Real-time workspace analytics, upcoming deadlines, and sprint deliverables.
          </p>
        </div>
        <Button onClick={handleCreateNewProject} className="rounded-xl h-11 px-5 font-bold space-x-2 shrink-0 shadow-md">
          <Plus className="w-5 h-5" />
          <span>New Project</span>
        </Button>
      </div>

      {/* Grid of 5 Stats cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 font-outfit">
        {/* TOTAL PROJECTS */}
        <Card className="border border-border bg-card shadow-sm p-5 flex flex-col justify-between h-32 rounded-2xl">
          <CardContent className="p-0 flex flex-col justify-between h-full w-full">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                  Total Projects
                </span>
                <span className="text-3xl font-extrabold text-foreground mt-1 block">
                  {totalProjects}
                </span>
              </div>
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                <Folder className="w-5 h-5" />
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground truncate block font-semibold">
              Active proj...
            </span>
          </CardContent>
        </Card>

        {/* TOTAL TASKS */}
        <Card className="border border-border bg-card shadow-sm p-5 flex flex-col justify-between h-32 rounded-2xl">
          <CardContent className="p-0 flex flex-col justify-between h-full w-full">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                  Total Tasks
                </span>
                <span className="text-3xl font-extrabold text-foreground mt-1 block">
                  {totalTasks}
                </span>
              </div>
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                <ClipboardList className="w-5 h-5" />
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground truncate block font-semibold">
              Global act...
            </span>
          </CardContent>
        </Card>

        {/* COMPLETED TASKS */}
        <Card className="border border-border bg-card shadow-sm p-5 flex flex-col justify-between h-32 rounded-2xl">
          <CardContent className="p-0 flex flex-col justify-between h-full w-full">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                  Completed Tasks
                </span>
                <span className="text-3xl font-extrabold text-foreground mt-1 block">
                  {completedTasks}
                </span>
              </div>
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                <CheckCircle2 className="w-5 h-5" />
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground truncate block font-semibold">
              <span className="text-emerald-500 font-extrabold">{taskCompletionRate}%</span> Tasks...
            </span>
          </CardContent>
        </Card>

        {/* PENDING TASKS */}
        <Card className="border border-border bg-card shadow-sm p-5 flex flex-col justify-between h-32 rounded-2xl">
          <CardContent className="p-0 flex flex-col justify-between h-full w-full">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                  Pending Tasks
                </span>
                <span className="text-3xl font-extrabold text-foreground mt-1 block">
                  {pendingTasks}
                </span>
              </div>
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                <Clock className="w-5 h-5" />
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground truncate block font-semibold">
              Tasks pend...
            </span>
          </CardContent>
        </Card>

        {/* OVERDUE TASKS */}
        <Card className="border border-border bg-card shadow-sm p-5 flex flex-col justify-between h-32 rounded-2xl">
          <CardContent className="p-0 flex flex-col justify-between h-full w-full">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                  Overdue Tasks
                </span>
                <span className="text-3xl font-extrabold text-foreground mt-1 block">
                  {overdueTasks}
                </span>
              </div>
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                <AlertTriangle className="w-5 h-5" />
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground truncate block font-semibold">
              Past milest...
            </span>
          </CardContent>
        </Card>
      </div>

      {/* Main Grid: Columns for Analytics, Active Areas, and Sprint Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Productivity Engine */}
        <Card className="border border-border bg-card shadow-sm rounded-2xl overflow-hidden flex flex-col">
          <CardHeader className="pb-3 border-b border-border/40">
            <span className="text-[10px] text-primary uppercase font-extrabold tracking-wider flex items-center gap-1.5 font-outfit">
              <TrendingUp className="w-3.5 h-3.5" />
              Analytics Core
            </span>
            <h2 className="text-lg font-extrabold text-foreground mt-0.5 font-outfit">
              Productivity Engine
            </h2>
          </CardHeader>
          <CardContent className="pt-6 space-y-6 flex-1 flex flex-col justify-between">
            <div className="space-y-5">
              {/* Task Completion Rate Progress */}
              <div className="space-y-2">
                <div className="flex justify-between items-center text-xs">
                  <span className="font-bold text-slate-700 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5 text-primary" />
                    Task Completion Rate
                  </span>
                  <span className="font-extrabold text-primary">{taskCompletionRate}%</span>
                </div>
                <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
                  <div
                    className="bg-primary h-full rounded-full transition-all duration-500"
                    style={{ width: `${taskCompletionRate}%` }}
                  />
                </div>
              </div>

              {/* Project Completion Ratio */}
              <div className="space-y-2 pt-2">
                <div className="flex justify-between items-center text-xs">
                  <span className="font-bold text-slate-700 flex items-center gap-1">
                    <Folder className="w-3.5 h-3.5 text-primary" />
                    Project Completion Ratio
                  </span>
                  <span className="font-bold bg-slate-100 border border-border px-2 py-0.5 rounded-md text-foreground font-outfit">
                    {completedProjectsCount}/{totalProjects}
                  </span>
                </div>
                <p className="text-[11px] text-muted-foreground leading-normal font-semibold">
                  Ratio of completed projects against overall active projects.
                </p>
              </div>
            </div>

            {/* Pending Tasks Workload */}
            <div className="space-y-3 pt-4 border-t border-border/40 mt-4">
              <span className="text-xs text-slate-800 uppercase font-extrabold tracking-wider block font-outfit">
                Pending Tasks Workload
              </span>
              {pendingTasksWorkload.length === 0 ? (
                <p className="text-xs text-muted-foreground font-semibold">
                  No active pending tasks. Excellent work!
                </p>
              ) : (
                <div className="space-y-2">
                  {pendingTasksWorkload.map((task) => (
                    <div
                      key={task.id}
                      onClick={() => navigate(`/projects/${task.projectId}`)}
                      className="p-2.5 rounded-xl border border-border bg-muted/20 hover:bg-muted/40 cursor-pointer text-xs flex justify-between items-center transition-colors font-outfit"
                    >
                      <span className="truncate font-bold text-slate-800 pr-3">{task.title}</span>
                      <span className="text-[8px] uppercase font-extrabold px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-500 border border-amber-500/15 shrink-0">
                        {task.priority}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Recent Projects */}
        <Card className="border border-border bg-card shadow-sm rounded-2xl overflow-hidden">
          <CardHeader className="pb-3 border-b border-border/40">
            <span className="text-[10px] text-primary uppercase font-extrabold tracking-wider font-outfit">
              Active Areas
            </span>
            <h2 className="text-lg font-extrabold text-foreground mt-0.5 font-outfit">
              Recent Projects
            </h2>
          </CardHeader>
          <CardContent className="pt-6 space-y-3">
            {recentProjects.length === 0 ? (
              <div className="text-center py-8 text-xs text-muted-foreground border border-dashed border-border/60 rounded-xl">
                No active projects found.
              </div>
            ) : (
              recentProjects.map((project) => (
                <div
                  key={project.id}
                  onClick={() => navigate(`/projects/${project.id}`)}
                  className="flex items-center justify-between p-3.5 border border-border hover:border-primary/30 rounded-xl bg-card hover:bg-card/75 transition-all cursor-pointer shadow-sm"
                >
                  <div className="flex items-center space-x-3 min-w-0">
                    <div className="w-9 h-9 rounded-lg bg-primary/5 border border-primary/25 flex items-center justify-center text-primary shrink-0">
                      <Folder className="w-5 h-5" />
                    </div>
                    <div className="min-w-0 font-outfit">
                      <span className="text-xs font-bold text-foreground block truncate">
                        {project.name}
                      </span>
                      <span className="inline-block text-[8px] font-extrabold bg-primary/10 text-primary px-1.5 py-0.5 rounded uppercase mt-0.5">
                        {project.status || 'ACTIVE'}
                      </span>
                    </div>
                  </div>
                  <ChevronRight className="w-4 h-4 text-muted-foreground" />
                </div>
              ))
            )}
          </CardContent>
        </Card>

        {/* Recent Tasks */}
        <Card className="border border-border bg-card shadow-sm rounded-2xl overflow-hidden">
          <CardHeader className="pb-3 border-b border-border/40">
            <span className="text-[10px] text-primary uppercase font-extrabold tracking-wider font-outfit">
              Sprint Activity
            </span>
            <h2 className="text-lg font-extrabold text-foreground mt-0.5 font-outfit">
              Recent Tasks
            </h2>
          </CardHeader>
          <CardContent className="pt-6 space-y-3">
            {recentTasks.length === 0 ? (
              <div className="text-center py-12 text-xs text-muted-foreground font-semibold">
                No active deliverables found.
              </div>
            ) : (
              recentTasks.map((task) => (
                <div
                  key={task.id}
                  onClick={() => navigate(`/projects/${task.projectId}`)}
                  className="p-3 border border-border hover:border-primary/20 rounded-xl bg-card hover:bg-card/50 transition-all cursor-pointer shadow-xs flex flex-col justify-between gap-1 font-outfit"
                >
                  <div className="flex justify-between items-start gap-3">
                    <span className="text-xs font-bold text-foreground truncate pr-2">
                      {task.title}
                    </span>
                    <span
                      className={`text-[8px] font-extrabold uppercase px-1.5 py-0.5 rounded ${
                        task.status === 'DONE'
                          ? 'bg-emerald-500/10 text-emerald-500'
                          : 'bg-amber-500/10 text-amber-500'
                      }`}
                    >
                      {task.status.replace('_', ' ')}
                    </span>
                  </div>
                  <div className="flex justify-between items-center text-[10px] text-muted-foreground mt-1">
                    <span className="truncate max-w-[120px] font-semibold">{task.projectName}</span>
                    <span>Due: {task.dueDate ? new Date(task.dueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : 'None'}</span>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>

      {/* Milestones Card at bottom */}
      <Card className="border border-border bg-card shadow-sm rounded-2xl overflow-hidden">
        <CardHeader className="pb-3 border-b border-border/40">
          <span className="text-[10px] text-primary uppercase font-extrabold tracking-wider font-outfit">
            Milestones
          </span>
          <h2 className="text-lg font-extrabold text-foreground mt-0.5 font-outfit">
            Upcoming Deadlines
          </h2>
        </CardHeader>
        <CardContent className="pt-6">
          {upcomingDeadlines.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-10 text-center space-y-3">
              <AlertCircle className="w-10 h-10 text-muted-foreground/60 stroke-[1.2]" />
              <div className="space-y-0.5">
                <p className="text-xs font-bold text-foreground font-outfit">
                  No upcoming deadlines.
                </p>
                <p className="text-[11px] text-muted-foreground font-semibold">
                  All clear for active sprints!
                </p>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {upcomingDeadlines.map((task) => (
                <div
                  key={task.id}
                  onClick={() => navigate(`/projects/${task.projectId}`)}
                  className="p-4 border border-border hover:border-primary/30 rounded-xl bg-card hover:bg-card/50 transition-all cursor-pointer flex flex-col justify-between h-24 font-outfit"
                >
                  <div className="space-y-1 min-w-0">
                    <span className="text-[9px] font-bold text-primary block truncate font-semibold uppercase">
                      {task.projectName}
                    </span>
                    <h4 className="text-xs font-bold text-foreground truncate block">
                      {task.title}
                    </h4>
                  </div>
                  <div className="flex justify-between items-center text-[10px] text-muted-foreground pt-2 border-t border-border/40">
                    <span className="font-semibold">Due: {new Date(task.dueDate!).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                    <span
                      className={`text-[8px] font-extrabold uppercase px-1.5 py-0.5 rounded ${
                        task.priority === 'HIGH'
                          ? 'bg-red-500/10 text-red-500'
                          : 'bg-blue-500/10 text-blue-500'
                      }`}
                    >
                      {task.priority}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Project form modal popup */}
      <ProjectForm isOpen={formOpen} onClose={() => setFormOpen(false)} />
    </div>
  )
}

export default DashboardPage
