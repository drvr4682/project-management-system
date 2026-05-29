import React, { useState, useEffect } from 'react'
import { useAppDispatch } from '@/hooks/store'
import { createTask, updateTask, assignTask, unassignTask } from '../store/taskSlice'
import { type ProjectMemberResponseDTO } from '@/features/projects/api/projectApi'
import profileApi, { type UserProfileResponse } from '@/features/auth/api/profileApi'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import { X, Calendar, User, AlignLeft } from 'lucide-react'
import { toast } from 'sonner'

interface TaskFormProps {
  isOpen: boolean
  onClose: () => void
  projectId: number
  projectMembers: ProjectMemberResponseDTO[]
  initialData?: {
    id: number
    title: string
    description: string
    status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
    dueDate: number | null // timestamp
    assignedTo: string | null // UUID
  }
}

export const TaskForm: React.FC<TaskFormProps> = ({
  isOpen,
  onClose,
  projectId,
  projectMembers,
  initialData,
}) => {
  const dispatch = useAppDispatch()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState('TODO')
  const [priority, setPriority] = useState('MEDIUM')
  const [dueDate, setDueDate] = useState('')
  const [assignedTo, setAssignedTo] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Cache to store resolved member profile details
  const [memberProfiles, setMemberProfiles] = useState<Record<string, UserProfileResponse>>({})

  // Resolve member profiles when modal opens
  useEffect(() => {
    if (!isOpen) return

    const resolveProfiles = async () => {
      const pendingIds = projectMembers.map((m) => m.userId).filter((id) => !memberProfiles[id])
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

        const newProfiles = { ...memberProfiles }
        resolved.forEach((item) => {
          if (item.profile) {
            newProfiles[item.id] = item.profile
          }
        })
        setMemberProfiles(newProfiles)
      } catch (err) {
        console.error('Failed to resolve member profiles for dropdown', err)
      }
    }

    resolveProfiles()
  }, [isOpen, projectMembers])

  useEffect(() => {
    if (initialData) {
      setTitle(initialData.title)
      setDescription(initialData.description || '')
      setStatus(initialData.status)
      setPriority(initialData.priority)
      setAssignedTo(initialData.assignedTo || '')
      
      if (initialData.dueDate) {
        const dateObj = new Date(initialData.dueDate)
        const year = dateObj.getFullYear()
        const month = String(dateObj.getMonth() + 1).padStart(2, '0')
        const day = String(dateObj.getDate()).padStart(2, '0')
        setDueDate(`${year}-${month}-${day}`)
      } else {
        setDueDate('')
      }
    } else {
      setTitle('')
      setDescription('')
      setStatus('TODO')
      setPriority('MEDIUM')
      setAssignedTo('')
      setDueDate('')
    }
  }, [initialData, isOpen])

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) {
      toast.error('Task Title is required')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        title,
        description,
        status,
        priority,
        dueDate: dueDate ? new Date(dueDate).toISOString() : null,
        projectId,
      }

      let savedTask
      if (initialData) {
        savedTask = await dispatch(updateTask({ taskId: initialData.id, payload })).unwrap()
        
        // Handle assignee separately if changed
        if (assignedTo !== (initialData.assignedTo || '')) {
          if (assignedTo) {
            await dispatch(assignTask({ taskId: initialData.id, assigneeId: assignedTo, projectId })).unwrap()
          } else {
            await dispatch(unassignTask({ taskId: initialData.id, projectId })).unwrap()
          }
        }
        
        toast.success('Task updated successfully')
      } else {
        savedTask = await dispatch(createTask(payload)).unwrap()
        
        // Auto assign on creation if set
        if (assignedTo && savedTask?.id) {
          await dispatch(assignTask({ taskId: savedTask.id, assigneeId: assignedTo, projectId })).unwrap()
        }
        
        toast.success('Task created successfully')
      }
      onClose()
    } catch (err: any) {
      toast.error(err || 'Action failed')
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
            {initialData ? 'Edit Task Details' : 'Create Task'}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-muted-foreground hover:bg-muted transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
          <div className="space-y-1.5">
            <Label htmlFor="taskTitle">Task Title</Label>
            <Input
              id="taskTitle"
              placeholder="e.g. Implement user database indexing"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="taskDescription">Description</Label>
            <div className="relative">
              <textarea
                id="taskDescription"
                placeholder="Detail the technical implementation steps..."
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-xl border border-border bg-card/50 px-3 py-2 pl-9 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200 resize-none"
              />
              <AlignLeft className="w-4 h-4 text-muted-foreground absolute left-3 top-3" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="taskStatus">Status</Label>
              <select
                id="taskStatus"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full h-11 px-3 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
              >
                <option value="TODO">TODO</option>
                <option value="IN_PROGRESS">IN PROGRESS</option>
                <option value="DONE">DONE</option>
                <option value="BLOCKED">BLOCKED</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="taskPriority">Priority</Label>
              <select
                id="taskPriority"
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full h-11 px-3 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="taskDueDate">Due Date</Label>
            <div className="relative">
              <input
                type="date"
                id="taskDueDate"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full h-11 px-3 pl-10 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
              />
              <Calendar className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="taskAssignee">Assignee</Label>
            <div className="relative">
              <select
                id="taskAssignee"
                value={assignedTo}
                onChange={(e) => setAssignedTo(e.target.value)}
                className="w-full h-11 px-3 pl-10 rounded-xl border border-border bg-card/50 text-sm text-foreground focus:outline-none focus:border-primary transition-all duration-200"
              >
                <option value="">Unassigned</option>
                {projectMembers.map((member) => {
                  const profile = memberProfiles[member.userId]
                  const nameStr = profile
                    ? `${profile.firstName} ${profile.surname || ''}`.trim() + ` (@${profile.username})`
                    : `User ${member.userId.substring(0, 8)}...`
                  return (
                    <option key={member.userId} value={member.userId}>
                      {nameStr} ({member.role})
                    </option>
                  )
                })}
              </select>
              <User className="w-4 h-4 text-muted-foreground absolute left-3.5 top-3.5" />
            </div>
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-border/60 mt-6">
            <Button type="button" variant="outline" onClick={onClose} className="rounded-xl h-10 font-bold">
              Cancel
            </Button>
            <Button type="submit" disabled={submitting} className="rounded-xl h-10 font-bold px-6">
              {submitting ? 'Saving...' : initialData ? 'Save Changes' : 'Create Task'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
