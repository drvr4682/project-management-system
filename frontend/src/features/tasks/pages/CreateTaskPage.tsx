import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { useTasks } from '../hooks/useTasks'
import { projectApi } from '@/features/projects/api/projectApi'
import TaskForm from '../components/TaskForm'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card'
import type { TaskFormValues } from '../validations/taskSchemas'
import type { ProjectDto } from '@/features/projects/types/projectTypes'

export const CreateTaskPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const defaultProjectId = searchParams.get('projectId')
  const parsedDefaultProjectId = defaultProjectId ? parseInt(defaultProjectId, 10) : undefined

  const { createTask } = useTasks()
  const navigate = useNavigate()

  const [projects, setProjects] = useState<ProjectDto[]>([])
  const [isFetchingProjects, setIsFetchingProjects] = useState(true)
  const [isCreating, setIsCreating] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Fetch all user projects to populate dropdown on mount
  useEffect(() => {
    const loadProjects = async () => {
      try {
        const response = await projectApi.getAll({ size: 100 })
        setProjects(response.content)
      } catch (e: any) {
        toast.error('Failed to load projects list.')
      } finally {
        setIsFetchingProjects(false)
      }
    }

    loadProjects()
  }, [])

  const handleFormSubmit = async (values: TaskFormValues) => {
    setIsCreating(true)
    setFormError(null)
    try {
      // Convert HTML datetime-local string to LocalDateTime ISO format: "YYYY-MM-DDTHH:mm:ss"
      // e.g. "2026-05-26T14:30" -> "2026-05-26T14:30:00"
      const isoDate = values.dueDate ? `${values.dueDate}:00` : undefined

      await createTask({
        title: values.title,
        description: values.description || undefined,
        status: values.status,
        priority: values.priority,
        dueDate: isoDate,
        projectId: values.projectId,
      })

      toast.success('Task created successfully!')
      
      // Redirect back to project's task board
      navigate(`/projects/${values.projectId}/tasks`)
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to create task.'
      setFormError(msg)
      toast.error(msg)
    } finally {
      setIsCreating(false)
    }
  }

  // Get back button routing
  const getBackLink = () => {
    return parsedDefaultProjectId ? `/projects/${parsedDefaultProjectId}/tasks` : '/projects'
  }

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      {/* Back button */}
      <div>
        <Link
          to={getBackLink()}
          className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group mb-2"
        >
          <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
          <span>Back to Workspace</span>
        </Link>
      </div>

      <Card className="border border-border/80 bg-card shadow-lg animate-in fade-in duration-300">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-extrabold tracking-tight">Create New Task</CardTitle>
          <CardDescription className="text-muted-foreground">
            Outline a single deliverable, assigning priorities and deadlines
          </CardDescription>
        </CardHeader>

        <CardContent className="pt-2">
          {isFetchingProjects ? (
            <div className="space-y-5 animate-pulse">
              <div className="space-y-2">
                <div className="h-4 w-28 bg-muted rounded-md"></div>
                <div className="h-10 w-full bg-muted rounded-md"></div>
              </div>
              <div className="space-y-2">
                <div className="h-4 w-24 bg-muted rounded-md"></div>
                <div className="h-10 w-full bg-muted rounded-md"></div>
              </div>
            </div>
          ) : (
            <>
              {formError && (
                <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm font-semibold mb-4">
                  {formError}
                </div>
              )}

              <TaskForm
                onSubmit={handleFormSubmit}
                defaultValues={{
                  projectId: parsedDefaultProjectId,
                }}
                isLoading={isCreating}
                submitLabel="Create Task"
                projects={projects}
                disableProjectSelect={!!parsedDefaultProjectId}
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default CreateTaskPage
