import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { taskSchema, type TaskFormValues } from '../validations/taskSchemas'
import { Label } from '@/components/ui/Label'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'

interface TaskFormProps {
  onSubmit: (values: TaskFormValues) => Promise<void>
  defaultValues?: Partial<TaskFormValues>
  isLoading: boolean
  submitLabel: string
  projects: { id: number; name: string }[]
  disableProjectSelect?: boolean
}

export const TaskForm: React.FC<TaskFormProps> = ({
  onSubmit,
  defaultValues = {
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
    dueDate: '',
    projectId: undefined,
  },
  isLoading,
  submitLabel,
  projects,
  disableProjectSelect = false,
}) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TaskFormValues>({
    resolver: zodResolver(taskSchema),
    defaultValues: {
      title: defaultValues.title || '',
      description: defaultValues.description || '',
      status: defaultValues.status || 'TODO',
      priority: defaultValues.priority || 'MEDIUM',
      dueDate: defaultValues.dueDate ? defaultValues.dueDate.substring(0, 16) : '', // Format for datetime-local
      projectId: defaultValues.projectId,
    },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="projectId">Associated Project</Label>
        <select
          id="projectId"
          disabled={isLoading || disableProjectSelect}
          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-75 disabled:cursor-not-allowed"
          {...register('projectId', { valueAsNumber: true })}
        >
          <option value="">Select a project...</option>
          {projects.map((project) => (
            <option key={project.id} value={project.id}>
              {project.name}
            </option>
          ))}
        </select>
        {errors.projectId && (
          <p className="text-xs text-destructive font-semibold">{errors.projectId.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="title">Task Title</Label>
        <Input
          id="title"
          type="text"
          placeholder="Refactor auth middleware, design landing page..."
          error={!!errors.title}
          disabled={isLoading}
          {...register('title')}
        />
        {errors.title && (
          <p className="text-xs text-destructive font-semibold">{errors.title.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <textarea
          id="description"
          placeholder="Provide clear technical briefs, checkboxes, or details for this task..."
          disabled={isLoading}
          className="flex min-h-[120px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 text-foreground"
          {...register('description')}
        />
        {errors.description && (
          <p className="text-xs text-destructive font-semibold">{errors.description.message}</p>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="status">Task Status</Label>
          <select
            id="status"
            disabled={isLoading}
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            {...register('status')}
          >
            <option value="TODO">To Do</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="DONE">Completed</option>
            <option value="BLOCKED">Blocked</option>
          </select>
          {errors.status && (
            <p className="text-xs text-destructive font-semibold">{errors.status.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="priority">Priority Level</Label>
          <select
            id="priority"
            disabled={isLoading}
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            {...register('priority')}
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
          {errors.priority && (
            <p className="text-xs text-destructive font-semibold">{errors.priority.message}</p>
          )}
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="dueDate">Due Date & Time</Label>
        <Input
          id="dueDate"
          type="datetime-local"
          error={!!errors.dueDate}
          disabled={isLoading}
          {...register('dueDate')}
        />
        {errors.dueDate && (
          <p className="text-xs text-destructive font-semibold">{errors.dueDate.message}</p>
        )}
      </div>

      <Button type="submit" className="w-full" isLoading={isLoading}>
        {submitLabel}
      </Button>
    </form>
  )
}

export default TaskForm
