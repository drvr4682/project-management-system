import React, { useState, useEffect } from 'react'
import { useAppDispatch } from '@/hooks/store'
import { createProject, updateProject } from '../store/projectSlice'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import { X } from 'lucide-react'
import { toast } from 'sonner'

interface ProjectFormProps {
  isOpen: boolean
  onClose: () => void
  initialData?: {
    id: number
    name: string
    description: string
    status: string
  }
}

export const ProjectForm: React.FC<ProjectFormProps> = ({ isOpen, onClose, initialData }) => {
  const dispatch = useAppDispatch()
  
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState('ACTIVE')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (initialData) {
      setName(initialData.name)
      setDescription(initialData.description)
      setStatus(initialData.status || 'ACTIVE')
    } else {
      setName('')
      setDescription('')
      setStatus('ACTIVE')
    }
  }, [initialData, isOpen])

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      toast.error('Project Name is required')
      return
    }

    setSubmitting(true)
    try {
      if (initialData) {
        await dispatch(
          updateProject({
            id: initialData.id,
            payload: { name, description, status },
          })
        ).unwrap()
        toast.success('Project updated successfully')
      } else {
        await dispatch(
          createProject({
            name,
            description,
            status,
          })
        ).unwrap()
        toast.success('Project created successfully')
      }
      onClose()
    } catch (error: any) {
      toast.error(error || 'Action failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-in fade-in duration-300">
      <div 
        className="w-full max-w-lg bg-card border border-border/80 rounded-2xl shadow-xl overflow-hidden animate-in zoom-in-95 duration-200"
        role="dialog"
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-border/60">
          <h3 className="text-lg font-bold text-foreground">
            {initialData ? 'Edit Project Workspace' : 'Create Project Workspace'}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="projectName">Project Name</Label>
            <Input
              id="projectName"
              placeholder="e.g. DRVR PMS Refactoring"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="projectDescription">Description</Label>
            <textarea
              id="projectDescription"
              placeholder="Provide a high-level summary of goals..."
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-xl border border-border bg-card/50 px-3 py-2 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200 resize-none"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="projectStatus">Status</Label>
            <select
              id="projectStatus"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-11 px-3 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="ARCHIVED">ARCHIVED</option>
            </select>
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-border/60 mt-6">
            <Button type="button" variant="outline" onClick={onClose} className="rounded-xl h-10 font-bold">
              Cancel
            </Button>
            <Button type="submit" disabled={submitting} className="rounded-xl h-10 font-bold px-6">
              {submitting ? 'Processing...' : initialData ? 'Save Changes' : 'Create Workspace'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
