import React, { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, Calendar, User, FileText, Trash2, Edit2, Folder, MessageSquare, Paperclip } from 'lucide-react'
import { toast } from 'sonner'
import { taskApi } from '../api/taskApi'
import { useTasks } from '../hooks/useTasks'
import { projectApi } from '@/features/projects/api/projectApi'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { TaskStatusBadge } from '../components/TaskStatusBadge'
import { TaskPriorityBadge } from '../components/TaskPriorityBadge'
import { DeleteTaskDialog } from '../components/DeleteTaskDialog'
import type { TaskDto } from '../types/taskTypes'
import type { ProjectDto } from '@/features/projects/types/projectTypes'

export const TaskDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const taskId = id ? parseInt(id, 10) : NaN
  const navigate = useNavigate()

  const { deleteTask } = useTasks()

  const [task, setTask] = useState<TaskDto | null>(null)
  const [project, setProject] = useState<ProjectDto | null>(null)
  const [isFetching, setIsFetching] = useState(true)
  const [fetchError, setFetchError] = useState<string | null>(null)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  // Fetch task and project info on mount
  useEffect(() => {
    if (isNaN(taskId)) {
      setFetchError('Invalid task ID')
      setIsFetching(false)
      return
    }

    const loadData = async () => {
      try {
        const fetchedTask = await taskApi.getById(taskId)
        setTask(fetchedTask)

        // Load project reference details
        const fetchedProject = await projectApi.getById(fetchedTask.projectId)
        setProject(fetchedProject)
      } catch (e: any) {
        const msg = e.response?.data?.message || 'Failed to fetch task details.'
        setFetchError(msg)
        toast.error(msg)
      } finally {
        setIsFetching(false)
      }
    }

    loadData()
  }, [taskId])

  const handleDeleteConfirm = async () => {
    if (!task) return
    setIsDeleting(true)
    try {
      await deleteTask(task.id, task.projectId)
      toast.success('Task deleted successfully.')
      navigate(`/projects/${task.projectId}/tasks`)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to delete task.'
      toast.error(msg)
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }

  const formatDueDate = (epochMillis: number) => {
    if (!epochMillis) return 'No due date'
    return new Date(epochMillis).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Navigation Breadcrumb */}
      <div className="flex items-center justify-between">
        {task && (
          <Link
            to={`/projects/${task.projectId}/tasks`}
            className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group"
          >
            <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
            <span>Back to Project Taskboard</span>
          </Link>
        )}

        {task && (
          <div className="flex items-center space-x-2">
            <Link to={`/tasks/${task.id}/edit`}>
              <Button variant="outline" size="sm" className="flex items-center space-x-1.5 h-9">
                <Edit2 size={14} />
                <span className="hidden sm:inline">Edit Task</span>
              </Button>
            </Link>

            <Button
              variant="destructive"
              size="sm"
              className="flex items-center space-x-1.5 h-9"
              onClick={() => setShowDeleteDialog(true)}
            >
              <Trash2 size={14} />
              <span className="hidden sm:inline">Delete</span>
            </Button>
          </div>
        )}
      </div>

      {isFetching ? (
        // Premium Pulse Skeletons
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-pulse">
          <div className="lg:col-span-2 space-y-6">
            <div className="h-40 bg-card border border-border rounded-xl"></div>
            <div className="h-48 bg-card border border-border rounded-xl"></div>
          </div>
          <div className="h-48 bg-card border border-border rounded-xl"></div>
        </div>
      ) : (
        <>
          {fetchError && !task && (
            <Card className="border-destructive/20 bg-destructive/5 text-destructive p-6 text-center max-w-xl mx-auto shadow-md">
              <div className="font-bold text-lg mb-2">Failed to Load Task Details</div>
              <p className="text-sm font-medium text-destructive/80 mb-4">{fetchError}</p>
              <Link to="/projects">
                <Button variant="outline" size="sm">Return to Workspaces</Button>
              </Link>
            </Card>
          )}

          {task && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
              {/* Left Column: Core Info & Placeholders */}
              <div className="lg:col-span-2 space-y-6">
                {/* Task description card */}
                <Card className="border border-border/80 bg-card shadow-sm">
                  <CardHeader className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <TaskStatusBadge status={task.status} />
                      <TaskPriorityBadge priority={task.priority} />
                    </div>
                    <CardTitle className="text-2xl font-extrabold tracking-tight text-foreground">
                      {task.title}
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center space-x-2 text-primary font-semibold text-xs uppercase tracking-wider">
                      <FileText size={15} />
                      <span>Task Description</span>
                    </div>
                    <p className="text-muted-foreground text-sm leading-relaxed font-medium whitespace-pre-wrap">
                      {task.description || 'No description provided for this task.'}
                    </p>
                  </CardContent>
                </Card>

                {/* Prepared UI Area for Future Comments & Attachments */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  {/* Comments placeholder */}
                  <div className="bg-card border border-border/80 rounded-xl p-6 shadow-sm text-center space-y-3">
                    <div className="mx-auto w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                      <MessageSquare size={20} />
                    </div>
                    <h4 className="font-bold text-sm text-foreground">Task Discussions</h4>
                    <p className="text-muted-foreground text-xs font-medium leading-relaxed">
                      Discuss briefs, append checklists, or tag members inside this comment area soon.
                    </p>
                  </div>

                  {/* Attachments placeholder */}
                  <div className="bg-card border border-border/80 rounded-xl p-6 shadow-sm text-center space-y-3">
                    <div className="mx-auto w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                      <Paperclip size={20} />
                    </div>
                    <h4 className="font-bold text-sm text-foreground">Attachments & Links</h4>
                    <p className="text-muted-foreground text-xs font-medium leading-relaxed">
                      Upload system files, link Figma designs, or attach logs directly to this workspace soon.
                    </p>
                  </div>
                </div>
              </div>

              {/* Right Column: Sidebar Metadata */}
              <div className="space-y-6">
                <Card className="border border-border/80 bg-card shadow-sm">
                  <CardHeader className="pb-3 border-b border-border/50">
                    <CardTitle className="text-xs uppercase tracking-wider font-extrabold text-muted-foreground">
                      Task Meta Details
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="pt-4 space-y-4">
                    <div className="space-y-1">
                      <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                        Target Workspace
                      </span>
                      {project && (
                        <Link
                          to={`/projects/${project.id}/tasks`}
                          className="flex items-center space-x-2 text-primary font-bold hover:underline text-sm"
                        >
                          <Folder size={15} />
                          <span>{project.name}</span>
                        </Link>
                      )}
                    </div>

                    <div className="space-y-1">
                      <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                        Assigned Member
                      </span>
                      <div className="flex items-center space-x-2 text-foreground font-semibold text-sm">
                        <User size={15} className="text-primary/70" />
                        <span>{task.assignedTo || 'Unassigned'}</span>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <span className="text-[10px] text-muted-foreground uppercase font-extrabold tracking-wider block">
                        Due Date
                      </span>
                      <div className="flex items-center space-x-2 text-foreground font-semibold text-sm">
                        <Calendar size={15} className="text-primary/70" />
                        <span>{formatDueDate(task.dueDate)}</span>
                      </div>
                    </div>

                    <div className="space-y-1 pt-2 border-t border-border/50 text-[10px] text-muted-foreground font-medium">
                      <div>Created: {new Date(task.createdAt).toLocaleDateString()} by {task.createdBy}</div>
                      <div>Updated: {new Date(task.updatedAt).toLocaleDateString()}</div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          )}
        </>
      )}

      {/* Delete Confirmation Overlay Dialog */}
      <DeleteTaskDialog
        isOpen={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        onConfirm={handleDeleteConfirm}
        taskTitle={task?.title || ''}
        isLoading={isDeleting}
      />
    </div>
  )
}

export default TaskDetailsPage
