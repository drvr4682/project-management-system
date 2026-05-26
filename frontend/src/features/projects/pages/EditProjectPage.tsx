import React, { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { projectApi } from '../api/projectApi'
import { useProjects } from '../hooks/useProjects'
import ProjectForm from '../components/ProjectForm'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card'
import type { ProjectFormValues } from '../validations/projectSchemas'
import type { ProjectDto } from '../types/projectTypes'

export const EditProjectPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const projectId = id ? parseInt(id, 10) : NaN

  const { updateProject } = useProjects()
  const navigate = useNavigate()

  const [project, setProject] = useState<ProjectDto | null>(null)
  const [isFetching, setIsFetching] = useState(true)
  const [isUpdating, setIsUpdating] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Fetch project details to hydrate form on mount
  useEffect(() => {
    if (isNaN(projectId)) {
      setFormError('Invalid project ID')
      setIsFetching(false)
      return
    }

    const fetchProject = async () => {
      try {
        const fetched = await projectApi.getById(projectId)
        setProject(fetched)
      } catch (e: any) {
        const msg = e.response?.data?.message || 'Failed to load project details.'
        setFormError(msg)
        toast.error(msg)
      } finally {
        setIsFetching(false)
      }
    }

    fetchProject()
  }, [projectId])

  const handleFormSubmit = async (values: ProjectFormValues) => {
    setIsUpdating(true)
    setFormError(null)
    try {
      await updateProject(projectId, {
        name: values.name,
        description: values.description || '',
        status: values.status,
      })
      toast.success('Project updated successfully!')
      navigate('/projects')
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to update project.'
      setFormError(msg)
      toast.error(msg)
    } finally {
      setIsUpdating(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-4 animate-in fade-in duration-300">
      {/* Back navigation */}
      <div>
        <Link
          to="/projects"
          className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group mb-2"
        >
          <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
          <span>Back to Projects</span>
        </Link>
      </div>

      <Card className="border border-border/80 bg-card shadow-lg">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-extrabold tracking-tight">Edit Project</CardTitle>
          <CardDescription className="text-muted-foreground">
            Modify workspace titles, descriptions, and active statuses
          </CardDescription>
        </CardHeader>

        <CardContent className="pt-2">
          {isFetching ? (
            <div className="space-y-5 animate-pulse">
              <div className="space-y-2">
                <div className="h-4 w-24 bg-muted rounded-md"></div>
                <div className="h-10 w-full bg-muted rounded-md"></div>
              </div>
              <div className="space-y-2">
                <div className="h-4 w-20 bg-muted rounded-md"></div>
                <div className="h-28 w-full bg-muted rounded-md"></div>
              </div>
              <div className="space-y-2">
                <div className="h-4 w-28 bg-muted rounded-md"></div>
                <div className="h-10 w-full bg-muted rounded-md"></div>
              </div>
              <div className="h-10 w-full bg-muted rounded-md"></div>
            </div>
          ) : (
            <>
              {formError && !project && (
                <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm font-semibold mb-4">
                  {formError}
                </div>
              )}

              {project && (
                <ProjectForm
                  onSubmit={handleFormSubmit}
                  defaultValues={{
                    name: project.name,
                    description: project.description || '',
                    status: project.status,
                  }}
                  isLoading={isUpdating}
                  submitLabel="Save Changes"
                />
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default EditProjectPage
