import React, { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, UserPlus, Users, ShieldCheck } from 'lucide-react'
import { toast } from 'sonner'
import { useCollaboration } from '../hooks/useCollaboration'
import { projectApi } from '@/features/projects/api/projectApi'
import MemberList from '../components/MemberList'
import EmptyMembersState from '../components/EmptyMembersState'
import AddMemberDialog from '../components/AddMemberDialog'
import { Button } from '@/components/ui/Button'
import { useAppSelector } from '@/hooks/store'
import { selectAuth } from '@/features/auth/store/authSlice'
import type { ProjectDto } from '@/features/projects/types/projectTypes'
import type { ProjectRole } from '../types/collaborationTypes'

export const ProjectMembersPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>()
  const parsedProjectId = projectId ? parseInt(projectId, 10) : NaN

  const { user } = useAppSelector(selectAuth)
  const { members, isLoading, fetchMembers, addMember, removeMember } = useCollaboration()

  const [project, setProject] = useState<ProjectDto | null>(null)
  const [isFetchingProject, setIsFetchingProject] = useState(true)
  const [showInviteDialog, setShowInviteDialog] = useState(false)
  const [isMutating, setIsMutating] = useState(false)

  // Fetch project details and members list on mount
  useEffect(() => {
    if (isNaN(parsedProjectId)) return

    const loadData = async () => {
      try {
        const fetchedProject = await projectApi.getById(parsedProjectId)
        setProject(fetchedProject)
        await fetchMembers(parsedProjectId)
      } catch (e: any) {
        toast.error('Failed to load project details or member roster.')
      } finally {
        setIsFetchingProject(false)
      }
    }

    loadData()
  }, [parsedProjectId, fetchMembers])

  const handleAddMemberSubmit = async (userId: string, role: ProjectRole) => {
    setIsMutating(true)
    try {
      await addMember(parsedProjectId, userId, role)
      setShowInviteDialog(false)
    } catch (e: any) {
      // hook displays toast
    } finally {
      setIsMutating(false)
    }
  }

  const handleRemoveMemberClick = async (userId: string) => {
    if (window.confirm(`Are you sure you want to remove user "${userId}" from the project?`)) {
      setIsMutating(true)
      try {
        await removeMember(parsedProjectId, userId)
      } catch (e: any) {
        // hook displays toast
      } finally {
        setIsMutating(false)
      }
    }
  }

  // Permissions validation
  const isOwner = project?.owner === user?.email
  const isAdmin = user?.role === 'ADMIN'
  const canManage = isOwner || isAdmin

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Navigation Breadcrumb */}
      <div className="space-y-2">
        <Link
          to={`/projects/${parsedProjectId}`}
          className="inline-flex items-center text-sm font-bold text-muted-foreground hover:text-foreground transition-colors group"
        >
          <ArrowLeft size={16} className="mr-1 group-hover:-translate-x-0.5 transition-transform" />
          <span>Back to Project Overview</span>
        </Link>

        <div className="flex flex-col md:flex-row md:items-center md:justify-between space-y-4 md:space-y-0">
          <div>
            <div className="flex items-center space-x-2 text-primary font-bold mb-1">
              <Users size={16} />
              <span className="text-xs uppercase tracking-wider font-extrabold">Roster Directory</span>
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl">
              {project ? `${project.name} Team` : 'Project Members'}
            </h1>
            <p className="text-muted-foreground text-sm font-medium mt-1">
              Manage roles, project authorizations, and active workspace collaboration.
            </p>
          </div>

          {canManage && project && (
            <Button
              onClick={() => setShowInviteDialog(true)}
              className="flex items-center space-x-2 w-full md:w-auto shadow-md font-semibold"
            >
              <UserPlus size={18} />
              <span>Invite Member</span>
            </Button>
          )}
        </div>
      </div>

      {/* Main Members Roster Content */}
      {isFetchingProject || isLoading ? (
        // Premium Pulse Loading Skeletons
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[...Array(4)].map((_, i) => (
            <div
              key={i}
              className="flex items-center justify-between p-4 bg-card/60 border border-border rounded-xl animate-pulse"
            >
              <div className="flex items-center space-x-3.5 w-full">
                <div className="h-10 w-10 bg-muted rounded-full"></div>
                <div className="space-y-2 flex-1">
                  <div className="h-4 w-1/3 bg-muted rounded"></div>
                  <div className="h-3 w-1/4 bg-muted rounded"></div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : members.length === 0 ? (
        <EmptyMembersState
          onAddClick={() => setShowInviteDialog(true)}
          canManage={canManage}
        />
      ) : (
        <div className="space-y-4">
          {/* Owner Box Banner */}
          {project && (
            <div className="flex items-center space-x-3 p-4 rounded-xl border border-primary/20 bg-primary/5 shadow-sm">
              <ShieldCheck className="text-primary shrink-0" size={20} />
              <span className="text-xs font-semibold text-foreground/95">
                Workspace administration managed by project creator <strong className="text-primary">{project.owner}</strong>.
              </span>
            </div>
          )}

          <MemberList
            members={members}
            ownerEmail={project?.owner || ''}
            canManage={canManage}
            onRemoveClick={handleRemoveMemberClick}
            isLoading={isMutating}
          />
        </div>
      )}

      {/* Invitation Modal Dialog Overlay */}
      <AddMemberDialog
        isOpen={showInviteDialog}
        onClose={() => setShowInviteDialog(false)}
        onConfirm={handleAddMemberSubmit}
        isLoading={isMutating}
      />
    </div>
  )
}

export default ProjectMembersPage
