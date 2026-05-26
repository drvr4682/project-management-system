import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { useProjects } from '../hooks/useProjects'
import ProjectForm from '../components/ProjectForm'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card'
import type { ProjectFormValues } from '../validations/projectSchemas'

export const CreateProjectPage: React.FC = () => {
  const { createProject } = useProjects()
  const navigate = useNavigate()
  const [isLoading, setIsLoading] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const handleFormSubmit = async (values: ProjectFormValues) => {
    setIsLoading(true)
    setFormError(null)
    try {
      await createProject({
        name: values.name,
        description: values.description || undefined,
        status: values.status,
      })
      toast.success('Project created successfully!')
      navigate('/projects')
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Failed to create project.'
      setFormError(msg)
      toast.error(msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-4">
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
          <CardTitle className="text-2xl font-extrabold tracking-tight">Create New Project</CardTitle>
          <CardDescription className="text-muted-foreground">
            Spin up a new workspace to organize task boards and collaborate
          </CardDescription>
        </CardHeader>

        <CardContent className="pt-2">
          {formError && (
            <div className="mb-4 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm font-semibold">
              {formError}
            </div>
          )}

          <ProjectForm
            onSubmit={handleFormSubmit}
            isLoading={isLoading}
            submitLabel="Create Project"
          />
        </CardContent>
      </Card>
    </div>
  )
}

export default CreateProjectPage
