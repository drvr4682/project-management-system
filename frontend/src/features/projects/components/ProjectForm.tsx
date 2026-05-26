import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { projectSchema, type ProjectFormValues } from '../validations/projectSchemas'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'

interface ProjectFormProps {
  onSubmit: (values: ProjectFormValues) => Promise<void>
  defaultValues?: Partial<ProjectFormValues>
  isLoading: boolean
  submitLabel: string
}

export const ProjectForm: React.FC<ProjectFormProps> = ({
  onSubmit,
  defaultValues = { name: '', description: '', status: 'ACTIVE' },
  isLoading,
  submitLabel,
}) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProjectFormValues>({
    resolver: zodResolver(projectSchema),
    defaultValues,
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="name">Project Name</Label>
        <Input
          id="name"
          type="text"
          placeholder="Acme Workspace, Marketing Campaign..."
          error={!!errors.name}
          disabled={isLoading}
          {...register('name')}
        />
        {errors.name && (
          <p className="text-xs text-destructive font-semibold">{errors.name.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <textarea
          id="description"
          placeholder="Briefly describe the goals, timeline, or scope of this project..."
          disabled={isLoading}
          className="flex min-h-[120px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 text-foreground"
          {...register('description')}
        />
        {errors.description && (
          <p className="text-xs text-destructive font-semibold">{errors.description.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="status">Project Status</Label>
        <select
          id="status"
          disabled={isLoading}
          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          {...register('status')}
        >
          <option value="ACTIVE">Active</option>
          <option value="COMPLETED">Completed</option>
          <option value="ARCHIVED">Archived</option>
        </select>
        {errors.status && (
          <p className="text-xs text-destructive font-semibold">{errors.status.message}</p>
        )}
      </div>

      <Button type="submit" className="w-full" isLoading={isLoading}>
        {submitLabel}
      </Button>
    </form>
  )
}

export default ProjectForm
