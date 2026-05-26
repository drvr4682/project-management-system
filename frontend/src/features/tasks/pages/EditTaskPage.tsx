import React, { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { taskApi } from '../api/taskApi'
import { useTasks } from '../hooks/useTasks'
import { projectApi } from '@/features/projects/api/projectApi'
import TaskForm from '../components/TaskForm'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card'
import type { TaskFormValues } from '../validations/taskSchemas'
import type { TaskDto } from '../types/taskTypes'
import type { ProjectDto } from '@/features/projects/types/projectTypes'

export const EditTaskPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const taskId = id ? parseInt(id, 10) : NaN

  const { updateTask } = useTasks()
  const navigate = useNavigate()

  const [task, setTask] = useState<TaskDto | null>(null)
  const [projects, setProjects] = useState<ProjectDto[]>([])
  const [isFetching, setIsFetching] = useState(true)
  const [isUpdating, setIsUpdating] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Fetch task details and projects list to populate form
  useEffect(() => {
    if (isNaN(taskId)) {
      setFormError('Invalid task ID')
      setIsFetching(false)
      return
    }

    const loadData = async () => {
      try {
        const fetchedTask = await taskApi.getById(taskId)
        setTask(fetchedTask)
        
        const response = await projectApi.getAll({ size: 100 })
        setProjects(response.content)
      } catch (e: any) {
        const msg = e.response?.data?.message || 'Failed to load task or projects.'
        setFormError(msg)
        toast.error(msg)
      } finally {
        setIsFetching(false)
      }
    }

    loadData()
  }, [taskId])

  const handleFormSubmit = async (values: TaskFormValues) => {
    setIsUpdating(true)
    setFormError(null)
    try {
      const isoDate = values.dueDate ? `${values.dueDate}:00` : undefined

      await updateTask(taskId, {
        title: values.title,
        description: values.description || '',
        status: values.status,
        priority: values.priority,
        dueDate: isoDate,
        projectId: values.projectId,
      })

      toast.success('Task updated successfully!')
      navigate(`/projects/${values.projectId}/tasks`)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to update task.'
      setFormError(msg)
      toast.error(msg)
    } finally {
      setIsUpdating(false)
    }
  }

  // Convert milliseconds epoch to "YYYY-MM-DDTHH:MM" local format
  const formatEpochToLocalInput = (epochMillis: number) => {
    if (!epochMillis) return ''
    const d = new Date(epochMillis)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
  }

  return (
    <div className="max-w-2xl mx-auto space-y-4 animate-in fade-in duration-300">
      {/* Back button */}
      <div>
        <Link
          to={task ? `/projects/${task.projectId}/tasks` : '/projects'}
          className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group mb-2"
        >
          <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
          <span>Back to Task Board</span>
        </Link>
      </div>

      <Card className="border border-border/80 bg-card shadow-lg">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-extrabold tracking-tight">Edit Task</CardTitle>
          <CardDescription className="text-muted-foreground">
            Modify task titles, descriptions, due dates, and priorities
          </CardDescription>
        </CardHeader>

        <CardContent className="pt-2">
          {isFetching ? (
            <div className="space-y-5 animate-pulse">
              <div className="space-y-2">
                <div className="h-4 w-28 bg-muted rounded-md"></div>
                <div className="h-10 w-full bg-muted rounded-md"></div>
              </div>
              <div className="space-y-2">
                <div className="h-4 w-20 bg-muted rounded-md"></div>
                <div className="h-28 w-full bg-muted rounded-md"></div>
              </div>
              <div className="h-10 w-full bg-muted rounded-md"></div>
            </div>
          ) : (
            <>
              {formError && !task && (
                <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm font-semibold mb-4">
                  {formError}
                </div>
              )}

              {task && (
                <TaskForm
                  onSubmit={handleFormSubmit}
                  defaultValues={{
                    title: task.title,
                    description: task.description || '',
                    status: task.status,
                    priority: task.priority,
                    dueDate: formatEpochToLocalInput(task.dueDate),
                    projectId: task.projectId,
                  }}
                  isLoading={isUpdating}
                  submitLabel="Save Changes"
                  projects={projects}
                  disableProjectSelect={true} // Lock project association on edit to match backend lifecycle
                />
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default EditTaskPage
