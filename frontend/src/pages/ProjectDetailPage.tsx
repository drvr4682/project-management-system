import React, { useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/hooks/store'
import {
  fetchProjectById,
  fetchProjectMembers,
  removeProjectMember,
  clearActiveProject,
  selectActiveProject,
  selectProjectMembers,
  selectProjectLoading,
  selectProjectError,
} from '@/features/projects/store/projectSlice'
import {
  fetchTasks,
  updateTaskStatus,
  deleteTask,
  resetTaskState,
  selectGroupedTasks,
} from '@/features/tasks/store/taskSlice'
import profileApi, { type UserProfileResponse } from '@/features/auth/api/profileApi'
import { ProjectForm } from '@/features/projects/components/ProjectForm'
import { MemberForm } from '@/features/projects/components/MemberForm'
import { TaskForm } from '@/features/tasks/components/TaskForm'
import { Button } from '@/components/ui/Button'
import { Card, CardHeader, CardContent } from '@/components/ui/Card'
import {
  Briefcase,
  Users,
  KanbanSquare,
  Plus,
  Edit2,
  Trash2,
  UserPlus,
  Calendar,
  AlertTriangle,
  UserMinus,
  HelpCircle,
} from 'lucide-react'
import { toast } from 'sonner'
import { selectAuth } from '@/features/auth/store/authSlice'

export const ProjectDetailPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>()
  const pId = Number(projectId)
  
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const queryTab = searchParams.get('tab')
  
  const { user: currentUser } = useAppSelector(selectAuth)
  const activeProject = useAppSelector(selectActiveProject)
  const members = useAppSelector(selectProjectMembers)
  const projectLoading = useAppSelector(selectProjectLoading)
  const projectError = useAppSelector(selectProjectError)

  const groupedTasks = useAppSelector(selectGroupedTasks)

  const [activeTab, setActiveTab] = useState<'overview' | 'kanban' | 'members'>('overview')

  // Sync activeTab with tab URL query parameter
  useEffect(() => {
    if (queryTab === 'kanban' || queryTab === 'members' || queryTab === 'overview') {
      setActiveTab(queryTab)
    }
  }, [queryTab])
  const [projectFormOpen, setProjectFormOpen] = useState(false)
  const [memberFormOpen, setMemberFormOpen] = useState(false)
  const [taskFormOpen, setTaskFormOpen] = useState(false)
  const [selectedTask, setSelectedTask] = useState<any | null>(null)

  // Local state cache for resolved user profile details
  const [resolvedProfiles, setResolvedProfiles] = useState<Record<string, UserProfileResponse>>({})

  // 1. Reset tasks and fetch project details on mount / projectId change
  useEffect(() => {
    if (!isNaN(pId)) {
      // Prevent cross-project data bleed by resetting task slice immediately
      dispatch(resetTaskState())
      dispatch(clearActiveProject())
      
      dispatch(fetchProjectById(pId))
      dispatch(fetchProjectMembers(pId))
      dispatch(fetchTasks(pId))
    }
    
    return () => {
      dispatch(clearActiveProject())
    }
  }, [pId, dispatch])

  // 2. Fetch and resolve member usernames / names in background
  useEffect(() => {
    if (members.length === 0) return

    const resolveMemberProfiles = async () => {
      const pendingIds = members.map((m) => m.userId).filter((id) => !resolvedProfiles[id])
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

        const updated = { ...resolvedProfiles }
        resolved.forEach((item) => {
          if (item.profile) {
            updated[item.id] = item.profile
          }
        })
        setResolvedProfiles(updated)
      } catch (err) {
        console.error('Failed to resolve member profile names', err)
      }
    }

    resolveMemberProfiles()
  }, [members, resolvedProfiles])

  // 3. Resolve assignees of currently fetched tasks in background
  const taskItems = useAppSelector((state) => state.tasks.items)
  useEffect(() => {
    if (taskItems.length === 0) return

    const resolveTaskAssignees = async () => {
      const pendingIds = taskItems
        .map((t) => t.assignedTo)
        .filter((id): id is string => !!id && !resolvedProfiles[id])
      
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

        const updated = { ...resolvedProfiles }
        resolved.forEach((item) => {
          if (item.profile) {
            updated[item.id] = item.profile
          }
        })
        setResolvedProfiles(updated)
      } catch (err) {
        console.error('Failed to resolve task assignee profiles', err)
      }
    }

    resolveTaskAssignees()
  }, [taskItems, resolvedProfiles])

  const handleRemoveMember = async (userId: string) => {
    if (window.confirm('Are you sure you want to remove this collaborator from the project?')) {
      try {
        await dispatch(removeProjectMember({ projectId: pId, userId })).unwrap()
        toast.success('Collaborator removed successfully')
      } catch (err: any) {
        toast.error(err || 'Failed to remove collaborator')
      }
    }
  }

  const handleStatusChange = async (task: any, newStatus: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED') => {
    if (task.status === newStatus) return
    const prevStatus = task.status
    try {
      // Dispatches optimistic status transition and triggers PUT API. Rolls back on catch block.
      await dispatch(
        updateTaskStatus({
          taskId: task.id,
          status: newStatus,
          previousStatus: prevStatus,
          projectId: pId,
          taskData: task,
        })
      ).unwrap()
      toast.success(`Task moved to ${newStatus}`)
    } catch (err: any) {
      toast.error(err || 'Failed to transition task status')
    }
  }

  const handleDeleteTask = async (taskId: number) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await dispatch(deleteTask({ taskId, projectId: pId })).unwrap()
        toast.success('Task deleted successfully')
      } catch (err: any) {
        toast.error(err || 'Failed to delete task')
      }
    }
  }

  const openEditTask = (task: any) => {
    setSelectedTask(task)
    setTaskFormOpen(true)
  }

  const openCreateTask = () => {
    setSelectedTask(null)
    setTaskFormOpen(true)
  }

  // Outfit initials calculator helper
  const getInitials = (userId?: string | null) => {
    if (!userId) return '?'
    const profile = resolvedProfiles[userId]
    if (profile) {
      return `${profile.firstName?.[0] || ''}${profile.surname?.[0] || ''}`.toUpperCase()
    }
    return 'U'
  }

  const getFullName = (userId?: string | null) => {
    if (!userId) return 'Unassigned'
    const profile = resolvedProfiles[userId]
    if (profile) {
      return `${profile.firstName} ${profile.surname || ''}`.trim()
    }
    return `User ${userId.substring(0, 8)}...`
  }

  // Count stats
  const totalTasks = taskItems.length
  const completedTasks = taskItems.filter(t => t.status === 'DONE').length
  const pendingTasks = totalTasks - completedTasks

  // Loading skeleton screen
  if (projectLoading && !activeProject) {
    return (
      <div className="max-w-7xl mx-auto px-4 md:px-8 py-8 space-y-8 animate-in fade-in">
        <div className="space-y-4">
          <div className="h-4 w-28 bg-muted rounded-md animate-pulse" />
          <div className="h-10 w-2/3 bg-muted rounded-md animate-pulse" />
          <div className="h-4 w-1/2 bg-muted rounded-md animate-pulse" />
        </div>
        <div className="h-12 w-96 bg-muted rounded-xl animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-96 rounded-2xl bg-card animate-pulse border border-border" />
          <div className="h-96 rounded-2xl bg-card animate-pulse border border-border" />
        </div>
      </div>
    )
  }

  // Error screen
  if (projectError) {
    return (
      <div className="max-w-xl mx-auto px-4 py-16 text-center space-y-4">
        <div className="flex justify-center text-red-500">
          <AlertTriangle className="w-16 h-16" />
        </div>
        <h2 className="text-2xl font-bold text-foreground">Workspace Error</h2>
        <p className="text-sm text-muted-foreground">{projectError}</p>
        <Button onClick={() => navigate('/')} variant="outline" className="rounded-xl font-bold">
          Back to Dashboard
        </Button>
      </div>
    )
  }

  if (!activeProject) return null

  // Check if current user is admin/owner
  const isOwner = currentUser?.id === activeProject.owner || currentUser?.role === 'ADMIN'

  return (
    <div className="max-w-7xl mx-auto px-4 md:px-8 py-8 space-y-6 animate-in fade-in duration-300">
      
      {/* Workspace Header Info */}
      <div className="flex flex-col md:flex-row md:items-start justify-between gap-4 border-b border-border/40 pb-6">
        <div className="space-y-2">
          <div className="flex items-center space-x-3">
            <h1 className="text-3xl font-extrabold tracking-tight text-foreground font-outfit">
              {activeProject.name}
            </h1>
            <span
              className={`text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-full ${
                activeProject.status === 'ACTIVE'
                  ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/10'
                  : activeProject.status === 'IN_PROGRESS'
                    ? 'bg-amber-500/10 text-amber-500 border border-amber-500/10'
                    : 'bg-muted text-muted-foreground'
              }`}
            >
              {activeProject.status}
            </span>
          </div>
          <p className="text-muted-foreground text-sm font-semibold max-w-2xl leading-relaxed">
            {activeProject.description || 'No description provided.'}
          </p>
        </div>

        {isOwner && (
          <div className="flex items-center space-x-2 shrink-0">
            <Button
              variant="outline"
              onClick={() => setProjectFormOpen(true)}
              className="rounded-xl h-10 font-bold space-x-1.5"
            >
              <Edit2 className="w-4 h-4" />
              <span>Edit Workspace</span>
            </Button>
          </div>
        )}
      </div>

      {/* Tabs list navigation */}
      <div className="flex border-b border-border">
        {[
          { id: 'overview', name: 'Overview', icon: Briefcase },
          { id: 'kanban', name: 'Kanban Board', icon: KanbanSquare },
          { id: 'members', name: 'Collaborators', icon: Users },
        ].map((tab) => {
          const Icon = tab.icon
          const isActive = activeTab === tab.id
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center space-x-2 px-6 py-3 border-b-2 font-bold text-sm transition-all duration-200 ${
                isActive
                  ? 'border-primary text-primary bg-primary/5 rounded-t-xl'
                  : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
              }`}
            >
              <Icon className="w-4.5 h-4.5" />
              <span>{tab.name}</span>
            </button>
          )
        })}
      </div>

      {/* TAB CONTENT: OVERVIEW */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-in fade-in duration-200">
          <div className="lg:col-span-2 space-y-6">
            <Card className="border border-border bg-card shadow-sm">
              <CardHeader>
                <h3 className="font-bold text-lg text-foreground">Workspace Metrics</h3>
              </CardHeader>
              <CardContent className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="p-4 rounded-xl bg-muted/30 border border-border">
                  <span className="text-[10px] text-muted-foreground uppercase font-extrabold block">Total Tasks</span>
                  <span className="text-3xl font-extrabold text-foreground font-outfit mt-1 block">{totalTasks}</span>
                </div>
                <div className="p-4 rounded-xl bg-muted/30 border border-border">
                  <span className="text-[10px] text-muted-foreground uppercase font-extrabold block">Pending Tasks</span>
                  <span className="text-3xl font-extrabold text-foreground font-outfit mt-1 block">{pendingTasks}</span>
                </div>
                <div className="p-4 rounded-xl bg-muted/30 border border-border">
                  <span className="text-[10px] text-muted-foreground uppercase font-extrabold block">Completed Tasks</span>
                  <span className="text-3xl font-extrabold text-emerald-500 font-outfit mt-1 block">{completedTasks}</span>
                </div>
              </CardContent>
            </Card>

            <Card className="border border-border bg-card shadow-sm">
              <CardHeader>
                <h3 className="font-bold text-lg text-foreground">Collaborators overview</h3>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground font-semibold">Project Owner</span>
                  <span className="text-foreground font-bold font-mono text-xs">{activeProject.owner}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground font-semibold">Total Members Joined</span>
                  <span className="text-foreground font-bold font-outfit">{members.length}</span>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card className="border border-border bg-card shadow-sm">
              <CardHeader>
                <h3 className="font-bold text-base text-foreground">Quick Info</h3>
              </CardHeader>
              <CardContent className="space-y-4 text-xs text-muted-foreground">
                <div>
                  <span className="font-bold block mb-1">Created At</span>
                  <span>{new Date(activeProject.createdAt).toLocaleString()}</span>
                </div>
                <div>
                  <span className="font-bold block mb-1">Last Updated</span>
                  <span>{new Date(activeProject.updatedAt).toLocaleString()}</span>
                </div>
                <div>
                  <span className="font-bold block mb-1">Workspace ID</span>
                  <span className="font-mono text-[10px]">{activeProject.id}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* TAB CONTENT: KANBAN BOARD */}
      {activeTab === 'kanban' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold text-muted-foreground">
              {totalTasks} total tasks in workspace
            </div>
            <Button onClick={openCreateTask} className="rounded-xl h-10 font-bold space-x-1.5 shadow-sm">
              <Plus className="w-4.5 h-4.5" />
              <span>Add Task</span>
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 items-start">
            {(['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED'] as const).map((colStatus) => {
              const tasksInCol = groupedTasks[colStatus] || []
              return (
                <div key={colStatus} className="rounded-2xl border border-border/80 bg-card/25 p-4 flex flex-col h-[70vh]">
                  {/* Column Title Header */}
                  <div className="flex items-center justify-between mb-4 pb-2 border-b border-border/40">
                    <div className="flex items-center space-x-2">
                      <span
                        className={`w-2.5 h-2.5 rounded-full ${
                          colStatus === 'TODO'
                            ? 'bg-zinc-400'
                            : colStatus === 'IN_PROGRESS'
                              ? 'bg-amber-500 animate-pulse'
                              : colStatus === 'DONE'
                                ? 'bg-blue-500'
                                : 'bg-red-500'
                        }`}
                      />
                      <h4 className="font-bold text-sm text-foreground">
                        {colStatus === 'IN_PROGRESS' ? 'IN PROGRESS' : colStatus}
                      </h4>
                    </div>
                    <span className="text-xs font-bold font-outfit px-2 py-0.5 bg-card border border-border/50 rounded-lg text-muted-foreground">
                      {tasksInCol.length}
                    </span>
                  </div>

                  {/* Scrollable Column Body */}
                  <div className="flex-1 overflow-y-auto space-y-3 pr-1">
                    {tasksInCol.length === 0 ? (
                      <div className="h-28 border border-dashed border-border/60 rounded-xl flex flex-col items-center justify-center text-center p-3 text-muted-foreground/60 text-xs">
                        <span>No tasks in this stage</span>
                      </div>
                    ) : (
                      tasksInCol.map((task) => (
                        <div
                          key={task.id}
                          className="group p-4 bg-card border border-border hover:border-primary/30 rounded-xl shadow-xs space-y-3 cursor-pointer relative"
                          onClick={() => openEditTask(task)}
                        >
                          <div className="space-y-1">
                            <div className="flex items-start justify-between">
                              <h5 className="font-bold text-sm text-foreground group-hover:text-primary transition-colors line-clamp-1 pr-4">
                                {task.title}
                              </h5>
                              <span
                                className={`text-[8px] font-extrabold px-1.5 py-0.5 rounded-full ${
                                  task.priority === 'CRITICAL'
                                    ? 'bg-red-500/10 text-red-500 border border-red-500/10'
                                    : task.priority === 'HIGH'
                                      ? 'bg-amber-500/10 text-amber-500 border border-amber-500/10'
                                      : 'bg-muted text-muted-foreground'
                                }`}
                              >
                                {task.priority}
                              </span>
                            </div>
                            <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                              {task.description || 'No technical summary details.'}
                            </p>
                          </div>

                          <div className="flex items-center justify-between pt-2 border-t border-border/30 text-xs">
                            <div className="flex items-center space-x-1.5 text-muted-foreground">
                              <Calendar className="w-3.5 h-3.5" />
                              <span className="font-semibold text-[10px]">
                                {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : 'No date'}
                              </span>
                            </div>

                            {/* Assignee Avatar Initials */}
                            {task.assignedTo ? (
                              <div
                                className="w-6 h-6 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary text-[9px] font-outfit"
                                title={`Assignee: ${getFullName(task.assignedTo)}`}
                              >
                                {getInitials(task.assignedTo)}
                              </div>
                            ) : (
                              <div className="w-6 h-6 rounded-lg border border-border border-dashed flex items-center justify-center text-muted-foreground" title="Unassigned">
                                <HelpCircle className="w-3.5 h-3.5" />
                              </div>
                            )}
                          </div>

                          {/* Instant click status transitions dropdown */}
                          <div className="absolute right-3 top-2 opacity-0 group-hover:opacity-100 transition-opacity flex items-center space-x-1">
                            <div className="relative inline-block text-left" onClick={(e) => e.stopPropagation()}>
                              <select
                                value={task.status}
                                onChange={(e) => handleStatusChange(task, e.target.value as any)}
                                className="h-6 text-[10px] font-extrabold uppercase bg-card border border-border rounded-lg px-2 cursor-pointer focus:outline-none focus:border-primary text-foreground"
                              >
                                <option value="TODO">TODO</option>
                                <option value="IN_PROGRESS">IN PROGRESS</option>
                                <option value="DONE">DONE</option>
                                <option value="BLOCKED">BLOCKED</option>
                              </select>
                            </div>

                            <button
                              onClick={(e) => {
                                e.stopPropagation()
                                handleDeleteTask(task.id)
                              }}
                              className="p-1 rounded bg-red-500/10 hover:bg-red-500/20 text-red-500 transition-colors"
                              title="Delete Task"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* TAB CONTENT: MEMBERS */}
      {activeTab === 'members' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-bold text-foreground">Project Workspace Collaborators</h3>
            {isOwner && (
              <Button onClick={() => setMemberFormOpen(true)} className="rounded-xl h-10 font-bold space-x-1.5 shadow-sm">
                <UserPlus className="w-4.5 h-4.5" />
                <span>Add Member</span>
              </Button>
            )}
          </div>

          <Card className="border border-border bg-card shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-muted/40 border-b border-border/60 text-xs font-extrabold uppercase text-muted-foreground tracking-wider">
                    <th className="px-6 py-3.5">User Identity Details</th>
                    <th className="px-6 py-3.5">System Identifiers</th>
                    <th className="px-6 py-3.5">Workspace Role</th>
                    {isOwner && <th className="px-6 py-3.5 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/40 text-sm">
                  {members.length === 0 ? (
                    <tr>
                      <td colSpan={isOwner ? 4 : 3} className="text-center py-8 text-muted-foreground font-semibold">
                        No collaborators assigned to this workspace.
                      </td>
                    </tr>
                  ) : (
                    members.map((member) => {
                      const profile = resolvedProfiles[member.userId]
                      return (
                        <tr key={member.userId} className="hover:bg-muted/10 transition-colors">
                          <td className="px-6 py-4">
                            <div className="flex items-center space-x-3">
                              <div className="w-9 h-9 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary text-xs font-outfit">
                                {getInitials(member.userId)}
                              </div>
                              <div className="min-w-0">
                                <span className="font-bold text-foreground block truncate">
                                  {profile ? `${profile.firstName} ${profile.surname || ''}`.trim() : 'Loading Profile...'}
                                </span>
                                {profile && (
                                  <span className="text-xs text-muted-foreground block truncate">
                                    @{profile.username}
                                  </span>
                                )}
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 font-mono text-xs text-muted-foreground">
                            {member.userId}
                          </td>
                          <td className="px-6 py-4">
                            <span
                              className={`text-[9px] font-extrabold uppercase px-2 py-0.5 rounded-full ${
                                member.role === 'ADMIN'
                                  ? 'bg-rose-500/10 text-rose-500 border border-rose-500/10'
                                  : member.role === 'VIEWER'
                                    ? 'bg-zinc-500/10 text-zinc-500 border border-zinc-500/10'
                                    : 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/10'
                              }`}
                            >
                              {member.role}
                            </span>
                          </td>
                          {isOwner && (
                            <td className="px-6 py-4 text-right">
                              {/* Keep owners safe from being self-kicked */}
                              {member.userId !== activeProject.owner && (
                                <button
                                  onClick={() => handleRemoveMember(member.userId)}
                                  className="p-1.5 rounded-lg text-muted-foreground hover:text-red-500 hover:bg-red-500/10 transition-colors"
                                  title="Remove Member"
                                >
                                  <UserMinus className="w-4.5 h-4.5" />
                                </button>
                              )}
                            </td>
                          )}
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      )}

      {/* Forms Overlay Components */}
      <ProjectForm
        isOpen={projectFormOpen}
        onClose={() => setProjectFormOpen(false)}
        initialData={{
          id: activeProject.id,
          name: activeProject.name,
          description: activeProject.description || '',
          status: activeProject.status,
        }}
      />

      <MemberForm
        isOpen={memberFormOpen}
        onClose={() => setMemberFormOpen(false)}
        projectId={pId}
        excludeUserIds={members.map((m) => m.userId)}
      />

      <TaskForm
        isOpen={taskFormOpen}
        onClose={() => setTaskFormOpen(false)}
        projectId={pId}
        projectMembers={members}
        initialData={selectedTask}
      />
    </div>
  )
}

export default ProjectDetailPage
